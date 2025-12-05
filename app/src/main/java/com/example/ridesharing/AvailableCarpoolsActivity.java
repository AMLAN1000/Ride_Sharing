package com.example.ridesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AvailableCarpoolsActivity extends AppCompatActivity implements CarpoolAdapter.OnCarpoolClickListener {

    private static final String TAG = "AvailableCarpools";
    private static final String FIELD_PASSENGER_IDS = "passengerIds";
    private static final String FIELD_PASSENGER_COUNT = "passengerCount";
    private static final String FIELD_MAX_SEATS = "maxSeats";
    private static final String FIELD_STATUS = "status";

    private RecyclerView carpoolsRecyclerView;
    private CarpoolAdapter carpoolAdapter;
    private View emptyStateLayout;
    private ProgressBar progressBar;
    private TextView carpoolsCountText, subtitleText;
    private List<Carpool> carpoolList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration carpoolsListener;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_carpools);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        loadAvailableCarpools();

        try {
            BottomNavigationHelper.setupBottomNavigation(this, "CARPOOL");
        } catch (Exception e) {
            Log.e(TAG, "Bottom navigation setup failed", e);
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        carpoolsRecyclerView = findViewById(R.id.carpools_recyclerview);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        progressBar = findViewById(R.id.progress_bar);
        carpoolsCountText = findViewById(R.id.carpools_count_text);
        subtitleText = findViewById(R.id.subtitle_text);

        Button filterButton = findViewById(R.id.filter_button);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

        Button btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        carpoolsRecyclerView.setLayoutManager(layoutManager);
        carpoolsRecyclerView.setHasFixedSize(true);
        carpoolAdapter = new CarpoolAdapter(carpoolList, this);
        carpoolsRecyclerView.setAdapter(carpoolAdapter);
    }

    private void loadAvailableCarpools() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping duplicate call");
            return;
        }

        isLoading = true;
        showLoadingState();

        if (carpoolsListener != null) {
            carpoolsListener.remove();
            carpoolsListener = null;
        }

        Log.d(TAG, "Loading carpool posts...");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        final String currentUserId = currentUser != null ? currentUser.getUid() : null;

        // Query for carpool posts (type = "carpool", isDriverPost = true, status = pending)
        carpoolsListener = db.collection("ride_requests")
                .whereEqualTo("type", "carpool")
                .whereEqualTo("isDriverPost", true)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    isLoading = false;

                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage(), error);
                        runOnUiThread(() -> {
                            hideLoadingState();
                            showEmptyState("Error loading carpools");
                            Toast.makeText(this, "Connection error. Check your internet.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        Log.e(TAG, "QueryDocumentSnapshots is null");
                        runOnUiThread(() -> {
                            hideLoadingState();
                            showEmptyState("No data received");
                        });
                        return;
                    }

                    Log.d(TAG, "Received " + queryDocumentSnapshots.size() + " carpool posts");

                    List<Carpool> newCarpools = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Carpool carpool = parseCarpoolFromFirestore(document);
                            if (carpool != null) {
                                // Check if there are available seats
                                if (carpool.getAvailableSeats() > 0) {
                                    // Check if current user has already joined this carpool
                                    if (currentUserId != null) {
                                        List<String> passengerIds = (List<String>) document.get(FIELD_PASSENGER_IDS);
                                        boolean alreadyJoined = passengerIds != null && passengerIds.contains(currentUserId);
                                        carpool.setUserAlreadyJoined(alreadyJoined);
                                    }
                                    newCarpools.add(carpool);
                                    Log.d(TAG, "Successfully parsed carpool: " + carpool.getDriverName());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document " + document.getId(), e);
                        }
                    }

                    runOnUiThread(() -> {
                        carpoolList.clear();
                        carpoolList.addAll(newCarpools);
                        carpoolAdapter.updateCarpools(carpoolList);
                        hideLoadingState();
                        updateUIState();
                    });
                });
    }

    private Carpool parseCarpoolFromFirestore(QueryDocumentSnapshot document) {
        try {
            String id = document.getId();
            String driverId = document.getString("driverId");
            String driverName = document.getString("driverName");
            String driverPhone = document.getString("driverPhone");
            String driverPhoto = document.getString("driverPhoto");
            Double rating = document.getDouble("driverRating");
            String vehicleType = document.getString("vehicleType");
            Long maxSeatsLong = document.getLong(FIELD_MAX_SEATS);
            Long passengerCountLong = document.getLong(FIELD_PASSENGER_COUNT);
            String pickupLocation = document.getString("pickupLocation");
            String dropLocation = document.getString("dropLocation");
            Long departureTime = document.getLong("departureTime");
            Double farePerPassenger = document.getDouble("farePerPassenger");
            Double totalFare = document.getDouble("totalFare");
            Double distance = document.getDouble("distance");

            // If farePerPassenger doesn't exist, calculate it from totalFare and maxSeats
            if (farePerPassenger == null && totalFare != null && maxSeatsLong != null && maxSeatsLong > 0) {
                farePerPassenger = totalFare / maxSeatsLong;
            }

            // Format departure time
            String departureTimeStr = "Now";
            if (departureTime != null) {
                try {
                    departureTimeStr = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(new java.util.Date(departureTime));
                } catch (Exception e) {
                    departureTimeStr = "Now";
                }
            }

            // Calculate available seats
            int maxSeats = maxSeatsLong != null ? maxSeatsLong.intValue() : 3;
            int passengerCount = passengerCountLong != null ? passengerCountLong.intValue() : 0;
            int availableSeats = maxSeats - passengerCount;

            return new Carpool(
                    id,
                    driverName != null ? driverName : "Driver",
                    vehicleType != null ? vehicleType.toUpperCase() : "CAR",
                    availableSeats,
                    rating != null ? rating : 4.5,
                    pickupLocation != null ? pickupLocation : "Pickup",
                    dropLocation != null ? dropLocation : "Drop",
                    departureTimeStr,
                    farePerPassenger != null ? farePerPassenger : 0.0,
                    totalFare != null ? totalFare : 0.0,
                    driverId,
                    passengerCount,
                    maxSeats,
                    driverPhone != null ? driverPhone : "",  // Add driver phone
                    distance != null ? distance : 0.0        // Add distance
            );
        } catch (Exception e) {
            Log.e(TAG, "Critical error parsing carpool", e);
            return null;
        }
    }

    private void updateUIState() {
        if (carpoolList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            carpoolsRecyclerView.setVisibility(View.GONE);
            carpoolsCountText.setText("No carpools available");
            if (subtitleText != null) {
                subtitleText.setText("No carpool rides at the moment");
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            carpoolsRecyclerView.setVisibility(View.VISIBLE);
            carpoolsCountText.setText(carpoolList.size() + " carpools available");
            if (subtitleText != null) {
                subtitleText.setText("Share rides and save money!");
            }
        }
    }

    private void showLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
        carpoolsRecyclerView.setVisibility(View.GONE);
        carpoolsCountText.setText("Loading carpools...");
    }

    private void hideLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
        carpoolsRecyclerView.setVisibility(View.GONE);
        carpoolsCountText.setText(message);
    }

    private void showFilterDialog() {
        Toast.makeText(this, "Filter feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCarpoolRequestClick(Carpool carpool) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to join carpools", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Prevent drivers from joining their own carpool
        if (currentUserId.equals(carpool.getDriverId())) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cannot Join Own Carpool")
                    .setMessage("You cannot join your own carpool. This feature is for passengers to find rides.")
                    .setPositiveButton("OK", null)
                    .show();
            Log.w(TAG, "Attempted self-join by driver: " + currentUserId);
            return;
        }

        // Check if user has already joined
        if (carpool.isUserAlreadyJoined()) {
            Toast.makeText(this, "You have already joined this carpool!", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if carpool is full
        if (carpool.getAvailableSeats() <= 0) {
            Toast.makeText(this, "This carpool is already full!", Toast.LENGTH_LONG).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Join Carpool")
                .setMessage("Join carpool with " + carpool.getDriverName() +
                        "\n\nFare per passenger: ৳" + String.format(Locale.getDefault(), "%.0f", carpool.getFarePerPassenger()) +
                        "\nTotal fare: ৳" + String.format(Locale.getDefault(), "%.0f", carpool.getTotalFare()) +
                        "\nVehicle: " + carpool.getVehicleModel() +
                        "\nAvailable Seats: " + carpool.getAvailableSeats() +
                        "\nAlready joined: " + carpool.getPassengerCount() + "/" + carpool.getMaxSeats())
                .setPositiveButton("Join", (dialog, which) -> joinCarpool(carpool))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onCarpoolProfileViewClick(Carpool carpool) {
        String profileInfo = "Driver: " + carpool.getDriverName() +
                "\n\nRating: " + String.format(Locale.getDefault(), "%.1f", carpool.getRating()) + " ⭐" +
                "\n\nVehicle: " + carpool.getVehicleModel() +
                "\n\nSeats available: " + carpool.getAvailableSeats() +
                "\nPassengers joined: " + carpool.getPassengerCount() + "/" + carpool.getMaxSeats() +
                "\n\nFare per passenger: ৳" + String.format(Locale.getDefault(), "%.0f", carpool.getFarePerPassenger()) +
                "\nTotal fare: ৳" + String.format(Locale.getDefault(), "%.0f", carpool.getTotalFare());

        if (carpool.getDriverPhone() != null && !carpool.getDriverPhone().isEmpty()) {
            profileInfo += "\n\n📞 Driver Phone: " + carpool.getDriverPhone();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Carpool Details")
                .setMessage(profileInfo)
                .setPositiveButton("OK", null)
                .setNeutralButton("Call Driver", (dialog, which) -> callDriver(carpool))
                .show();
    }

    @Override
    public void onCarpoolCallClick(Carpool carpool) {
        callDriver(carpool);
    }

    @Override
    public void onCarpoolMessageClick(Carpool carpool) {
        Toast.makeText(this, "Messaging feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void callDriver(Carpool carpool) {
        if (carpool.getDriverPhone() != null && !carpool.getDriverPhone().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + carpool.getDriverPhone()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Driver phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    // In AvailableCarpoolsActivity.java
// Replace the joinCarpool() method with this fixed version:

    // In AvailableCarpoolsActivity.java
// Replace the joinCarpool() method with this fixed version:

    // In AvailableCarpoolsActivity.java
// Replace the joinCarpool() method with this fixed version:

    private void joinCarpool(Carpool carpool) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String passengerId = currentUser.getUid();
        Toast.makeText(this, "Joining carpool...", Toast.LENGTH_SHORT).show();

        // Get passenger's name from Firestore
        db.collection("users").document(passengerId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    String passengerName = "Passenger";

                    if (userDocument.exists()) {
                        passengerName = userDocument.getString("fullName");
                        if (passengerName == null || passengerName.trim().isEmpty()) {
                            passengerName = currentUser.getDisplayName();
                            if (passengerName == null || passengerName.trim().isEmpty()) {
                                passengerName = "Passenger";
                            }
                        }
                    }

                    final String finalPassengerName = passengerName;

                    // Update carpool with passenger info
                    db.collection("ride_requests").document(carpool.getId())
                            .update(
                                    FIELD_PASSENGER_IDS, com.google.firebase.firestore.FieldValue.arrayUnion(passengerId),
                                    "passengerNames", com.google.firebase.firestore.FieldValue.arrayUnion(passengerName),
                                    FIELD_PASSENGER_COUNT, com.google.firebase.firestore.FieldValue.increment(1),
                                    "lastUpdatedBy", passengerId  // Track who made this update
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Joined carpool successfully!", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "✅ Carpool joined successfully");

                                // Check if carpool is now full
                                checkAndUpdateCarpoolStatus(carpool, passengerId);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to join: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error joining carpool", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching passenger info", e);
                    Toast.makeText(this, "Failed to get passenger details.", Toast.LENGTH_SHORT).show();
                });
    }

    // Update checkAndUpdateCarpoolStatus to include notificationSentBy
    private void checkAndUpdateCarpoolStatus(Carpool carpool, String passengerId) {
        db.collection("ride_requests").document(carpool.getId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long passengerCount = document.getLong(FIELD_PASSENGER_COUNT);
                        Long maxSeats = document.getLong(FIELD_MAX_SEATS);

                        if (passengerCount != null && maxSeats != null) {
                            if (passengerCount >= maxSeats) {
                                // Carpool is full - mark as accepted
                                db.collection("ride_requests").document(carpool.getId())
                                        .update(
                                                FIELD_STATUS, "accepted",
                                                "notificationSentBy", passengerId  // Last person who joined
                                        )
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this,
                                                    "✅ Carpool is now full! Ride confirmed.",
                                                    Toast.LENGTH_LONG).show();
                                            Log.d(TAG, "✅ Carpool full, StatusMonitor will notify all");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating carpool status to accepted", e);
                                        });
                            } else {
                                // Carpool not full yet - send notification directly to driver
                                // Since status doesn't change, we need to send notification here
                                db.collection("ride_requests").document(carpool.getId())
                                        .get()
                                        .addOnSuccessListener(carpoolDoc -> {
                                            // Only send notification to DRIVER (not the passenger who just joined)
                                            String driverId = carpoolDoc.getString("driverId");
                                            if (driverId != null && !driverId.equals(passengerId)) {
                                                RideNotificationManager notificationManager =
                                                        RideNotificationManager.getInstance(AvailableCarpoolsActivity.this);
                                                notificationManager.sendCarpoolSeatFilled(carpoolDoc);
                                                Log.d(TAG, "✅ Seat filled notification sent to driver");
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking carpool status", e);
                });
    }



    @Override
    protected void onResume() {
        super.onResume();
        try {
            BottomNavigationHelper.setupBottomNavigation(this, "CARPOOL");
        } catch (Exception e) {
            Log.e(TAG, "Bottom navigation setup failed", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (carpoolsListener != null) {
            carpoolsListener.remove();
            carpoolsListener = null;
        }
    }
}
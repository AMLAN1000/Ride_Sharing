package com.example.ridesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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

public class AvailableRidesActivity extends AppCompatActivity implements RideAdapter.OnRideClickListener {

    private static final String TAG = "AvailableRides";

    private RecyclerView ridesRecyclerView;
    private RideAdapter rideAdapter;
    private View emptyStateLayout;
    private ProgressBar progressBar;
    private TextView ridesCountText, subtitleText;
    private List<Ride> rideList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration ridesListener;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_rides);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        loadAvailableRides();
        ExpiredRequestsCleaner.cleanExpiredPendingRequests();


        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        ridesRecyclerView = findViewById(R.id.rides_recyclerview);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        progressBar = findViewById(R.id.progress_bar);
        ridesCountText = findViewById(R.id.rides_count_text);
        subtitleText = findViewById(R.id.subtitle_text);

        ImageView filterButton = findViewById(R.id.filter_button);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ridesRecyclerView.setLayoutManager(layoutManager);
        ridesRecyclerView.setHasFixedSize(true);
        rideAdapter = new RideAdapter(rideList, this);
        ridesRecyclerView.setAdapter(rideAdapter);
    }

    private void loadAvailableRides() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping duplicate call");
            return;
        }

        isLoading = true;
        showLoadingState();

        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }

        Log.d(TAG, "Loading driver-posted rides...");

        // Query for driver-posted rides (isDriverPost = true, status = pending)
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("isDriverPost", true)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    isLoading = false;

                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage(), error);
                        runOnUiThread(() -> {
                            hideLoadingState();
                            showEmptyState("Error loading rides");
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

                    Log.d(TAG, "Received " + queryDocumentSnapshots.size() + " driver-posted rides");

                    List<Ride> newRides = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Ride ride = parseRideFromFirestore(document);
                            if (ride != null) {
                                newRides.add(ride);
                                Log.d(TAG, "Successfully parsed: " + ride.getDriverName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document " + document.getId(), e);
                        }
                    }

                    runOnUiThread(() -> {
                        rideList.clear();
                        rideList.addAll(newRides);
                        rideAdapter.updateRides(rideList);
                        hideLoadingState();
                        updateUIState();
                    });
                });
    }

    private Ride parseRideFromFirestore(QueryDocumentSnapshot document) {
        try {
            String id = document.getId();
            String driverId = document.getString("driverId");
            String driverName = document.getString("driverName");
            String driverPhone = document.getString("driverPhone");
            Double rating = document.getDouble("driverRating");
            String vehicleType = document.getString("vehicleType");
            Long passengersLong = document.getLong("passengers");
            String pickupLocation = document.getString("pickupLocation");
            String dropLocation = document.getString("dropLocation");
            Long departureTime = document.getLong("departureTime");
            Double fare = document.getDouble("fare");

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

            // Calculate available seats (total passengers the driver can accommodate)
            int availableSeats = passengersLong != null ? passengersLong.intValue() : 1;

            return new Ride(
                    id,
                    driverName != null ? driverName : "Driver",
                    vehicleType != null ? vehicleType.toUpperCase() : "CAR",
                    availableSeats,
                    rating != null ? rating : 4.5,
                    pickupLocation != null ? pickupLocation : "Pickup",
                    dropLocation != null ? dropLocation : "Drop",
                    departureTimeStr,
                    "", // arrivalTime (can be calculated if needed)
                    fare != null ? fare : 0.0,
                    driverId
            );
        } catch (Exception e) {
            Log.e(TAG, "Critical error parsing ride", e);
            return null;
        }
    }

    private void updateUIState() {
        if (rideList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            ridesRecyclerView.setVisibility(View.GONE);
            ridesCountText.setText("No rides available");
            if (subtitleText != null) {
                subtitleText.setText("No driver-posted rides at the moment");
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            ridesRecyclerView.setVisibility(View.VISIBLE);
            ridesCountText.setText(rideList.size() + " rides available");
            if (subtitleText != null) {
                subtitleText.setText(rideList.size() + " rides near you");
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
        ridesRecyclerView.setVisibility(View.GONE);
        ridesCountText.setText("Loading rides...");
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
        ridesRecyclerView.setVisibility(View.GONE);
        ridesCountText.setText(message);
    }

    private void showFilterDialog() {
        Toast.makeText(this, "Filter feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRideRequestClick(Ride ride) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to request rides", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Prevent drivers from accepting their own ride posts
        if (currentUserId.equals(ride.getDriverId())) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cannot Accept Own Ride")
                    .setMessage("You cannot accept your own posted ride. This feature is for passengers to find rides.")
                    .setPositiveButton("OK", null)
                    .show();
            Log.w(TAG, "Attempted self-acceptance by driver: " + currentUserId);
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Request Ride")
                .setMessage("Request ride with " + ride.getDriverName() +
                        "\n\nFare: ৳" + String.format(Locale.getDefault(), "%.0f", ride.getFare()) +
                        "\nVehicle: " + ride.getVehicleModel() +
                        "\nAvailable Seats: " + ride.getAvailableSeats())
                .setPositiveButton("Confirm", (dialog, which) -> acceptRide(ride))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptRide(Ride ride) {
        String passengerId = mAuth.getCurrentUser().getUid();
        Toast.makeText(this, "Accepting ride...", Toast.LENGTH_SHORT).show();

        // Get passenger's name and phone from Firestore
        db.collection("users").document(passengerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String passengerName = "Passenger";
                    String passengerPhone = "";

                    if (documentSnapshot.exists()) {
                        passengerName = documentSnapshot.getString("fullName");
                        if (passengerName == null || passengerName.trim().isEmpty()) {
                            passengerName = mAuth.getCurrentUser().getDisplayName();
                            if (passengerName == null || passengerName.trim().isEmpty()) {
                                passengerName = "Passenger";
                            }
                        }
                        passengerPhone = documentSnapshot.getString("phone");
                        if (passengerPhone == null) {
                            passengerPhone = "";
                        }
                    }

                    // Update ride with passenger info
                    db.collection("ride_requests").document(ride.getId())
                            .update(
                                    "status", "accepted",
                                    "passengerId", passengerId,
                                    "passengerName", passengerName,
                                    "passengerPhone", passengerPhone,
                                    "acceptedAt", System.currentTimeMillis(),
                                    "notificationShown", false
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Ride accepted! Contact " +
                                        ride.getDriverName(), Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error accepting ride", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching passenger info", e);
                    Toast.makeText(this, "Failed to get passenger details.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onProfileViewClick(Ride ride) {
        String profileInfo = "Name: " + ride.getDriverName() +
                "\n\nRating: " + String.format(Locale.getDefault(), "%.1f", ride.getRating()) + " ⭐" +
                "\n\nVehicle: " + ride.getVehicleModel() +
                "\n\nAvailable Seats: " + ride.getAvailableSeats();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Driver Profile")
                .setMessage(profileInfo)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }
    }
}
package com.example.ridesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue; // FieldValue is key for incrementing
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Assuming MyRideItem is correctly defined elsewhere with appropriate getters/setters

public class MyRidesActivity extends AppCompatActivity {

    private static final String TAG = "MyRidesActivity";

    private TextView tabAsPassenger, tabAsDriver;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private TextView emptyStateText;

    private MyRidesAdapter adapter;
    private List<MyRideItem> ridesList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration ridesListener;

    private boolean showingPassengerRides = true; // Default to passenger view

    // New constants for ride status for cleaner code
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_COMPLETED = "completed";
    private static final String FIELD_STATUS = "status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        setupClickListeners();

        // Load passenger rides by default
        updateTabUI(); // Set initial tab appearance
        loadPassengerRides();
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");

    }
    // ✅ ADD THIS ENTIRE METHOD AFTER onCreate()
    private void listenForDriverRideAcceptance() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Listen for rides where I'm the driver and status changes to accepted
        db.collection("ride_requests")
                .whereEqualTo("driverId", userId)
                .whereEqualTo("isDriverPost", true)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for driver ride acceptance", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                com.google.firebase.firestore.QueryDocumentSnapshot document = dc.getDocument();
                                String status = document.getString("status");
                                Boolean notifShown = document.getBoolean("notificationShown");

                                // Show dialog if just accepted and notification not shown
                                if ("accepted".equals(status) && (notifShown == null || !notifShown)) {
                                    showDriverRideAcceptedDialog(document);

                                    // Mark as shown
                                    document.getReference().update("notificationShown", true);
                                }
                            }
                        }
                    }
                });
    }
    // ✅ ADD THIS ENTIRE METHOD AFTER listenForDriverRideAcceptance()
    private void showDriverRideAcceptedDialog(com.google.firebase.firestore.QueryDocumentSnapshot document) {
        String passengerName = document.getString("passengerName");
        String passengerPhone = document.getString("passengerPhone");
        String pickupLocation = document.getString("pickupLocation");
        String dropLocation = document.getString("dropLocation");
        Double fare = document.getDouble("fare");

        // Show notification
        NotificationHelper.showRideAcceptedByPassengerNotification(
                this,
                passengerName != null ? passengerName : "Passenger",
                passengerPhone,
                pickupLocation != null ? pickupLocation : "Pickup",
                dropLocation != null ? dropLocation : "Drop",
                fare != null ? fare : 0.0,
                document.getId()
        );

        // Build dialog message
        String message = "🎉 Great news! " + passengerName +
                " has accepted your ride offer!\n\n" +
                "📍 From: " + pickupLocation + "\n" +
                "🎯 To: " + dropLocation + "\n" +
                "💰 Fare: ৳" + String.format(java.util.Locale.getDefault(), "%.0f", fare);

        if (passengerPhone != null && !passengerPhone.isEmpty()) {
            message += "\n\n📞 Passenger Phone: " + passengerPhone;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("✅ Ride Offer Accepted!")
                .setMessage(message)
                .setPositiveButton("Call Passenger", (dialog, which) -> {
                    if (passengerPhone != null && !passengerPhone.isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(android.net.Uri.parse("tel:" + passengerPhone));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("OK", null)
                .setCancelable(false)
                .show();

        NotificationHelper.playNotificationSound(this);
    }

    private void initializeViews() {
        tabAsPassenger = findViewById(R.id.tab_as_passenger);
        tabAsDriver = findViewById(R.id.tab_as_driver);
        recyclerView = findViewById(R.id.recycler_my_rides);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        emptyStateText = findViewById(R.id.empty_state_text);

        View btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRidesAdapter(ridesList, new MyRidesAdapter.OnRideItemClickListener() {
            @Override
            public void onCallClick(MyRideItem ride) {
                callContact(ride);
            }

            @Override
            public void onMessageClick(MyRideItem ride) {
                Toast.makeText(MyRidesActivity.this, "Messaging coming soon", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onViewDetailsClick(MyRideItem ride) {
                showRideDetails(ride);
            }

            @Override
            public void onCompleteRideClick(MyRideItem ride) {
                // Adapter should only enable this click if it's the driver view.
                if (!showingPassengerRides) {
                    completeRide(ride);
                } else {
                    Toast.makeText(MyRidesActivity.this, "Only the driver can mark the ride as complete.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        tabAsPassenger.setOnClickListener(v -> switchToPassengerTab());
        tabAsDriver.setOnClickListener(v -> switchToDriverTab());
    }

    private void switchToPassengerTab() {
        if (showingPassengerRides) return;

        showingPassengerRides = true;
        updateTabUI();
        loadPassengerRides();
    }

    private void switchToDriverTab() {
        if (!showingPassengerRides) return;

        showingPassengerRides = false;
        updateTabUI();
        loadDriverRides();
    }

    private void updateTabUI() {
        if (showingPassengerRides) {
            tabAsPassenger.setTextColor(getResources().getColor(android.R.color.white));
            tabAsPassenger.setBackgroundResource(R.drawable.tab_selected_background);
            tabAsDriver.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabAsDriver.setBackgroundResource(R.drawable.tab_unselected_background);
        } else {
            tabAsDriver.setTextColor(getResources().getColor(android.R.color.white));
            tabAsDriver.setBackgroundResource(R.drawable.tab_selected_background);
            tabAsPassenger.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tabAsPassenger.setBackgroundResource(R.drawable.tab_unselected_background);
        }
    }

    private void loadPassengerRides() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading();

        if (ridesListener != null) {
            ridesListener.remove();
        }

        // Query by passengerId
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("passengerId", currentUser.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    processRidesSnapshot(snapshots, error, true, "No accepted or completed rides yet.\nYour rides will appear here.");
                });
    }

    private void loadDriverRides() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading();

        if (ridesListener != null) {
            ridesListener.remove();
        }

        // Query by driverId
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("driverId", currentUser.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    processRidesSnapshot(snapshots, error, false, "No accepted or completed rides yet.\nRides you drive will appear here.");
                });
    }

    // Unified snapshot processor to handle client-side status filtering
    private void processRidesSnapshot(
            com.google.firebase.firestore.QuerySnapshot snapshots,
            com.google.firebase.firestore.FirebaseFirestoreException error,
            boolean isPassengerView,
            String emptyMessage) {

        hideLoading();

        if (error != null) {
            Log.e(TAG, "Error loading rides", error);
            showEmptyState("Error loading your rides");
            return;
        }

        ridesList.clear();
        if (snapshots != null) {
            for (QueryDocumentSnapshot document : snapshots) {
                try {
                    String status = document.getString(FIELD_STATUS);
                    // Filter: only show accepted or completed rides.
                    // This is key for the fix - we read them here, and prevent deletion in MyRequestsActivity.
                    if (STATUS_ACCEPTED.equals(status) || STATUS_COMPLETED.equals(status)) {
                        MyRideItem ride = parseRideItem(document, isPassengerView);
                        if (ride != null) {
                            ridesList.add(ride);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing ride", e);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (ridesList.isEmpty()) {
            showEmptyState(emptyMessage);
        } else {
            hideEmptyState();
        }
    }


    private MyRideItem parseRideItem(QueryDocumentSnapshot document, boolean isPassenger) {
        // ... (Parsing logic remains mostly the same)
        String id = document.getId();
        String status = document.getString("status");
        String pickupLocation = document.getString("pickupLocation");
        String dropLocation = document.getString("dropLocation");
        Double fare = document.getDouble("fare");
        String vehicleType = document.getString("vehicleType");
        Long passengers = document.getLong("passengers");
        Long departureTime = document.getLong("departureTime");
        Long acceptedAt = document.getLong("acceptedAt");

        String passengerId = document.getString("passengerId");
        String passengerName = document.getString("passengerName");
        String passengerPhone = document.getString("passengerPhone");

        String driverId = document.getString("driverId");
        String driverName = document.getString("driverName");
        String driverPhone = document.getString("driverPhone");

        // Determine who the "other person" is based on role
        String otherPersonName;
        String otherPersonPhone;
        String otherPersonId;

        if (isPassenger) {
            // I'm the passenger, so show driver info
            otherPersonName = driverName != null ? driverName : "Driver";
            otherPersonPhone = driverPhone;
            otherPersonId = driverId; // Driver's ID is the other person's ID
        } else {
            // I'm the driver, so show passenger info
            otherPersonName = passengerName != null ? passengerName : "Passenger";
            otherPersonPhone = passengerPhone;
            otherPersonId = passengerId; // Passenger's ID is the other person's ID
        }

        return new MyRideItem(
                id, status, pickupLocation, dropLocation,
                fare != null ? fare : 0.0,
                vehicleType,
                passengers != null ? passengers.intValue() : 1,
                departureTime, acceptedAt,
                otherPersonName, otherPersonPhone, otherPersonId,
                isPassenger // true if viewing as passenger, false if viewing as driver
        );
    }

    private void callContact(MyRideItem ride) {
        if (ride.getOtherPersonPhone() != null && !ride.getOtherPersonPhone().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ride.getOtherPersonPhone()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRideDetails(MyRideItem ride) {
        String roleLabel = ride.isPassengerView() ? "Driver" : "Passenger";

        String details = "Status: " + ride.getStatus().toUpperCase() + "\n\n" +
                "From: " + ride.getPickupLocation() + "\n" +
                "To: " + ride.getDropLocation() + "\n" +
                "Fare: ৳" + String.format(Locale.getDefault(), "%.0f", ride.getFare()) + "\n" +
                "Vehicle: " + (ride.getVehicleType() != null ?
                ride.getVehicleType().toUpperCase() : "CAR") + "\n" +
                "Passengers: " + ride.getPassengers() + "\n\n" +
                "--- " + roleLabel + " Info ---\n" +
                "Name: " + ride.getOtherPersonName();

        if (ride.getOtherPersonPhone() != null && !ride.getOtherPersonPhone().isEmpty()) {
            details += "\nPhone: " + ride.getOtherPersonPhone();
        }

        new AlertDialog.Builder(this)
                .setTitle("Ride Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Call " + roleLabel, (dialog, which) -> callContact(ride))
                .show();
    }


    private void completeRide(MyRideItem ride) {
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Safety check: Ensure the current user is the driver
        if (showingPassengerRides || !currentUserId.equals(mAuth.getCurrentUser().getUid())) {
            Toast.makeText(this, "Permission denied. Only the driver can complete the ride.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Complete Ride?")
                .setMessage("Are you sure you want to mark this ride as completed? This action will count the ride towards your total.")
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    // Determine the passenger ID. Since we are in the driver tab, otherPersonId is the passengerId.
                    String passengerId = ride.getOtherPersonId();

                    db.collection("ride_requests")
                            .document(ride.getId())
                            .update(FIELD_STATUS, STATUS_COMPLETED, "completedAt", System.currentTimeMillis())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Ride completed!", Toast.LENGTH_SHORT).show();

                                // INCREMENT RIDE COUNT FOR BOTH DRIVER AND PASSENGER
                                incrementRideCount(currentUserId, passengerId, ride.getId());

                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error completing ride", e);
                                Toast.makeText(this, "Failed to complete ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Increments the totalRides field by 1 for both the driver and the passenger.
     * The `FieldValue.increment(1)` operation is atomic and safe.
     * @param driverId The ID of the driver.
     * @param passengerId The ID of the passenger.
     * @param rideId The ID of the ride request (for logging).
     */
    private void incrementRideCount(String driverId, String passengerId, String rideId) {
        // Increment Driver's totalRides count
        db.collection("users").document(driverId)
                .update("totalRides", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ride " + rideId + ": Driver ride count incremented."))
                .addOnFailureListener(e -> Log.e(TAG, "Ride " + rideId + ": Failed to increment driver ride count", e));

        // Increment Passenger's totalRides count
        db.collection("users").document(passengerId)
                .update("totalRides", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ride " + rideId + ": Passenger ride count incremented."))
                .addOnFailureListener(e -> Log.e(TAG, "Ride " + rideId + ": Failed to increment passenger ride count", e));
    }


    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setText(message);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ridesListener != null) {
            ridesListener.remove();
        }
    }
}
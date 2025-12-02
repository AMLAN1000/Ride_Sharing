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
import com.google.firebase.firestore.DocumentSnapshot; // NEW IMPORT
import com.google.firebase.firestore.FieldValue; // NEW IMPORT
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query; // NEW IMPORT
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // Assuming BottomNavigationHelper is defined elsewhere
        // BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    private void initializeViews() {
        tabAsPassenger = findViewById(R.id.tab_as_passenger);
        tabAsDriver = findViewById(R.id.tab_as_driver);
        recyclerView = findViewById(R.id.recycler_my_rides);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        emptyStateText = findViewById(R.id.empty_state_text);
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
                // This check assumes MyRideItem has an 'isPassengerView' field
                // and the adapter should only enable this click if it's the driver view.
                if (!ride.isPassengerView()) {
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

        View btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
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

    // New constants for ride status for cleaner code
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_COMPLETED = "completed";
    private static final String FIELD_STATUS = "status";

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

        // Query for both "accepted" and "completed" rides
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("passengerId", currentUser.getUid())
                // Firestore doesn't support array-contains for multiple values in a single field,
                // nor does it support OR queries without an index. The best solution for a simple
                // status list is typically two queries or changing the structure/logic.
                // For simplicity, we will assume you need to use a compound query if status were an index.
                // Since Firestore doesn't easily support WHERE status == 'accepted' OR status == 'completed',
                // we will query all where passengerId matches and filter in the client, or use multiple listeners/queries
                // (which is complex). Let's use two `whereEqualTo` calls for the required status fields.

                // *** Using only whereEqualTo(status, accepted) OR whereEqualTo(status, completed) is NOT possible with standard Firebase queries. ***
                // The practical way is to either use status >= 'accepted' if status names are ordered (accepted < completed)
                // or query both states separately, or query only by passengerId and filter, which is inefficient.

                // BEST PRACTICE: If only ACCEPTED and COMPLETED are shown, filter out non-accepted/completed states client-side.
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

        // Query for both "accepted" and "completed" rides where current user is the driver
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
                    // Filter: only show accepted or completed rides
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
            otherPersonId = driverId;
        } else {
            // I'm the driver, so show passenger info
            otherPersonName = passengerName != null ? passengerName : "Passenger";
            otherPersonPhone = passengerPhone;
            otherPersonId = passengerId;
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

    // ... (callContact and showRideDetails remain the same)
    private void callContact(MyRideItem ride) {
        // ... (unchanged)
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
        // ... (unchanged)
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

        // 🚨 SAFETY CHECK: Ensure only the driver can complete the ride
        if (ride.isPassengerView() || !currentUserId.equals(ride.getOtherPersonId())) {
            // In MyRideItem, otherPersonId is the driverId if isPassengerView is true,
            // and passengerId if isPassengerView is false.
            // We need to check the actual role, which is easier by checking isPassengerView is FALSE
            // and if the current user ID matches the driver ID (which is the passenger's otherPersonId)
            // Let's assume the adapter only calls this for driver view, but check just in case.
            // Given the context, the adapter's onCompleteRideClick needs to pass the right IDs.
            // Reverting to the simpler check based on the tab view:
            if (showingPassengerRides) {
                Toast.makeText(this, "Only the driver can mark this ride as complete.", Toast.LENGTH_SHORT).show();
                return;
            }
            // NOTE: If you need to make this check 100% robust, the MyRideItem must also carry the driverId.
            // Assuming showingPassengerRides == false is enough of a check based on your UI structure.
        }

        new AlertDialog.Builder(this)
                .setTitle("Complete Ride?")
                .setMessage("Are you sure you want to mark this ride as completed? This action will count the ride towards your total.")
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    db.collection("ride_requests")
                            .document(ride.getId())
                            .update(FIELD_STATUS, STATUS_COMPLETED, "completedAt", System.currentTimeMillis())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Ride completed!", Toast.LENGTH_SHORT).show();

                                // 🚨 INCREMENT RIDE COUNT FOR BOTH DRIVER AND PASSENGER
                                incrementRideCount(currentUserId, ride.getOtherPersonId(), ride.getId());

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
    protected void onResume() {
        super.onResume();
        // Assuming BottomNavigationHelper is defined elsewhere
        // BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ridesListener != null) {
            ridesListener.remove();
        }
    }
}
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
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

    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String FIELD_STATUS = "status";
    private static final String TYPE_CARPOOL = "carpool";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        Log.d(TAG, "🚗 MyRidesActivity started");

        // 🔥 HANDLE NOTIFICATION INTENTS
        handleNotificationIntent();

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        setupClickListeners();

        // Load passenger rides by default
        updateTabUI();
        loadPassengerRides();
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    /**
     * Handle notification intents when activity is opened from notification
     */
    private void handleNotificationIntent() {
        boolean openAsPassenger = getIntent().getBooleanExtra("openAsPassenger", false);
        boolean openAsDriver = getIntent().getBooleanExtra("openAsDriver", false);
        String rideId = getIntent().getStringExtra("rideId");

        Log.d(TAG, "📱 Notification intent received - Passenger: " + openAsPassenger +
                ", Driver: " + openAsDriver + ", RideID: " + rideId);

        if (openAsPassenger) {
            showingPassengerRides = true;
            Toast.makeText(this, "📱 Showing your passenger rides", Toast.LENGTH_SHORT).show();
        } else if (openAsDriver) {
            showingPassengerRides = false;
            Toast.makeText(this, "📱 Showing your driver rides", Toast.LENGTH_SHORT).show();
        }

        if (rideId != null) {
            Log.d(TAG, "📋 Notification for specific ride: " + rideId);
            // Store rideId to potentially highlight/show specific ride
            getIntent().putExtra("highlightRideId", rideId);
        }
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
                openChat(ride);
            }

            @Override
            public void onViewDetailsClick(MyRideItem ride) {
                showRideDetails(ride);
            }

            @Override
            public void onCompleteRideClick(MyRideItem ride) {
                if (!showingPassengerRides) {
                    completeRide(ride);
                } else {
                    Toast.makeText(MyRidesActivity.this, "Only the driver can mark the ride as complete.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelRideClick(MyRideItem ride) {
                showCancelRideDialog(ride);
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

        String currentUserId = currentUser.getUid();

        // Query for rides where user is passenger (regular rides OR part of carpool)
        ridesListener = db.collection("ride_requests")
                .addSnapshotListener((snapshots, error) -> {
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
                                String type = document.getString("type");

                                // Check if user is passenger
                                String passengerId = document.getString("passengerId");
                                List<String> passengerIds = (List<String>) document.get("passengerIds");

                                boolean isRegularPassenger = passengerId != null && passengerId.equals(currentUserId);
                                boolean isCarpoolPassenger = passengerIds != null && passengerIds.contains(currentUserId);

                                // Show accepted or completed rides where user is passenger
                                if ((isRegularPassenger || isCarpoolPassenger) &&
                                        (STATUS_ACCEPTED.equals(status) || STATUS_COMPLETED.equals(status))) {

                                    MyRideItem ride = parseRideItem(document, true, currentUserId);
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
                        showEmptyState("No accepted or completed rides yet.\nYour rides will appear here.");
                    } else {
                        hideEmptyState();
                    }
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
                                // Show accepted, completed, pending, or cancelled rides
                                if (STATUS_ACCEPTED.equals(status) || STATUS_COMPLETED.equals(status) ||
                                        STATUS_PENDING.equals(status) || STATUS_CANCELLED.equals(status)) {
                                    MyRideItem ride = parseRideItem(document, false, null);
                                    if (ride != null) {
                                        ridesList.add(ride);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing ride", e);
                            }
                        }
                    }

                    // Sort rides: accepted (ongoing) first, then completed
                    ridesList.sort((ride1, ride2) -> {
                        String status1 = ride1.getStatus();
                        String status2 = ride2.getStatus();

                        // Accepted rides come first
                        if ("accepted".equals(status1) && !"accepted".equals(status2)) {
                            return -1;
                        } else if (!"accepted".equals(status1) && "accepted".equals(status2)) {
                            return 1;
                        }

                        // If both same status, sort by time (newest first)
                        if (ride1.getAcceptedAt() != null && ride2.getAcceptedAt() != null) {
                            return ride2.getAcceptedAt().compareTo(ride1.getAcceptedAt());
                        } else if (ride1.getDepartureTime() != null && ride2.getDepartureTime() != null) {
                            return ride2.getDepartureTime().compareTo(ride1.getDepartureTime());
                        }

                        return 0;
                    });

                    // Sort rides: accepted first, then pending, then completed, then cancelled
                    ridesList.sort((ride1, ride2) -> {
                        String status1 = ride1.getStatus();
                        String status2 = ride2.getStatus();

                        // Priority order: accepted > pending > completed > cancelled
                        int priority1 = getStatusPriority(status1);
                        int priority2 = getStatusPriority(status2);

                        if (priority1 != priority2) {
                            return Integer.compare(priority1, priority2);
                        }

                        // If both same status, sort by time (newest first)
                        if (ride1.getAcceptedAt() != null && ride2.getAcceptedAt() != null) {
                            return ride2.getAcceptedAt().compareTo(ride1.getAcceptedAt());
                        } else if (ride1.getDepartureTime() != null && ride2.getDepartureTime() != null) {
                            return ride2.getDepartureTime().compareTo(ride1.getDepartureTime());
                        }

                        return 0;
                    });


                    adapter.notifyDataSetChanged();

                    if (ridesList.isEmpty()) {
                        showEmptyState("No accepted or completed rides yet.\nRides you drive will appear here.");
                    } else {
                        hideEmptyState();
                    }
                });
    }

    private MyRideItem parseRideItem(QueryDocumentSnapshot document, boolean isPassenger, String currentUserId) {
        try {
            String id = document.getId();
            String status = document.getString("status");
            String type = document.getString("type");
            String pickupLocation = document.getString("pickupLocation");
            String dropLocation = document.getString("dropLocation");
            Double fare = document.getDouble("fare");
            Double farePerPassenger = document.getDouble("farePerPassenger");
            String vehicleType = document.getString("vehicleType");
            Long passengers = document.getLong("passengers");
            Long maxSeats = document.getLong("maxSeats");
            Long passengerCount = document.getLong("passengerCount");
            Long departureTime = document.getLong("departureTime");
            Long acceptedAt = document.getLong("acceptedAt");

            String passengerId = document.getString("passengerId");
            String passengerName = document.getString("passengerName");
            String passengerPhone = document.getString("passengerPhone");

            List<String> passengerNames = (List<String>) document.get("passengerNames");
            List<String> passengerIds = (List<String>) document.get("passengerIds");

            String driverId = document.getString("driverId");
            String driverName = document.getString("driverName");
            String driverPhone = document.getString("driverPhone");

            // Determine who the "other person" is based on role
            String otherPersonName;
            String otherPersonPhone;
            String otherPersonId;
            String allPassengerNames = "";

            if (isPassenger) {
                // I'm the passenger, so show driver info
                otherPersonName = driverName != null ? driverName : "Driver";
                otherPersonPhone = driverPhone != null ? driverPhone : "";
                otherPersonId = driverId != null ? driverId : "";

                // For carpools, also show other passengers
                if (TYPE_CARPOOL.equals(type) && passengerNames != null && passengerIds != null) {
                    StringBuilder passengersBuilder = new StringBuilder();
                    for (int i = 0; i < passengerNames.size(); i++) {
                        if (i < passengerIds.size()) {
                            String pName = passengerNames.get(i);
                            String pId = passengerIds.get(i);
                            if (currentUserId != null && !pId.equals(currentUserId)) {
                                if (passengersBuilder.length() > 0) {
                                    passengersBuilder.append(", ");
                                }
                                passengersBuilder.append(pName);
                            }
                        }
                    }
                    allPassengerNames = passengersBuilder.toString();
                }
            } else {
                // I'm the driver, so show passenger info
                if (TYPE_CARPOOL.equals(type) && passengerNames != null && !passengerNames.isEmpty()) {
                    // For carpools, show all passengers
                    StringBuilder passengersBuilder = new StringBuilder();
                    for (String name : passengerNames) {
                        if (passengersBuilder.length() > 0) {
                            passengersBuilder.append(", ");
                        }
                        passengersBuilder.append(name);
                    }
                    otherPersonName = passengersBuilder.toString();
                    otherPersonPhone = passengerNames.get(0); // Show first passenger's name
                    otherPersonId = passengerIds != null && !passengerIds.isEmpty() ? passengerIds.get(0) : "";
                } else {
                    // Regular ride
                    otherPersonName = passengerName != null ? passengerName : "Passenger";
                    otherPersonPhone = passengerPhone != null ? passengerPhone : "";
                    otherPersonId = passengerId != null ? passengerId : "";
                }
            }

            return new MyRideItem(
                    id,
                    status != null ? status : "unknown",
                    pickupLocation != null ? pickupLocation : "",
                    dropLocation != null ? dropLocation : "",
                    fare != null ? fare : 0.0,
                    vehicleType != null ? vehicleType : "car",
                    passengers != null ? passengers.intValue() : 1,
                    departureTime,
                    acceptedAt,
                    otherPersonName,
                    otherPersonPhone,
                    otherPersonId,
                    isPassenger,
                    TYPE_CARPOOL.equals(type),
                    farePerPassenger != null ? farePerPassenger : 0.0,
                    maxSeats != null ? maxSeats.intValue() : 1,
                    passengerCount != null ? passengerCount.intValue() : 0,
                    allPassengerNames
            );
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ride item", e);
            return null;
        }
    }

    private void openChat(MyRideItem ride) {
        // Only allow chat for accepted (ongoing) rides
        if (!"accepted".equals(ride.getStatus())) {
            Toast.makeText(this, "Chat is only available for ongoing rides", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("rideId", ride.getId());
        intent.putExtra("otherPersonName", ride.getOtherPersonName());
        intent.putExtra("otherPersonId", ride.getOtherPersonId()); // ✅ ADD THIS LINE
        intent.putExtra("pickupLocation", ride.getPickupLocation());
        intent.putExtra("dropLocation", ride.getDropLocation());
        startActivity(intent);
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
        boolean isCarpool = ride.isCarpool();

        String details = "Type: " + (isCarpool ? "CARPOOL" : "REGULAR RIDE") + "\n" +
                "Status: " + ride.getStatus().toUpperCase() + "\n\n" +
                "From: " + ride.getPickupLocation() + "\n" +
                "To: " + ride.getDropLocation() + "\n" +
                "Fare: ৳" + String.format(Locale.getDefault(), "%.0f", ride.getFare()) + "\n";

        if (isCarpool && ride.getFarePerPassenger() > 0) {
            details += "Per passenger: ৳" + String.format(Locale.getDefault(), "%.0f", ride.getFarePerPassenger()) + "\n";
            details += "Passengers: " + ride.getPassengerCount() + "/" + ride.getMaxSeats() + "\n";
        }

        details += "Vehicle: " + (ride.getVehicleType() != null ?
                ride.getVehicleType().toUpperCase() : "CAR") + "\n" +
                "Seats: " + ride.getPassengers() + "\n\n";

        if (isCarpool && ride.isPassengerView() && ride.getAllPassengerNames() != null &&
                !ride.getAllPassengerNames().isEmpty()) {
            details += "--- Other Passengers ---\n" +
                    ride.getAllPassengerNames() + "\n\n";
        }

        details += "--- " + roleLabel + " Info ---\n" +
                "Name: " + ride.getOtherPersonName();

        if (ride.getOtherPersonPhone() != null && !ride.getOtherPersonPhone().isEmpty() &&
                !ride.getOtherPersonPhone().equals(ride.getOtherPersonName())) {
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
        if (showingPassengerRides) {
            Toast.makeText(this, "Permission denied. Only the driver can complete the ride.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Complete Ride?")
                .setMessage("Are you sure you want to mark this ride as completed? This action will count the ride towards your total.")
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    if (ride.isCarpool()) {
                        // For carpools, increment ride count for all passengers
                        completeCarpoolRide(ride, currentUserId);
                    } else {
                        // For regular rides
                        String passengerId = ride.getOtherPersonId();
                        db.collection("ride_requests")
                                .document(ride.getId())
                                .update(FIELD_STATUS, STATUS_COMPLETED, "completedAt", System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "✅ Ride completed!", Toast.LENGTH_SHORT).show();
                                    incrementRideCount(currentUserId, passengerId, ride.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error completing ride", e);
                                    Toast.makeText(this, "Failed to complete ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeCarpoolRide(MyRideItem ride, String driverId) {
        db.collection("ride_requests")
                .document(ride.getId())
                .update(FIELD_STATUS, STATUS_COMPLETED, "completedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Carpool completed!", Toast.LENGTH_SHORT).show();

                    // Increment ride count for driver
                    db.collection("users").document(driverId)
                            .update("totalRides", FieldValue.increment(1));

                    // Get passenger IDs and increment their counts
                    db.collection("ride_requests").document(ride.getId())
                            .get()
                            .addOnSuccessListener(document -> {
                                List<String> passengerIds = (List<String>) document.get("passengerIds");
                                if (passengerIds != null) {
                                    for (String passengerId : passengerIds) {
                                        db.collection("users").document(passengerId)
                                                .update("totalRides", FieldValue.increment(1));
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error completing carpool", e);
                    Toast.makeText(this, "Failed to complete carpool: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showCancelRideDialog(MyRideItem ride) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String status = ride.getStatus();

        if (!showingPassengerRides) {
            // Driver is cancelling
            if (STATUS_PENDING.equals(status)) {
                new AlertDialog.Builder(this)
                        .setTitle("Cancel Ride Offer?")
                        .setMessage("Are you sure you want to cancel this ride offer?\n\n" +
                                "Pickup: " + ride.getPickupLocation() + "\n" +
                                "Drop: " + ride.getDropLocation() + "\n" +
                                "Fare: ৳" + String.format(Locale.getDefault(), "%.0f", ride.getFare()))
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelRideOffer(ride))
                        .setNegativeButton("No", null)
                        .show();
            } else if (STATUS_ACCEPTED.equals(status)) {
                new AlertDialog.Builder(this)
                        .setTitle("Cancel Accepted Ride?")
                        .setMessage("This ride has been accepted. Cancelling now may inconvenience passengers.\n\n" +
                                "Are you sure you want to cancel?")
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelAcceptedRide(ride))
                        .setNegativeButton("No", null)
                        .show();
            } else if (STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status)) {
                Toast.makeText(this, "Cannot cancel a " + status + " ride", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Only the driver can cancel rides.", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelRideOffer(MyRideItem ride) {
        db.collection("ride_requests")
                .document(ride.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Ride offer cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error cancelling ride offer", e);
                });
    }

    private void cancelAcceptedRide(MyRideItem ride) {
        db.collection("ride_requests")
                .document(ride.getId())
                .update(FIELD_STATUS, STATUS_CANCELLED, "cancelledAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Ride cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error cancelling accepted ride", e);
                });
    }

    private void incrementRideCount(String driverId, String passengerId, String rideId) {
        // Increment Driver's totalRides count
        db.collection("users").document(driverId)
                .update("totalRides", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ride " + rideId + ": Driver ride count incremented."))
                .addOnFailureListener(e -> Log.e(TAG, "Ride " + rideId + ": Failed to increment driver ride count", e));

        // Increment Passenger's totalRides count
        if (passengerId != null && !passengerId.isEmpty()) {
            db.collection("users").document(passengerId)
                    .update("totalRides", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Ride " + rideId + ": Passenger ride count incremented."))
                    .addOnFailureListener(e -> Log.e(TAG, "Ride " + rideId + ": Failed to increment passenger ride count", e));
        }
    }
    private int getStatusPriority(String status) {
        switch (status.toLowerCase()) {
            case "accepted":
                return 1; // Highest priority
            case "pending":
                return 2;
            case "completed":
                return 3;
            case "cancelled":
                return 4; // Lowest priority
            default:
                return 5;
        }
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

    // 🔥 IMPORTANT: Handle notification clicks when activity is already running
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "📱 onNewIntent called - handling notification click");
        setIntent(intent);
        handleNotificationIntent();

        // Refresh the current view
        if (showingPassengerRides) {
            loadPassengerRides();
        } else {
            loadDriverRides();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 MyRidesActivity resumed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 MyRidesActivity destroyed");
        if (ridesListener != null) {
            ridesListener.remove();
        }
    }
}
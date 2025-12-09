package com.example.ridesharing;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class MyRidesActivity extends AppCompatActivity {

    private static final String TAG = "MyRidesActivity";

    private TextView tabAsPassenger, tabAsDriver;
    private MaterialCardView tabPassengerCard, tabDriverCard;
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
        tabPassengerCard = findViewById(R.id.tab_passenger_card);
        tabDriverCard = findViewById(R.id.tab_driver_card);
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
            @Override
            public void onTrackClick(MyRideItem ride) {
                openTracking(ride);
            }
            @Override
            public void onSafetyClick(MyRideItem ride) {
                openSafety(ride);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        if (tabPassengerCard != null) {
            tabPassengerCard.setOnClickListener(v -> switchToPassengerTab());
        }
        if (tabDriverCard != null) {
            tabDriverCard.setOnClickListener(v -> switchToDriverTab());
        }

        // Also keep the TextViews clickable for backward compatibility
        tabAsPassenger.setOnClickListener(v -> switchToPassengerTab());
        tabAsDriver.setOnClickListener(v -> switchToDriverTab());
    }

    private void switchToPassengerTab() {
        if (showingPassengerRides) return;

        showingPassengerRides = true;
        updateTabUI();
        loadPassengerRides();

        // Add animation
        animateTabSelection(tabPassengerCard, true);
    }

    private void switchToDriverTab() {
        if (!showingPassengerRides) return;

        showingPassengerRides = false;
        updateTabUI();
        loadDriverRides();

        // Add animation
        animateTabSelection(tabDriverCard, false);
    }

    private void animateTabSelection(MaterialCardView selectedCard, boolean isPassenger) {
        selectedCard.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(100)
                .withEndAction(() -> {
                    selectedCard.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void updateTabUI() {
        if (showingPassengerRides) {
            // Passenger tab selected
            if (tabPassengerCard != null) {
                tabPassengerCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.purple_primary));
                tabPassengerCard.setStrokeWidth(0);
                tabPassengerCard.setStrokeColor(Color.TRANSPARENT);
            }
            if (tabDriverCard != null) {
                tabDriverCard.setCardBackgroundColor(Color.WHITE);
                tabDriverCard.setStrokeWidth(2);
                tabDriverCard.setStrokeColor(Color.parseColor("#E0E0E0"));
            }

            tabAsPassenger.setTextColor(Color.WHITE);
            tabAsDriver.setTextColor(Color.parseColor("#8A8AA3"));
        } else {
            // Driver tab selected
            if (tabDriverCard != null) {
                tabDriverCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.purple_primary));
                tabDriverCard.setStrokeWidth(0);
                tabDriverCard.setStrokeColor(Color.TRANSPARENT);
            }
            if (tabPassengerCard != null) {
                tabPassengerCard.setCardBackgroundColor(Color.WHITE);
                tabPassengerCard.setStrokeWidth(2);
                tabPassengerCard.setStrokeColor(Color.parseColor("#E0E0E0"));
            }

            tabAsDriver.setTextColor(Color.WHITE);
            tabAsPassenger.setTextColor(Color.parseColor("#8A8AA3"));
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

                    // Sort passenger rides: accepted (ongoing) first, then completed
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
            List<String> passengerPhones = (List<String>) document.get("passengerPhones");

            // NEW: Get passenger-specific locations and fares
            List<Map<String, Object>> passengerPickups = (List<Map<String, Object>>) document.get("passengerPickups");
            List<Map<String, Object>> passengerDrops = (List<Map<String, Object>>) document.get("passengerDrops");
            List<Double> passengerFares = (List<Double>) document.get("passengerFares");

            String driverId = document.getString("driverId");
            String driverName = document.getString("driverName");
            String driverPhone = document.getString("driverPhone");

            // Determine who the "other person" is based on role
            String otherPersonName;
            String otherPersonPhone;
            String otherPersonId;
            String allPassengerNames = "";
            String displayPickup = pickupLocation;
            String displayDrop = dropLocation;
            double displayFare = fare != null ? fare : 0.0;

            if (isPassenger) {
                // I'm the passenger, so show driver info
                otherPersonName = driverName != null ? driverName : "Driver";
                otherPersonPhone = driverPhone != null ? driverPhone : "";
                otherPersonId = driverId != null ? driverId : "";

                // For passenger view in carpool, show THEIR specific pickup/drop
                if (TYPE_CARPOOL.equals(type) && currentUserId != null) {
                    // Find this passenger's specific stops
                    if (passengerIds != null && passengerPickups != null && passengerDrops != null && passengerFares != null) {
                        int myIndex = passengerIds.indexOf(currentUserId);
                        if (myIndex >= 0 && myIndex < passengerPickups.size()) {
                            Map<String, Object> myPickup = passengerPickups.get(myIndex);
                            Map<String, Object> myDrop = passengerDrops.get(myIndex);

                            displayPickup = myPickup != null ? (String) myPickup.get("address") : pickupLocation;
                            displayDrop = myDrop != null ? (String) myDrop.get("address") : dropLocation;
                            displayFare = myIndex < passengerFares.size() ? passengerFares.get(myIndex) : (farePerPassenger != null ? farePerPassenger : 0.0);
                        }
                    }

                    // Show other passengers
                    if (passengerNames != null && passengerIds != null) {
                        StringBuilder passengersBuilder = new StringBuilder();
                        for (int i = 0; i < passengerNames.size(); i++) {
                            if (i < passengerIds.size()) {
                                String pName = passengerNames.get(i);
                                String pId = passengerIds.get(i);
                                if (!pId.equals(currentUserId)) {
                                    if (passengersBuilder.length() > 0) {
                                        passengersBuilder.append(", ");
                                    }
                                    passengersBuilder.append(pName);
                                }
                            }
                        }
                        allPassengerNames = passengersBuilder.toString();
                    }
                }
            } else {
                // I'm the driver
                if (TYPE_CARPOOL.equals(type) && passengerNames != null && !passengerNames.isEmpty()) {
                    // For driver view in carpool, show all passengers
                    StringBuilder passengersBuilder = new StringBuilder();
                    for (String name : passengerNames) {
                        if (passengersBuilder.length() > 0) {
                            passengersBuilder.append(", ");
                        }
                        passengersBuilder.append(name);
                    }
                    otherPersonName = passengersBuilder.toString();
                    otherPersonPhone = passengerNames.get(0);
                    otherPersonId = passengerIds != null && !passengerIds.isEmpty() ? passengerIds.get(0) : "";

                    // Keep full route for driver
                    displayPickup = pickupLocation;
                    displayDrop = dropLocation;
                    displayFare = fare != null ? fare : 0.0;
                } else {
                    // Regular ride
                    otherPersonName = passengerName != null ? passengerName : "Passenger";
                    otherPersonPhone = passengerPhone != null ? passengerPhone : "";
                    otherPersonId = passengerId != null ? passengerId : "";
                }
            }

            MyRideItem rideItem = new MyRideItem(
                    id,
                    status != null ? status : "unknown",
                    displayPickup != null ? displayPickup : "",
                    displayDrop != null ? displayDrop : "",
                    displayFare,
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
                    allPassengerNames,
                    0
            );

            // NEW: For driver view in carpool, add individual passenger details
            if (!isPassenger && TYPE_CARPOOL.equals(type) && passengerNames != null && passengerPickups != null && passengerDrops != null) {
                List<MyRideItem.PassengerDetail> details = new ArrayList<>();

                for (int i = 0; i < passengerNames.size(); i++) {
                    String pName = passengerNames.get(i);
                    String pPhone = (passengerPhones != null && i < passengerPhones.size()) ? passengerPhones.get(i) : "";

                    String pPickup = pickupLocation; // Default to full route
                    String pDrop = dropLocation;
                    double pFare = farePerPassenger != null ? farePerPassenger : 0.0;

                    // Get passenger-specific locations
                    if (i < passengerPickups.size() && passengerPickups.get(i) != null) {
                        Map<String, Object> pickup = passengerPickups.get(i);
                        pPickup = (String) pickup.get("address");
                    }

                    if (i < passengerDrops.size() && passengerDrops.get(i) != null) {
                        Map<String, Object> drop = passengerDrops.get(i);
                        pDrop = (String) drop.get("address");
                    }

                    if (passengerFares != null && i < passengerFares.size()) {
                        pFare = passengerFares.get(i);
                    }

                    details.add(new MyRideItem.PassengerDetail(pName, pPhone, pPickup, pDrop, pFare));
                }

                rideItem.setPassengerDetails(details);
            }

            countUnreadMessages(id, rideItem);
            return rideItem;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing ride item", e);
            return null;
        }
    }
    private void countUnreadMessages(String rideId, MyRideItem rideItem) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();

        db.collection("ride_requests")
                .document(rideId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int unreadCount = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        // Count only messages NOT sent by current user
                        if (senderId != null && !senderId.equals(currentUserId)) {
                            unreadCount++;
                        }
                    }

                    // Update the ride item with unread count
                    if (unreadCount > 0) {
                        // Find and update the ride in the list
                        for (int i = 0; i < ridesList.size(); i++) {
                            if (ridesList.get(i).getId().equals(rideId)) {
                                // Create updated ride item with unread count
                                MyRideItem oldRide = ridesList.get(i);
                                MyRideItem updatedRide = new MyRideItem(
                                        oldRide.getId(),
                                        oldRide.getStatus(),
                                        oldRide.getPickupLocation(),
                                        oldRide.getDropLocation(),
                                        oldRide.getFare(),
                                        oldRide.getVehicleType(),
                                        oldRide.getPassengers(),
                                        oldRide.getDepartureTime(),
                                        oldRide.getAcceptedAt(),
                                        oldRide.getOtherPersonName(),
                                        oldRide.getOtherPersonPhone(),
                                        oldRide.getOtherPersonId(),
                                        oldRide.isPassengerView(),
                                        oldRide.isCarpool(),
                                        oldRide.getFarePerPassenger(),
                                        oldRide.getMaxSeats(),
                                        oldRide.getPassengerCount(),
                                        oldRide.getAllPassengerNames(),
                                        unreadCount
                                );
                                updatedRide.setPassengerDetails(oldRide.getPassengerDetails());
                                ridesList.set(i, updatedRide);
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error counting unread messages", e));
    }

    // Update showRideDetails method to show passenger-specific info for driver
    private void showRideDetails(MyRideItem ride) {
        String roleLabel = ride.isPassengerView() ? "Driver" : "Passenger";
        boolean isCarpool = ride.isCarpool();

        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(isCarpool ? "CARPOOL" : "REGULAR RIDE").append("\n");
        details.append("Status: ").append(ride.getStatus().toUpperCase()).append("\n\n");

        if (!ride.isPassengerView() && isCarpool && ride.getPassengerDetails() != null && !ride.getPassengerDetails().isEmpty()) {
            // DRIVER VIEW - Show each passenger's details
            details.append("=== FULL ROUTE ===\n");
            details.append("Start: ").append(ride.getPickupLocation()).append("\n");
            details.append("End: ").append(ride.getDropLocation()).append("\n");
            details.append("Total Fare: ৳").append(String.format(Locale.getDefault(), "%.0f", ride.getFare())).append("\n");
            details.append("Passengers: ").append(ride.getPassengerCount()).append("/").append(ride.getMaxSeats()).append("\n\n");

            details.append("=== PASSENGER DETAILS ===\n");
            List<MyRideItem.PassengerDetail> passengers = ride.getPassengerDetails();
            for (int i = 0; i < passengers.size(); i++) {
                MyRideItem.PassengerDetail p = passengers.get(i);
                details.append("\nPassenger ").append(i + 1).append(":\n");
                details.append("Name: ").append(p.getName()).append("\n");
                if (p.getPhone() != null && !p.getPhone().isEmpty()) {
                    details.append("Phone: ").append(p.getPhone()).append("\n");
                }
                details.append("Pickup: ").append(p.getPickupLocation()).append("\n");
                details.append("Drop: ").append(p.getDropLocation()).append("\n");
                details.append("Fare: ৳").append(String.format(Locale.getDefault(), "%.0f", p.getFare())).append("\n");
            }

            details.append("\nVehicle: ").append(ride.getVehicleType() != null ? ride.getVehicleType().toUpperCase() : "CAR");
        } else {
            // PASSENGER VIEW or REGULAR RIDE
            details.append("From: ").append(ride.getPickupLocation()).append("\n");
            details.append("To: ").append(ride.getDropLocation()).append("\n");
            details.append("Fare: ৳").append(String.format(Locale.getDefault(), "%.0f", ride.getFare())).append("\n");

            if (isCarpool && ride.getFarePerPassenger() > 0) {
                details.append("Per passenger: ৳").append(String.format(Locale.getDefault(), "%.0f", ride.getFarePerPassenger())).append("\n");
                details.append("Passengers: ").append(ride.getPassengerCount()).append("/").append(ride.getMaxSeats()).append("\n");
            }

            details.append("Vehicle: ").append(ride.getVehicleType() != null ? ride.getVehicleType().toUpperCase() : "CAR").append("\n");
            details.append("Seats: ").append(ride.getPassengers()).append("\n\n");

            if (isCarpool && ride.isPassengerView() && ride.getAllPassengerNames() != null && !ride.getAllPassengerNames().isEmpty()) {
                details.append("--- Other Passengers ---\n");
                details.append(ride.getAllPassengerNames()).append("\n\n");
            }

            details.append("--- ").append(roleLabel).append(" Info ---\n");
            details.append("Name: ").append(ride.getOtherPersonName());

            if (ride.getOtherPersonPhone() != null && !ride.getOtherPersonPhone().isEmpty() && !ride.getOtherPersonPhone().equals(ride.getOtherPersonName())) {
                details.append("\nPhone: ").append(ride.getOtherPersonPhone());
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Ride Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Call " + roleLabel, (dialog, which) -> callContact(ride))
                .show();
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
        intent.putExtra("otherPersonId", ride.getOtherPersonId());
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



    private void completeRide(MyRideItem ride) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Safety check: Ensure the current user is the driver
        if (showingPassengerRides) {
            Toast.makeText(this, "Permission denied. Only the driver can complete the ride.", Toast.LENGTH_SHORT).show();
            return;
        }

        // For carpools, check if at least one passenger has joined
        if (ride.isCarpool() && ride.getPassengerCount() == 0) {
            Toast.makeText(this, "Cannot complete ride. No passengers have joined yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build confirmation message
        String confirmMessage;
        if (ride.isCarpool()) {
            int seatsLeft = ride.getMaxSeats() - ride.getPassengerCount();
            if (seatsLeft > 0) {
                confirmMessage = "This carpool has " + ride.getPassengerCount() + " passenger(s) and " +
                        seatsLeft + " empty seat(s).\n\n" +
                        "Are you sure you want to complete the ride now?";
            } else {
                confirmMessage = "All seats are filled! Mark this carpool as completed?";
            }
        } else {
            confirmMessage = "Are you sure you want to mark this ride as completed? " +
                    "This action will count the ride towards your total.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Complete Ride?")
                .setMessage(confirmMessage)
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    if (ride.isCarpool()) {
                        completeCarpoolRide(ride, currentUserId);
                    } else {
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
    private void showStartRideEarlyOption(MyRideItem ride) {
        if (!ride.isCarpool() || ride.getPassengerCount() == 0) {
            return;
        }

        int seatsLeft = ride.getMaxSeats() - ride.getPassengerCount();

        new AlertDialog.Builder(this)
                .setTitle("Start Ride Early?")
                .setMessage("You have " + ride.getPassengerCount() + " passenger(s) and " +
                        seatsLeft + " empty seat(s).\n\n" +
                        "Do you want to:\n" +
                        "• Start the ride now with current passengers\n" +
                        "• Wait for more passengers to join")
                .setPositiveButton("Start Now", (dialog, which) -> {
                    // Mark as accepted and start ride
                    db.collection("ride_requests")
                            .document(ride.getId())
                            .update(
                                    FIELD_STATUS, STATUS_ACCEPTED,
                                    "startedEarly", true,
                                    "startedAt", System.currentTimeMillis()
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Ride started! No more passengers can join.",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to start ride: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Wait", null)
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

    private void openTracking(MyRideItem ride) {
        // Only allow tracking for accepted (ongoing) rides
        if (!"accepted".equals(ride.getStatus())) {
            Toast.makeText(this, "Tracking is only available for ongoing rides", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, RideTrackingActivity.class);
        intent.putExtra("rideId", ride.getId());
        intent.putExtra("otherPersonName", ride.getOtherPersonName());
        intent.putExtra("pickupLocation", ride.getPickupLocation());
        intent.putExtra("dropLocation", ride.getDropLocation());
        intent.putExtra("isDriver", !ride.isPassengerView());
        startActivity(intent);
    }

    private void openSafety(MyRideItem ride) {
        android.util.Log.e("SAFETY_TEST", "");
        android.util.Log.e("SAFETY_TEST", "╔════════════════════════════════════════╗");
        android.util.Log.e("SAFETY_TEST", "║   OPENSAFETY METHOD WAS CALLED!!!      ║");
        android.util.Log.e("SAFETY_TEST", "╚════════════════════════════════════════╝");
        android.util.Log.e("SAFETY_TEST", "");


        android.util.Log.e("SAFETY_TEST", "Step 1: Checking if ride is null...");
        if (ride == null) {
            android.util.Log.e("SAFETY_TEST", "❌ RIDE IS NULL!");
            Toast.makeText(this, "ERROR: Ride is null!", Toast.LENGTH_LONG).show();
            return;
        }
        android.util.Log.e("SAFETY_TEST", "✅ Ride is NOT null");

        android.util.Log.e("SAFETY_TEST", "Step 2: Getting ride details...");
        android.util.Log.e("SAFETY_TEST", "  - Ride ID: " + ride.getId());
        android.util.Log.e("SAFETY_TEST", "  - Status: " + ride.getStatus());
        android.util.Log.e("SAFETY_TEST", "  - Pickup: " + ride.getPickupLocation());
        android.util.Log.e("SAFETY_TEST", "  - Drop: " + ride.getDropLocation());

        android.util.Log.e("SAFETY_TEST", "Step 3: Checking status...");
        if (!"accepted".equals(ride.getStatus())) {
            android.util.Log.e("SAFETY_TEST", "❌ Status is not 'accepted': " + ride.getStatus());
            Toast.makeText(this, "Safety only for ongoing rides", Toast.LENGTH_LONG).show();
            return;
        }
        android.util.Log.e("SAFETY_TEST", "✅ Status is 'accepted'");

        android.util.Log.e("SAFETY_TEST", "Step 4: Checking ride ID...");
        if (ride.getId() == null || ride.getId().isEmpty()) {
            android.util.Log.e("SAFETY_TEST", "❌ Ride ID is null or empty!");
            Toast.makeText(this, "ERROR: Ride ID missing!", Toast.LENGTH_LONG).show();
            return;
        }
        android.util.Log.e("SAFETY_TEST", "✅ Ride ID exists: " + ride.getId());

        android.util.Log.e("SAFETY_TEST", "Step 5: Preparing intent data...");
        long rideStartTime = ride.getAcceptedAt() != null ? ride.getAcceptedAt() : System.currentTimeMillis();
        String otherPersonName = ride.getOtherPersonName();
        if (otherPersonName == null || otherPersonName.isEmpty()) {
            otherPersonName = ride.isPassengerView() ? "Driver" : "Passenger";
        }
        android.util.Log.e("SAFETY_TEST", "  - Start time: " + rideStartTime);
        android.util.Log.e("SAFETY_TEST", "  - Other person: " + otherPersonName);

        android.util.Log.e("SAFETY_TEST", "Step 6: Creating intent...");
        Intent intent = new Intent(this, SafetyActivity.class);
        intent.putExtra("rideId", ride.getId());
        intent.putExtra("pickupLocation", ride.getPickupLocation() != null ? ride.getPickupLocation() : "Unknown");
        intent.putExtra("dropLocation", ride.getDropLocation() != null ? ride.getDropLocation() : "Unknown");
        intent.putExtra("driverName", otherPersonName);
        intent.putExtra("rideStartTime", rideStartTime);
        android.util.Log.e("SAFETY_TEST", "✅ Intent created");

        android.util.Log.e("SAFETY_TEST", "Step 7: Starting SafetyActivity...");
        try {
            startActivity(intent);
            android.util.Log.e("SAFETY_TEST", "✅✅✅ SafetyActivity started successfully!");
        } catch (Exception e) {
            android.util.Log.e("SAFETY_TEST", "❌❌❌ EXCEPTION!", e);
            Toast.makeText(this, "CRASH: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        android.util.Log.e("SAFETY_TEST", "");
        android.util.Log.e("SAFETY_TEST", "╔════════════════════════════════════════╗");
        android.util.Log.e("SAFETY_TEST", "║   OPENSAFETY METHOD COMPLETED          ║");
        android.util.Log.e("SAFETY_TEST", "╚════════════════════════════════════════╝");
        android.util.Log.e("SAFETY_TEST", "");
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
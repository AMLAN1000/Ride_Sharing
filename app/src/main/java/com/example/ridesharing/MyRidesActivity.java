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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
        loadPassengerRides();

        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
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
                completeRide(ride);
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

        // Load rides where current user is the passenger AND status is accepted
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("passengerId", currentUser.getUid())
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snapshots, error) -> {
                    hideLoading();

                    if (error != null) {
                        Log.e(TAG, "Error loading passenger rides", error);
                        showEmptyState("Error loading your rides");
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmptyState("No accepted rides yet.\nYour accepted requests will appear here.");
                        return;
                    }

                    ridesList.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        try {
                            MyRideItem ride = parseRideItem(document, true);
                            if (ride != null) {
                                ridesList.add(ride);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing ride", e);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (ridesList.isEmpty()) {
                        showEmptyState("No accepted rides");
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

        // Load rides where current user is the driver AND status is accepted
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("driverId", currentUser.getUid())
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snapshots, error) -> {
                    hideLoading();

                    if (error != null) {
                        Log.e(TAG, "Error loading driver rides", error);
                        showEmptyState("Error loading your rides");
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmptyState("No accepted rides yet.\nRides you accept will appear here.");
                        return;
                    }

                    ridesList.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        try {
                            MyRideItem ride = parseRideItem(document, false);
                            if (ride != null) {
                                ridesList.add(ride);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing ride", e);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (ridesList.isEmpty()) {
                        showEmptyState("No accepted rides");
                    } else {
                        hideEmptyState();
                    }
                });
    }

    private MyRideItem parseRideItem(QueryDocumentSnapshot document, boolean isPassenger) {
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
        new AlertDialog.Builder(this)
                .setTitle("Complete Ride?")
                .setMessage("Mark this ride as completed?")
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    db.collection("ride_requests")
                            .document(ride.getId())
                            .update("status", "completed", "completedAt", System.currentTimeMillis())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Ride completed!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to complete ride", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ridesListener != null) {
            ridesListener.remove();
        }
    }
}
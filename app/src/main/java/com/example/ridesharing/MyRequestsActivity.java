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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyRequestsActivity extends AppCompatActivity {

    private static final String TAG = "MyRequestsActivity";
    private RecyclerView recyclerView;
    private MyRequestsAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private View emptyStateLayout;
    private List<MyRideRequest> myRequests = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_requests);

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        loadMyRequests();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_my_requests);
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
        adapter = new MyRequestsAdapter(myRequests, new MyRequestsAdapter.OnMyRequestClickListener() {
            @Override
            public void onCallDriverClick(MyRideRequest request) {
                callDriver(request);
            }

            @Override
            public void onCancelRequestClick(MyRideRequest request) {
                showCancelConfirmation(request);
            }

            @Override
            public void onViewDetailsClick(MyRideRequest request) {
                showRequestDetails(request);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadMyRequests() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading();

        if (requestsListener != null) {
            requestsListener.remove();
        }

        requestsListener = db.collection("ride_requests")
                .whereEqualTo("passengerId", currentUser.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    hideLoading();

                    if (error != null) {
                        Log.e(TAG, "Error loading requests", error);
                        showEmptyState("Error loading your requests");
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showEmptyState("You haven't posted any ride requests yet");
                        return;
                    }

                    // Check for newly accepted requests
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            QueryDocumentSnapshot document = dc.getDocument();
                            String status = document.getString("status");
                            Boolean notifShown = document.getBoolean("notificationShown");

                            if ("accepted".equals(status) &&
                                    (notifShown == null || !notifShown)) {

                                MyRideRequest request = parseMyRideRequest(document);
                                if (request != null) {
                                    showAcceptanceDialog(request);
                                    document.getReference().update("notificationShown", true);
                                }
                            }
                        }
                    }

                    myRequests.clear();
                    for (QueryDocumentSnapshot document : snapshots) {
                        try {
                            MyRideRequest request = parseMyRideRequest(document);
                            if (request != null) {
                                myRequests.add(request);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing request", e);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (myRequests.isEmpty()) {
                        showEmptyState("No active requests");
                    } else {
                        hideEmptyState();
                    }
                });
    }

    private MyRideRequest parseMyRideRequest(QueryDocumentSnapshot document) {
        String id = document.getId();
        String status = document.getString("status");
        String pickupLocation = document.getString("pickupLocation");
        String dropLocation = document.getString("dropLocation");
        Double fare = document.getDouble("fare");
        String vehicleType = document.getString("vehicleType");
        Long passengers = document.getLong("passengers");
        Long departureTime = document.getLong("departureTime");
        Long createdAt = document.getLong("createdAt");
        String driverId = document.getString("driverId");
        String driverName = document.getString("driverName");
        String driverPhone = document.getString("driverPhone");
        Long acceptedAt = document.getLong("acceptedAt");
        Boolean notificationShown = document.getBoolean("notificationShown");

        return new MyRideRequest(
                id, status, pickupLocation, dropLocation,
                fare != null ? fare : 0.0,
                vehicleType, passengers != null ? passengers.intValue() : 1,
                departureTime, createdAt,
                driverId, driverName, driverPhone, acceptedAt,
                notificationShown != null ? notificationShown : false
        );
    }

    private void showAcceptanceDialog(MyRideRequest request) {
        String message = "🎉 Great news! " + request.getDriverName() +
                " has accepted your ride request!\n\n" +
                "📍 From: " + request.getPickupLocation() + "\n" +
                "📍 To: " + request.getDropLocation() + "\n" +
                "💰 Fare: ৳" + String.format(Locale.getDefault(), "%.0f", request.getFare());

        if (request.getDriverPhone() != null && !request.getDriverPhone().isEmpty()) {
            message += "\n\n📞 Driver Phone: " + request.getDriverPhone();
        }

        new AlertDialog.Builder(this)
                .setTitle("✅ Request Accepted!")
                .setMessage(message)
                .setPositiveButton("Call Driver", (dialog, which) -> callDriver(request))
                .setNegativeButton("OK", null)
                .setCancelable(false)
                .show();

        playNotificationSound();
    }

    private void playNotificationSound() {
        try {
            android.media.RingtoneManager.getRingtone(
                    this,
                    android.media.RingtoneManager.getDefaultUri(
                            android.media.RingtoneManager.TYPE_NOTIFICATION
                    )
            ).play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }

    private void callDriver(MyRideRequest request) {
        if (request.getDriverPhone() != null && !request.getDriverPhone().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + request.getDriverPhone()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Driver phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCancelConfirmation(MyRideRequest request) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Request?")
                .setMessage("Are you sure you want to cancel this ride request?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelRequest(request))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelRequest(MyRideRequest request) {
        db.collection("ride_requests")
                .document(request.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showRequestDetails(MyRideRequest request) {
        String details = "Status: " + request.getStatus().toUpperCase() + "\n\n" +
                "From: " + request.getPickupLocation() + "\n" +
                "To: " + request.getDropLocation() + "\n" +
                "Fare: ৳" + String.format(Locale.getDefault(), "%.0f", request.getFare()) + "\n" +
                "Vehicle: " + (request.getVehicleType() != null ?
                request.getVehicleType().toUpperCase() : "CAR") + "\n" +
                "Passengers: " + request.getPassengers();

        if ("accepted".equals(request.getStatus())) {
            details += "\n\n--- Driver Info ---\n" +
                    "Name: " + request.getDriverName();
            if (request.getDriverPhone() != null) {
                details += "\nPhone: " + request.getDriverPhone();
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Request Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
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
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}
package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;



public class AvailableRidesActivity extends AppCompatActivity implements RideAdapter.OnRideClickListener {

    private RecyclerView ridesRecyclerView;
    private RideAdapter rideAdapter;
    private LinearLayout emptyStateLayout;
    private TextView ridesCountText;
    private List<Ride> rideList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_rides);

        initializeViews();
        setupRecyclerView();
        setupBackPressedHandler();
        loadSampleRides();

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    private void initializeViews() {
        ridesRecyclerView = findViewById(R.id.rides_recyclerview);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        ridesCountText = findViewById(R.id.rides_count_text);

        // Setup filter button
        ImageView filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ridesRecyclerView.setLayoutManager(layoutManager);
        ridesRecyclerView.setHasFixedSize(true);

        // Create and set adapter
        rideAdapter = new RideAdapter(rideList, this);
        ridesRecyclerView.setAdapter(rideAdapter);
    }

    private void setupBackPressedHandler() {
        // Handle back button press with modern approach
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Close this activity and return to MainActivity
            }
        });
    }

    private void loadSampleRides() {
        showLoadingState();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<Ride> sampleRides = createSampleRides();
            rideList.clear();
            rideList.addAll(sampleRides);
            rideAdapter.updateRides(sampleRides);
            updateUIState();
        }, 1000);
    }

    private List<Ride> createSampleRides() {
        List<Ride> rides = new ArrayList<>();

        // Add sample rides - make sure Ride class exists in your project
        rides.add(new Ride("1", "John Doe", "Toyota Corolla", 3, 4.8,
                "EWU Main Campus, Aftabnagar", "Bashundhara City Mall",
                "2:30 PM", "3:15 PM", 120.0, "driver1"));

        rides.add(new Ride("2", "Sarah Ahmed", "Honda Civic", 2, 4.9,
                "EWU Permanent Campus", "Jamuna Future Park",
                "4:00 PM", "4:45 PM", 150.0, "driver2"));

        rides.add(new Ride("3", "Mike Rahman", "Yamaha Bike", 1, 4.7,
                "Rampura Bridge", "EWU Main Campus",
                "5:30 PM", "5:50 PM", 80.0, "driver3"));

        rides.add(new Ride("4", "Lisa Chowdhury", "Mitsubishi Pajero", 4, 4.6,
                "EWU Main Campus", "Airport Area",
                "6:00 PM", "6:40 PM", 200.0, "driver4"));

        return rides;
    }

    private void updateUIState() {
        if (rideList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            ridesRecyclerView.setVisibility(View.GONE);
            ridesCountText.setText("No rides available");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            ridesRecyclerView.setVisibility(View.VISIBLE);
            ridesCountText.setText(rideList.size() + " rides available near you");
        }
    }

    private void showLoadingState() {
        emptyStateLayout.setVisibility(View.GONE);
        ridesRecyclerView.setVisibility(View.VISIBLE);
        ridesCountText.setText("Loading rides...");
    }

    private void showFilterDialog() {
        android.widget.Toast.makeText(this, "Filter feature coming soon!", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRideRequestClick(Ride ride) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Request Ride")
                .setMessage("Request ride with " + ride.getDriverName() + " for ৳" + ride.getFare() + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    android.widget.Toast.makeText(this, "Ride request sent to " + ride.getDriverName(), android.widget.Toast.LENGTH_SHORT).show();
                    // TODO: Implement Firebase request logic
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onProfileViewClick(Ride ride) {
        android.widget.Toast.makeText(this, "Viewing " + ride.getDriverName() + "'s profile", android.widget.Toast.LENGTH_SHORT).show();
        // TODO: Navigate to ProfileActivity
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }
}
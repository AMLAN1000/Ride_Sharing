package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Main content buttons
    private View btnAvailableRequests, btnAvailableRides, btnPostRide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();

        // Setup bottom navigation using the helper class with HOME selected
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.HOME);
    }

    private void initializeViews() {
        // Main content buttons
        btnAvailableRequests = findViewById(R.id.btn_available_requests);
        btnAvailableRides = findViewById(R.id.btn_available_rides);
        btnPostRide = findViewById(R.id.btn_post_ride);
    }

    private void setupClickListeners() {
        // Main Content Click Listeners
        btnAvailableRequests.setOnClickListener(v -> {
            // For now, show toast since AvailableRequestsActivity might not exist yet
            Toast.makeText(this, "Available Requests clicked", Toast.LENGTH_SHORT).show();
            // TODO: Uncomment when AvailableRequestsActivity is created
            // startActivity(new Intent(this, AvailableRequestsActivity.class));
        });

        btnAvailableRides.setOnClickListener(v -> {
            // For now, show toast since AvailableRidesActivity might not exist yet
            Toast.makeText(this, "Available Rides clicked", Toast.LENGTH_SHORT).show();
            // TODO: Uncomment when AvailableRidesActivity is created
            // startActivity(new Intent(this, AvailableRidesActivity.class));
        });

        btnPostRide.setOnClickListener(v -> {
            // Navigate to Post Ride Activity
            startActivity(new Intent(this, PostActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-setup bottom navigation when returning to MainActivity
        // This ensures HOME is selected when user returns from other activities
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.HOME);
    }
}
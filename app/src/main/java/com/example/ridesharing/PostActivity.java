package com.example.ridesharing;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.ridesharing.BottomNavigationHelper;

public class PostActivity extends AppCompatActivity {

    private CardView btnOfferRide;
    private CardView btnRequestRide;
    private View btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Setup bottom navigation with POST tab selected
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.POST);

        // Initialize views
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        btnOfferRide = findViewById(R.id.btn_offer_ride);
        btnRequestRide = findViewById(R.id.btn_request_ride);
        btnClose = findViewById(R.id.btn_close);
    }

    private void setupClickListeners() {
        // Offer Ride button click listener
        btnOfferRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle offer ride action
                Toast.makeText(PostActivity.this, "Offer Ride Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to OfferRideActivity or show offer ride dialog
                navigateToOfferRide();
            }
        });

        // Request Ride button click listener
        btnRequestRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle request ride action
                Toast.makeText(PostActivity.this, "Request Ride Clicked", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to RequestRideActivity or show request ride dialog
                navigateToRequestRide();
            }
        });

        // Close button click listener
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close the activity
                finish();
            }
        });
    }

    private void navigateToOfferRide() {
        // TODO: Implement navigation to Offer Ride screen
        // Example:
        // Intent intent = new Intent(PostActivity.this, OfferRideActivity.class);
        // startActivity(intent);
    }

    private void navigateToRequestRide() {
        // TODO: Implement navigation to Request Ride screen
        // Example:
        // Intent intent = new Intent(PostActivity.this, RequestRideActivity.class);
        // startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure POST tab stays selected when returning to this activity
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.POST);
    }
}
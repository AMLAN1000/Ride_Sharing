package com.example.ridesharing;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage, btnUploadDp, btnEdit;
    private TextView profileName, profileEmail, profileNid, profilePhone, profileJoinDate;
    private TextView totalRides, userRating;
    private View btnEditProfile, btnSettings, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Setup bottom navigation here
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.PROFILE);

        initializeViews();
        setupClickListeners();

        // Load user data here
        loadUserData();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        btnUploadDp = findViewById(R.id.btn_upload_dp);
        btnEdit = findViewById(R.id.btn_edit);

        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        profileNid = findViewById(R.id.profile_nid);
        profilePhone = findViewById(R.id.profile_phone);
        profileJoinDate = findViewById(R.id.profile_join_date);

        totalRides = findViewById(R.id.total_rides);
        userRating = findViewById(R.id.user_rating);

        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnSettings = findViewById(R.id.btn_settings);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        btnUploadDp.setOnClickListener(v -> {
            // Handle profile picture upload here
            // Open image picker or camera
        });

        btnEdit.setOnClickListener(v -> {
            // Handle edit profile action here
            // Open edit profile screen
        });

        btnEditProfile.setOnClickListener(v -> {
            // Navigate to edit profile screen here
        });

        btnSettings.setOnClickListener(v -> {
            // Navigate to settings screen here
        });

        btnLogout.setOnClickListener(v -> {
            // Handle logout here
            // Clear session and go to login screen
        });
    }

    private void loadUserData() {
        // Load user data from SharedPreferences or database here

        // Example:
        // profileName.setText(currentUser.getName());
        // profileEmail.setText(currentUser.getEmail());
        // profileNid.setText(currentUser.getNid());
        // profilePhone.setText(currentUser.getPhone());

        // Load stats from database here
        // totalRides.setText(String.valueOf(getTotalRidesFromDB()));
        // userRating.setText(String.valueOf(getUserRatingFromDB()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh bottom navigation here
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.PROFILE);

        // Reload user data if needed
        loadUserData();
    }
}
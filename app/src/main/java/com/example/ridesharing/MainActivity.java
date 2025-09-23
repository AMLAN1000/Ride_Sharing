package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    // Main content buttons
    private View btnAvailableRequests, btnAvailableRides, btnPostRide;
    private TextView usernameText;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserData();

        // Setup bottom navigation using the helper class with HOME selected
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.HOME);
    }

    private void initializeViews() {
        // Main content buttons
        btnAvailableRequests = findViewById(R.id.btn_available_requests);
        btnAvailableRides = findViewById(R.id.btn_available_rides);
        btnPostRide = findViewById(R.id.btn_post_ride);

        // Username text view
        usernameText = findViewById(R.id.username_text);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Reference to the user document in Firestore
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Get the fullName field from Firestore (matches your User class)
                        String fullName = document.getString("fullName");

                        if (fullName != null && !fullName.isEmpty()) {
                            usernameText.setText(fullName);
                        } else {
                            // Fallback to email if fullName is empty
                            String email = currentUser.getEmail();
                            usernameText.setText(email != null ? email : "User");
                        }
                    } else {
                        // Document doesn't exist, use email as fallback
                        String email = currentUser.getEmail();
                        usernameText.setText(email != null ? email : "User");
                    }
                } else {
                    // Handle errors - use email as fallback
                    String email = currentUser.getEmail();
                    usernameText.setText(email != null ? email : "User");
                    Toast.makeText(MainActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // User not logged in
            usernameText.setText("Guest");
        }
    }

    private void setupClickListeners() {
        // Main Content Click Listeners
        btnAvailableRequests.setOnClickListener(v -> {
            Toast.makeText(this, "Available Requests clicked", Toast.LENGTH_SHORT).show();
        });

        btnAvailableRides.setOnClickListener(v -> {
            Toast.makeText(this, "Available Rides clicked", Toast.LENGTH_SHORT).show();
        });

        btnPostRide.setOnClickListener(v -> {
            startActivity(new Intent(this, PostActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-setup bottom navigation when returning to MainActivity
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.HOME);

        // Optionally reload user data in case it was updated
        loadUserData();
    }
}
package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private View btnAvailableRequests, btnAvailableRides, btnPostRide;
    private TextView usernameText;
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

        testBottomNavigationManually();

        // Use string constant
        BottomNavigationHelper.setupBottomNavigation(this, "HOME");


        // TEMPORARY DEBUG CODE - Add this in MainActivity.onCreate()


    }


    private void initializeViews() {
        // Fix: Use the correct IDs from your XML layout
        btnAvailableRequests = findViewById(R.id.btn_available_requests); // This is the clickable view inside the card
        btnAvailableRides = findViewById(R.id.btn_available_rides); // This is the clickable view inside the card
        btnPostRide = findViewById(R.id.btn_post_ride); // This is the clickable view inside the card
        usernameText = findViewById(R.id.username_text);
    }

    private void setupClickListeners() {
        // Fix: Available Requests should open AvailableRequestsActivity
        btnAvailableRequests.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AvailableRequestsActivity.class);
            startActivity(intent);
        });

        btnAvailableRides.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AvailableRidesActivity.class);
            startActivity(intent);
        });

        btnPostRide.setOnClickListener(v -> {
            startActivity(new Intent(this, PostActivity.class));
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String fullName = document.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            usernameText.setText(fullName);
                        } else {
                            String email = currentUser.getEmail();
                            usernameText.setText(email != null ? email : "User");
                        }
                    } else {
                        String email = currentUser.getEmail();
                        usernameText.setText(email != null ? email : "User");
                    }
                } else {
                    String email = currentUser.getEmail();
                    usernameText.setText(email != null ? email : "User");
                }
            });
        } else {
            usernameText.setText("Guest");
        }
    }



    private void testBottomNavigationManually() {
        Log.d("TEST", "=== MANUAL BOTTOM NAV TEST ===");

        // Test each button manually
        testButton(R.id.nav_home, "HOME");
        testButton(R.id.nav_post, "POST");
        testButton(R.id.nav_rides, "RIDES");
        testButton(R.id.nav_profile, "PROFILE");
    }

    private void testButton(int buttonId, String buttonName) {
        LinearLayout button = findViewById(buttonId);
        if (button != null) {
            Log.d("TEST", "✅ " + buttonName + " BUTTON FOUND!");

            button.setOnClickListener(v -> {
                Log.d("TEST", "🎯 " + buttonName + " BUTTON CLICKED!");
                Toast.makeText(this, buttonName + " clicked!", Toast.LENGTH_SHORT).show();

                if (buttonName.equals("POST")) {
                    try {
                        Intent intent = new Intent(this, PostActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e("TEST", "💥 Cannot open PostActivity: " + e.getMessage());
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Log.e("TEST", "❌ " + buttonName + " BUTTON NOT FOUND!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
        loadUserData();
    }
}
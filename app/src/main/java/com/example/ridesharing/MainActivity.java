package com.example.ridesharing;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private View btnAvailableRequests, btnAvailableRides, btnPostRide, btnAvailableCarpools;
    private TextView usernameText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "🚀 MainActivity started");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔥 INITIALIZE NOTIFICATION SYSTEM
        initializeNotificationSystem();

        initializeViews();
        setupClickListeners();
        loadUserData();
        ExpiredRequestsCleaner.cleanExpiredPendingRequests();

        testBottomNavigationManually();

        // Use string constant
        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
    }

    // 🔥 NOTIFICATION SYSTEM INITIALIZATION
    private void initializeNotificationSystem() {
        // 1. Request notification permission (Android 13+)
        requestNotificationPermission();

        // 2. Create notification channel
        RideNotificationManager.createNotificationChannelStatic(this);

        // 3. Initialize ride status monitoring
        RideStatusMonitor.initialize(this);

        Log.d(TAG, "✅ Notification system initialized");
    }

    // Request notification permission for Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "📢 Requesting notification permission...");

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            } else {
                Log.d(TAG, "✅ Notification permission already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Notification permission granted by user");
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "❌ Notification permission denied by user");
                Toast.makeText(this, "Notifications may not work properly", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeViews() {
        // Fix: Use the correct IDs from your XML layout
        btnAvailableRequests = findViewById(R.id.btn_available_requests);
        btnAvailableRides = findViewById(R.id.btn_available_rides);
        btnPostRide = findViewById(R.id.btn_post_ride);
        btnAvailableCarpools = findViewById(R.id.btn_available_carpools); // FIXED: Use class member
        usernameText = findViewById(R.id.username_text);
    }

    private void setupClickListeners() {
        // Fix: Available Requests should open AvailableRequestsActivity
        if (btnAvailableRequests != null) {
            btnAvailableRequests.setOnClickListener(v -> {
                Log.d(TAG, "📋 Opening Available Requests");
                Intent intent = new Intent(MainActivity.this, AvailableRequestsActivity.class);
                startActivity(intent);
            });
        }

        if (btnAvailableRides != null) {
            btnAvailableRides.setOnClickListener(v -> {
                Log.d(TAG, "🚗 Opening Available Rides");
                Intent intent = new Intent(MainActivity.this, AvailableRidesActivity.class);
                startActivity(intent);
            });
        }

        if (btnPostRide != null) {
            btnPostRide.setOnClickListener(v -> {
                Log.d(TAG, "➕ Opening Post Activity");
                startActivity(new Intent(this, PostActivity.class));
            });
        }

        if (btnAvailableCarpools != null) {
            btnAvailableCarpools.setOnClickListener(v -> {
                Log.d(TAG, "👥 Opening Available Carpools");
                startActivity(new Intent(this, AvailableCarpoolsActivity.class));
            });
        }
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
                            Log.d(TAG, "👤 User loaded: " + fullName);
                        } else {
                            String email = currentUser.getEmail();
                            usernameText.setText(email != null ? email : "User");
                            Log.d(TAG, "👤 Using email as username: " + email);
                        }
                    } else {
                        String email = currentUser.getEmail();
                        usernameText.setText(email != null ? email : "User");
                        Log.w(TAG, "❌ User document not found, using email");
                    }
                } else {
                    String email = currentUser.getEmail();
                    usernameText.setText(email != null ? email : "User");
                    Log.e(TAG, "❌ Error loading user data: " + task.getException());
                }
            });
        } else {
            usernameText.setText("Guest");
            Log.d(TAG, "👤 User not logged in, showing Guest");

            // Redirect to login if not authenticated
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void testBottomNavigationManually() {
        Log.d(TAG, "=== TESTING BOTTOM NAVIGATION ===");

        // Test each button manually
        testButton(R.id.nav_home, "HOME");
        testButton(R.id.nav_post, "POST");
        testButton(R.id.nav_rides, "RIDES");
        testButton(R.id.nav_profile, "PROFILE");
    }

    private void testButton(int buttonId, String buttonName) {
        LinearLayout button = findViewById(buttonId);
        if (button != null) {
            Log.d(TAG, "✅ " + buttonName + " BUTTON FOUND!");

            button.setOnClickListener(v -> {
                Log.d(TAG, "🎯 " + buttonName + " BUTTON CLICKED!");
                Toast.makeText(this, buttonName + " clicked!", Toast.LENGTH_SHORT).show();

                if (buttonName.equals("POST")) {
                    try {
                        Intent intent = new Intent(this, PostActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "💥 Cannot open PostActivity: " + e.getMessage());
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Log.e(TAG, "❌ " + buttonName + " BUTTON NOT FOUND!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 MainActivity resumed");

        // Ensure notification system is ready
        RideNotificationManager.createNotificationChannelStatic(this);

        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
        loadUserData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "⏸️ MainActivity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 MainActivity destroyed");
    }

    // Handle back button press
    @Override
    public void onBackPressed() {
        // Ask user to confirm exit
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    finishAffinity(); // Close all activities
                    System.exit(0);
                })
                .setNegativeButton("No", null)
                .show();
    }
}
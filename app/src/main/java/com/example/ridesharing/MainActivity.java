package com.example.ridesharing;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;
import android.animation.ObjectAnimator;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.card.MaterialCardView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    // Card views from XML layout
    private MaterialCardView cardAvailableRequests, cardAvailableRides, cardAvailableCarpools;
    private View btnAvailableRequests, btnAvailableRides, btnAvailableCarpools;
    private TextView usernameText, greetingText;
    private TextView ridesCountText, carpoolsCountText;
    private LottieAnimationView carAnimation;

    // Lottie animations for cards
    private LottieAnimationView rideRequestAnimation, ridesAnimation, carpoolsAnimation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Real-time listeners
    private ListenerRegistration requestsListener;
    private ListenerRegistration ridesListener;
    private ListenerRegistration carpoolsListener;

    // Back press handling
    private long backPressedTime = 0;
    private Toast backToast;
    private OnBackPressedCallback onBackPressedCallback;

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
        setupAnimations();
        loadUserData();

        // 🔥 SETUP MODERN BACK PRESS HANDLER
        setupBackPressHandler();

        // 🔥 SETUP REAL-TIME DATA LISTENERS
        setupRealTimeDataListeners();

        ExpiredRequestsCleaner.cleanExpiredPendingRequests();

        // Setup scroll shrink effect
        setupScrollListener();

        // Use string constant
        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
    }

    private void setupBackPressHandler() {
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void handleBackPress() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            // Double tap within 2 seconds - exit
            if (backToast != null) {
                backToast.cancel();
            }
            finishAffinity();
            System.exit(0);
        } else {
            // First back press - show toast
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
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
        // Get references to all views from XML
        cardAvailableRequests = findViewById(R.id.available_requests_card);
        cardAvailableRides = findViewById(R.id.available_rides_card);
        cardAvailableCarpools = findViewById(R.id.available_carpools_card);

        // Get clickable views
        btnAvailableRequests = findViewById(R.id.btn_available_requests);
        btnAvailableRides = findViewById(R.id.btn_available_rides);
        btnAvailableCarpools = findViewById(R.id.btn_available_carpools);

        // Text views
        usernameText = findViewById(R.id.username_text);
        greetingText = findViewById(R.id.greeting_text);

        // Real-time count text views
        ridesCountText = findViewById(R.id.rides_count_text);
        carpoolsCountText = findViewById(R.id.carpools_count_text);

        // Car animation
        carAnimation = findViewById(R.id.car_animation);

        // Lottie animations for cards
        rideRequestAnimation = findViewById(R.id.riderequest);
        ridesAnimation = findViewById(R.id.rides_animation);
        carpoolsAnimation = findViewById(R.id.carpools_animation);


        Log.d(TAG, "✅ All views initialized");
    }

    // 🔥 REAL-TIME DATA LISTENERS
    private void setupRealTimeDataListeners() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // 1. Load AVAILABLE REQUESTS count
        requestsListener = db.collection("rideRequests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening to requests: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        int count = querySnapshot.size();
                        updateRequestsAnimation(count);
                        Log.d(TAG, "📋 Real-time requests count: " + count);
                    }
                });

        // 2. Load AVAILABLE RIDES count
        ridesListener = db.collection("rides")
                .whereEqualTo("status", "available")
                .whereGreaterThan("availableSeats", 0)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening to rides: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        int count = querySnapshot.size();
                        updateRidesCount(count);
                        Log.d(TAG, "🚗 Real-time rides count: " + count);
                    }
                });

        // 3. Load CARPOOLS count
        carpoolsListener = db.collection("rides")
                .whereEqualTo("isCarpool", true)
                .whereEqualTo("status", "available")
                .whereGreaterThan("availableSeats", 1)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening to carpools: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        int count = querySnapshot.size();
                        updateCarpoolsCount(count);
                        Log.d(TAG, "👥 Real-time carpools count: " + count);
                    }
                });
    }

    private void updateRequestsAnimation(int count) {
        runOnUiThread(() -> {
            // Update ride request animation based on count
            if (rideRequestAnimation != null) {
                if (count > 0) {
                    // Speed up animation when there are requests
                    rideRequestAnimation.setSpeed(1.5f);

                    // Visual feedback for new requests
                    ObjectAnimator pulseAnim = ObjectAnimator.ofFloat(cardAvailableRequests, "alpha", 0.9f, 1f);
                    pulseAnim.setDuration(500);
                    pulseAnim.setRepeatCount(1);
                    pulseAnim.setRepeatMode(ObjectAnimator.REVERSE);
                    pulseAnim.start();
                } else {
                    // Slow down animation when no requests
                    rideRequestAnimation.setSpeed(0.7f);
                }
            }
        });
    }

    private void updateRidesCount(int count) {
        runOnUiThread(() -> {
            if (ridesCountText != null) {
                String countText = count > 0 ? count + " Active" : "None available";
                ridesCountText.setText(countText);

                // Update rides animation based on count
                if (ridesAnimation != null) {
                    if (count > 0) {
                        ridesAnimation.setSpeed(1.2f);
                    } else {
                        ridesAnimation.setSpeed(0.8f);
                    }
                }
            }
        });
    }

    private void updateCarpoolsCount(int count) {
        runOnUiThread(() -> {
            if (carpoolsCountText != null) {
                String countText = count > 0 ? count + " Shared" : "None available";
                carpoolsCountText.setText(countText);

                // Update carpools animation based on count
                if (carpoolsAnimation != null) {
                    if (count > 0) {
                        carpoolsAnimation.setSpeed(1.1f);
                    } else {
                        carpoolsAnimation.setSpeed(0.9f);
                    }
                }
            }
        });
    }

    private void removeRealTimeListeners() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }
        if (carpoolsListener != null) {
            carpoolsListener.remove();
            carpoolsListener = null;
        }
    }

    private void setupAnimations() {
        // Apply Apple-like press animations to cards
        applyCardAnimation(cardAvailableRequests);
        applyCardAnimation(cardAvailableRides);
        applyCardAnimation(cardAvailableCarpools);

        // Set up car animation in header
        if (carAnimation != null) {
            carAnimation.setSpeed(0.9f); // Speed as per XML
            // Scale is set in XML with app:lottie_scale="2"
        }

        Log.d(TAG, "✅ Animations set up");
    }

    private void applyCardAnimation(View card) {
        card.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(v, "scaleX", 0.97f);
                        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(v, "scaleY", 0.97f);
                        scaleDownX.setDuration(120);
                        scaleDownY.setDuration(120);
                        scaleDownX.start();
                        scaleDownY.start();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            v.setElevation(12f); // Increase elevation on press (from 8dp to 12dp)
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(v, "scaleX", 1f);
                        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(v, "scaleY", 1f);
                        scaleUpX.setDuration(200);
                        scaleUpY.setDuration(200);
                        scaleUpX.start();
                        scaleUpY.start();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            v.setElevation(8f); // Return to normal elevation
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void setupClickListeners() {
        // Available Requests Card
        if (btnAvailableRequests != null) {
            btnAvailableRequests.setOnClickListener(v -> {
                Log.d(TAG, "📋 Opening Available Requests");
                animateClick(cardAvailableRequests, () -> {
                    Intent intent = new Intent(MainActivity.this, AvailableRequestsActivity.class);
                    startActivity(intent);
                });
            });
        }

        // Available Rides Card
        if (btnAvailableRides != null) {
            btnAvailableRides.setOnClickListener(v -> {
                Log.d(TAG, "🚗 Opening Available Rides");
                animateClick(cardAvailableRides, () -> {
                    Intent intent = new Intent(MainActivity.this, AvailableRidesActivity.class);
                    startActivity(intent);
                });
            });
        }

        // Available Carpools Card
        if (btnAvailableCarpools != null) {
            btnAvailableCarpools.setOnClickListener(v -> {
                Log.d(TAG, "👥 Opening Available Carpools");
                animateClick(cardAvailableCarpools, () -> {
                    Intent intent = new Intent(MainActivity.this, AvailableCarpoolsActivity.class);
                    startActivity(intent);
                });
            });
        }

        Log.d(TAG, "✅ All click listeners set up");
    }

    private void animateClick(View view, Runnable action) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(50)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .withEndAction(action)
                            .start();
                })
                .start();
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
                            // Get only first name
                            String firstName = fullName.split(" ")[0];
                            usernameText.setText(firstName);

                            // Update greeting
                            updateGreeting();

                            Log.d(TAG, "👤 User loaded: " + firstName);
                        } else {
                            usernameText.setText("User");
                            updateGreeting();
                            Log.d(TAG, "👤 Using default username");
                        }
                    } else {
                        usernameText.setText("User");
                        updateGreeting();
                        Log.w(TAG, "❌ User document not found");
                    }
                } else {
                    usernameText.setText("User");
                    updateGreeting();
                    Log.e(TAG, "❌ Error loading user data: " + task.getException());
                }
            });
        } else {
            usernameText.setText("Guest");
            updateGreeting();
            Log.d(TAG, "👤 User not logged in, showing Guest");

            // Redirect to login if not authenticated
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void updateGreeting() {
        String greeting = "Hello";

        if (greetingText != null) {
            greetingText.setText(greeting);
        }
    }

    // Scroll listener for header shrink effect
    private void setupScrollListener() {
        NestedScrollView scrollView = findViewById(R.id.main_scroll_view);
        CardView headerCard = findViewById(R.id.header_card);

        if (scrollView == null || headerCard == null) {
            Log.e(TAG, "❌ Scroll view or header not found");
            return;
        }

        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            private final int initialHeaderHeight = 200; // dp - matches XML header height

            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // Calculate shrink factor (0 to 1)
                float shrinkFactor = Math.min(1.0f, scrollY / 100f);

                // Calculate new header height
                int newHeight = (int) (initialHeaderHeight - (shrinkFactor * 40)); // Shrink by max 40dp

                // Update header card height
                ViewGroup.LayoutParams params = headerCard.getLayoutParams();
                params.height = dpToPx(newHeight);
                headerCard.setLayoutParams(params);

                // Adjust car animation scale based on scroll
                if (carAnimation != null) {
                    float carScale = 2.0f - (shrinkFactor * 0.5f); // Shrink car from 2.0 to 1.5
                    carAnimation.setScaleX(carScale);
                    carAnimation.setScaleY(carScale);
                }
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 MainActivity resumed");

        // Ensure notification system is ready
        RideNotificationManager.createNotificationChannelStatic(this);

        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
        loadUserData();

        // 🔥 RESTART REAL-TIME LISTENERS
        setupRealTimeDataListeners();

        // Restart animations
        if (carAnimation != null) {
            carAnimation.playAnimation();
        }

        // Restart card animations
        if (rideRequestAnimation != null) {
            rideRequestAnimation.playAnimation();
        }
        if (ridesAnimation != null) {
            ridesAnimation.playAnimation();
        }
        if (carpoolsAnimation != null) {
            carpoolsAnimation.playAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "⏸️ MainActivity paused");

        // 🔥 REMOVE REAL-TIME LISTENERS TO SAVE RESOURCES
        removeRealTimeListeners();

        // Pause animations
        if (carAnimation != null) {
            carAnimation.pauseAnimation();
        }

        if (rideRequestAnimation != null) {
            rideRequestAnimation.pauseAnimation();
        }
        if (ridesAnimation != null) {
            ridesAnimation.pauseAnimation();
        }
        if (carpoolsAnimation != null) {
            carpoolsAnimation.pauseAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 MainActivity destroyed");

        // 🔥 CLEAN UP LISTENERS
        removeRealTimeListeners();

        // Remove back press callback
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
        }

        // Cancel any active toast
        if (backToast != null) {
            backToast.cancel();
        }
    }
}
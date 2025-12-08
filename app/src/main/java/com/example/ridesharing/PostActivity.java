package com.example.ridesharing;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostActivity extends AppCompatActivity {

    private static final String TAG = "PostActivity";

    // Card views
    private MaterialCardView postRequestCard, postRideCard, postCarpoolCard;
    private View btnPostRequest, btnPostRide, btnPostCarpool;
    private TextView usernameText;

    // Animations
    private LottieAnimationView carAnimation;
    private LottieAnimationView requestAnimation, rideAnimation, carpoolAnimation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        Log.d(TAG, "🚀 PostActivity started");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        setupAnimations();
        loadUserData();
        setupScrollListener();

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, "POST");
    }

    private void initializeViews() {
        // Card views
        postRequestCard = findViewById(R.id.post_request_card);
        postRideCard = findViewById(R.id.post_ride_card);
        postCarpoolCard = findViewById(R.id.post_carpool_card);

        // Clickable areas
        btnPostRequest = findViewById(R.id.btn_post_request);
        btnPostRide = findViewById(R.id.btn_post_ride);
        btnPostCarpool = findViewById(R.id.btn_post_carpool);

        // Text views
        usernameText = findViewById(R.id.username_text);

        // Animations
        carAnimation = findViewById(R.id.car_animation);
        requestAnimation = findViewById(R.id.request_animation);
        rideAnimation = findViewById(R.id.ride_animation);
        carpoolAnimation = findViewById(R.id.carpool_animation);

        Log.d(TAG, "✅ All views initialized");
    }

    private void setupAnimations() {
        // Apply Apple-like press animations to cards
        applyCardAnimation(postRequestCard);
        applyCardAnimation(postRideCard);
        applyCardAnimation(postCarpoolCard);

        // Set up car animation in header
        if (carAnimation != null) {
            carAnimation.setSpeed(0.9f);
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
                            v.setElevation(12f);
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
                            v.setElevation(8f);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void setupClickListeners() {
        // Post Request Card
        if (btnPostRequest != null) {
            btnPostRequest.setOnClickListener(v -> {
                Log.d(TAG, "📋 Opening Post Request");
                animateClick(postRequestCard, () -> {
                    Intent intent = new Intent(PostActivity.this, PostRequestActivity.class);
                    startActivity(intent);
                });
            });
        }

        // Post Ride Card
        if (btnPostRide != null) {
            btnPostRide.setOnClickListener(v -> {
                Log.d(TAG, "🚗 Opening Post Ride");
                animateClick(postRideCard, () -> {
                    Intent intent = new Intent(PostActivity.this, PostRideActivity.class);
                    startActivity(intent);
                });
            });
        }

        // Post Carpool Card
        if (btnPostCarpool != null) {
            btnPostCarpool.setOnClickListener(v -> {
                Log.d(TAG, "👥 Opening Post Carpool");
                animateClick(postCarpoolCard, () -> {
                    Intent intent = new Intent(PostActivity.this, CarpoolPostActivity.class);
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
                            Log.d(TAG, "👤 User loaded: " + firstName);
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
        Log.d(TAG, "🔄 PostActivity resumed");

        BottomNavigationHelper.setupBottomNavigation(this, "POST");
        loadUserData();

        // Restart animations
        if (carAnimation != null) {
            carAnimation.playAnimation();
        }
        if (requestAnimation != null) {
            requestAnimation.playAnimation();
        }
        if (rideAnimation != null) {
            rideAnimation.playAnimation();
        }
        if (carpoolAnimation != null) {
            carpoolAnimation.playAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "⏸️ PostActivity paused");

        // Pause animations
        if (carAnimation != null) {
            carAnimation.pauseAnimation();
        }
        if (requestAnimation != null) {
            requestAnimation.pauseAnimation();
        }
        if (rideAnimation != null) {
            rideAnimation.pauseAnimation();
        }
        if (carpoolAnimation != null) {
            carpoolAnimation.pauseAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 PostActivity destroyed");
    }
}
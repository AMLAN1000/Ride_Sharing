package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostActivity extends AppCompatActivity {

    private View btnPostRequest, btnPostRide;
    private TextView usernameText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserData();

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, "POST");
    }

    private void initializeViews() {
        btnPostRequest = findViewById(R.id.btn_post_request);
        btnPostRide = findViewById(R.id.btn_post_ride);
        usernameText = findViewById(R.id.username_text);
    }

    private void setupClickListeners() {
        btnPostRequest.setOnClickListener(v -> {
            // Navigate to PostRequestActivity
            Intent intent = new Intent(PostActivity.this, PostRequestActivity.class);
            startActivity(intent);
        });

        btnPostRide.setOnClickListener(v -> {
            Intent intent = new Intent(PostActivity.this, PostRideActivity.class);
            startActivity(intent);
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

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "POST");
        loadUserData();
    }
}
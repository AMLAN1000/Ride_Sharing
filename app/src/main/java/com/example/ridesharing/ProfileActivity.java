package com.example.ridesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName, profileEmail, profilePhone, profileJoinDate;
    private TextView totalRides, userRating;
    private ImageView btnEdit, btnUploadDp;
    private de.hdodenhof.circleimageview.CircleImageView profileImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get current user ID
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            // Redirect to login if user is not logged in
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Load user data from Firestore
        loadUserData();

        // Set up click listeners
        setupClickListeners();

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, "PROFILE");
    }

    private void initializeViews() {
        // Profile information views
        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        profilePhone = findViewById(R.id.profile_phone);
        profileJoinDate = findViewById(R.id.profile_join_date);

        // Stats views
        totalRides = findViewById(R.id.total_rides);
        userRating = findViewById(R.id.user_rating);

        // Button views
        btnEdit = findViewById(R.id.btn_edit);
        btnUploadDp = findViewById(R.id.btn_upload_dp);

        // Profile image view
        profileImage = findViewById(R.id.profile_image);
    }

    private void loadUserData() {
        DocumentReference userRef = db.collection("users").document(currentUserId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Convert document to User object
                    User user = document.toObject(User.class);
                    if (user != null) {
                        updateUIWithUserData(user);
                    }
                } else {
                    // Document doesn't exist
                    setDefaultValues();
                }
            } else {
                // Error loading document
                setDefaultValues();
                Toast.makeText(ProfileActivity.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithUserData(User user) {
        // Set basic profile information
        profileName.setText(user.getFullName() != null ? user.getFullName() : "Not provided");
        profileEmail.setText(user.getEmail() != null ? user.getEmail() : "Not provided");

        // Handle phone number - show "Tap to add" if empty
        if (user.getPhone() != null && !user.getPhone().isEmpty() && !user.getPhone().equals("Tap to add phone number")) {
            profilePhone.setText(user.getPhone());
            profilePhone.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            profilePhone.setText("Tap to add phone number");
            profilePhone.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Format and set join date
        if (user.getCreatedAt() > 0) {
            String joinDate = formatDate(user.getCreatedAt());
            profileJoinDate.setText(joinDate);
        } else {
            profileJoinDate.setText("Unknown");
        }

        // Load and display profile image if available
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            // Use Glide to load the image
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .into(profileImage);
        } else {
            // Set default image
            profileImage.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        // TODO: Load ride statistics
        loadRideStatistics();
    }

    private void setDefaultValues() {
        profileName.setText("Not available");
        profileEmail.setText("Not available");
        profilePhone.setText("Tap to add phone number");
        profilePhone.setTextColor(getResources().getColor(android.R.color.darker_gray));
        profileJoinDate.setText("Unknown");

        // Set default values for stats
        totalRides.setText("0");
        userRating.setText("0.0");

        // Set default image
        profileImage.setImageResource(android.R.drawable.ic_menu_myplaces);
    }

    private String formatDate(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void setupClickListeners() {
        // Edit profile button - NOW OPENS PHONE DIALOG
        btnEdit.setOnClickListener(v -> {
            showEditPhoneDialog();
        });

        // Upload profile picture button
        btnUploadDp.setOnClickListener(v -> {
            openImagePicker();
        });

        // Edit profile menu item - NOW OPENS PHONE DIALOG
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            showEditPhoneDialog();
        });

        // Settings menu item
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            // TODO: Implement settings navigation
            Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Logout menu item
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            showLogoutConfirmation();
        });

        // Make phone number clickable to edit via dialog
        profilePhone.setOnClickListener(v -> {
            showEditPhoneDialog();
        });
        findViewById(R.id.btn_my_requests).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MyRequestsActivity.class);
            startActivity(intent);
        });
    }

    private void showEditPhoneDialog() {
        // Create dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Phone Number");

        // Create input field
        final TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        input.setHint("Enter your phone number");

        // Set current phone number if exists (but not the placeholder text)
        String currentPhone = profilePhone.getText().toString();
        if (!currentPhone.equals("Tap to add phone number")) {
            input.setText(currentPhone);
        }

        // Set layout parameters
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(lp);

        // Create container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 20, 50, 10);
        container.addView(input);

        builder.setView(container);

        // Set buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String phone = input.getText().toString().trim();
            if (!phone.isEmpty()) {
                updatePhoneNumber(phone);
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updatePhoneNumber(String phone) {
        // Update phone number in Firestore
        DocumentReference userRef = db.collection("users").document(currentUserId);

        userRef.update("phone", phone)
                .addOnSuccessListener(aVoid -> {
                    // Update UI immediately
                    profilePhone.setText(phone);
                    profilePhone.setTextColor(getResources().getColor(android.R.color.black));

                    Toast.makeText(this, "Phone number updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            // For now, just display the selected image locally
            // In a real app, you'd upload this to a server or Firebase Storage
            handleSelectedImage(imageUri);
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        // Display the selected image locally using Glide
        Glide.with(this)
                .load(imageUri)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .into(profileImage);

        // TODO: If you want to store images, you'll need to:
        // 1. Add Firebase Storage to your project
        // 2. Upload the image to Firebase Storage
        // 3. Get the download URL
        // 4. Store the URL in Firestore

        Toast.makeText(this, "Image selected locally. Add Firebase Storage to save permanently.", Toast.LENGTH_LONG).show();

        // Example of what you would do with Firebase Storage:
        /*
        StorageReference profileImageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images")
                .child(currentUserId + ".jpg");

        profileImageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Save this URL to Firestore
                    updateProfileImageUrl(uri.toString());
                });
            });
        */
    }

    // Method to update profile image URL in Firestore (when you add Firebase Storage)
    private void updateProfileImageUrl(String imageUrl) {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            performLogout();
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void performLogout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadRideStatistics() {
        // TODO: Implement ride statistics loading from Firestore
        /*
        db.collection("rides")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    int rideCount = task.getResult().size();
                    totalRides.setText(String.valueOf(rideCount));

                    // Calculate average rating
                    double totalRating = 0;
                    int ratedRides = 0;
                    for (DocumentSnapshot document : task.getResult()) {
                        if (document.contains("rating")) {
                            Double rating = document.getDouble("rating");
                            if (rating != null) {
                                totalRating += rating;
                                ratedRides++;
                            }
                        }
                    }

                    double averageRating = ratedRides > 0 ? totalRating / ratedRides : 0.0;
                    userRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                } else {
                    totalRides.setText("0");
                    userRating.setText("0.0");
                }
            });
        */

        // Temporary placeholder values
        totalRides.setText("0");
        userRating.setText("0.0");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when activity resumes to reflect any changes
        if (mAuth.getCurrentUser() != null) {
            loadUserData();
        }
    }
}
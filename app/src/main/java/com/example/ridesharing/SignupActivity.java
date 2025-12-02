package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final String REQUIRED_EMAIL_DOMAIN = "@std.ewubd.edu";

    // UI Components
    private TextInputEditText etFullName, etEmail, etPhone, etStudentId, etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private MaterialButton btnSignUp, btnGoogleSignIn;
    private ProgressBar progressBar;
    private ImageView btnBack;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        initializeFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
    }

    private void initializeFirebase() {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("482721895597-igo9hnlbbuutdglmgl0is2aba7oti3e1.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            if (validateInputs()) {
                createUserWithEmailAndPassword();
            }
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private boolean validateInputs() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Check if all fields are filled
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        // Validate email domain
        if (!email.endsWith(REQUIRED_EMAIL_DOMAIN)) {
            etEmail.setError("Email must be from @std.ewubd.edu domain");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }

        // Validate phone number (basic validation)
        if (phone.length() < 10) {
            etPhone.setError("Please enter a valid phone number");
            etPhone.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(studentId)) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createUserWithEmailAndPassword() {
        showProgressBar(true);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserDataToFirestore(user, false);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            showProgressBar(false);
                            String errorMessage = "Registration failed.";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        showProgressBar(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

                // Check if Google account email has correct domain
                if (account.getEmail() != null && account.getEmail().endsWith(REQUIRED_EMAIL_DOMAIN)) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    showProgressBar(false);
                    Toast.makeText(this, "Please use your @std.ewubd.edu email account", Toast.LENGTH_LONG).show();
                    mGoogleSignInClient.signOut(); // Sign out from Google
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                showProgressBar(false);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserDataToFirestore(user, true);
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            showProgressBar(false);
                            Toast.makeText(SignupActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(FirebaseUser user, boolean isGoogleSignIn) {
        // First check if the user document already exists
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        Map<String, Object> userData = new HashMap<>();

                        if (document.exists()) {
                            // User already exists - merge data (preserve existing fields)
                            if (isGoogleSignIn) {
                                // For Google Sign In, only update fields that Google provides
                                // and preserve existing data
                                userData.put("fullName", user.getDisplayName() != null ? user.getDisplayName() :
                                        document.getString("fullName"));
                                userData.put("email", user.getEmail());

                                // Save Google profile image URL if available
                                if (user.getPhotoUrl() != null) {
                                    String profileImageUrl = user.getPhotoUrl().toString();
                                    // Convert to higher resolution image
                                    profileImageUrl = profileImageUrl.replace("s96-c", "s400-c");
                                    userData.put("profileImageUrl", profileImageUrl);
                                } else {
                                    // Preserve existing profile image if no Google image
                                    if (document.contains("profileImageUrl") && document.getString("profileImageUrl") != null) {
                                        userData.put("profileImageUrl", document.getString("profileImageUrl"));
                                    }
                                }

                                // Preserve existing phone number if it exists
                                if (document.contains("phone") && document.getString("phone") != null
                                        && !document.getString("phone").isEmpty()) {
                                    userData.put("phone", document.getString("phone"));
                                } else {
                                    userData.put("phone", ""); // Keep empty if no existing phone
                                }
                                // Preserve existing studentId if it exists
                                if (document.contains("studentId") && document.getString("studentId") != null
                                        && !document.getString("studentId").isEmpty()) {
                                    userData.put("studentId", document.getString("studentId"));
                                } else {
                                    userData.put("studentId", ""); // Keep empty if no existing studentId
                                }
                                userData.put("signInMethod", "google");
                                userData.put("isVerified", true); // Google users are verified
                            } else {
                                // For email/password sign up, use form data (this is a new registration)
                                userData.put("fullName", etFullName.getText().toString().trim());
                                userData.put("email", etEmail.getText().toString().trim());
                                userData.put("phone", etPhone.getText().toString().trim());
                                userData.put("studentId", etStudentId.getText().toString().trim());
                                userData.put("profileImageUrl", ""); // Empty for email signup
                                userData.put("signInMethod", "email");
                                userData.put("isVerified", user.isEmailVerified());
                            }

                            // Update timestamp and verification status
                            userData.put("lastLogin", System.currentTimeMillis());

                            // Use update() instead of set() to preserve existing fields not being modified
                            db.collection("users").document(user.getUid())
                                    .update(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User data updated successfully");
                                        handleSignUpSuccess(user, isGoogleSignIn);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Error updating user data", e);
                                        // Even if update fails, still navigate
                                        handleSignUpSuccess(user, isGoogleSignIn);
                                    });

                        } else {
                            // New user - create document with all fields
                            if (isGoogleSignIn) {
                                // For Google Sign In
                                String profileImageUrl = "";
                                if (user.getPhotoUrl() != null) {
                                    profileImageUrl = user.getPhotoUrl().toString();
                                    // Convert to higher resolution image
                                    profileImageUrl = profileImageUrl.replace("s96-c", "s400-c");
                                }

                                userData.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
                                userData.put("email", user.getEmail());
                                userData.put("phone", ""); // Empty by default
                                userData.put("studentId", ""); // Empty by default
                                userData.put("profileImageUrl", profileImageUrl); // Save Google image
                                userData.put("signInMethod", "google");
                                userData.put("isVerified", true); // Google users are verified
                            } else {
                                // For email/password sign up
                                userData.put("fullName", etFullName.getText().toString().trim());
                                userData.put("email", etEmail.getText().toString().trim());
                                userData.put("phone", etPhone.getText().toString().trim());
                                userData.put("studentId", etStudentId.getText().toString().trim());
                                userData.put("profileImageUrl", ""); // Empty for email signup
                                userData.put("signInMethod", "email");
                                userData.put("isVerified", user.isEmailVerified());
                            }

                            userData.put("uid", user.getUid()); // Changed to "uid" to match your User model
                            userData.put("createdAt", System.currentTimeMillis());
                            userData.put("userType", "student"); // Add userType

                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User data saved successfully");
                                        handleSignUpSuccess(user, isGoogleSignIn);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Error saving user data", e);
                                        handleSignUpSuccess(user, isGoogleSignIn);
                                    });
                        }
                    } else {
                        Log.w(TAG, "Error checking user existence", task.getException());
                        // If we can't check existence, proceed with creating new document
                        createNewUserDocument(user, isGoogleSignIn);
                    }
                });
    }

    private void createNewUserDocument(FirebaseUser user, boolean isGoogleSignIn) {
        Map<String, Object> userData = new HashMap<>();

        if (isGoogleSignIn) {
            // For Google Sign In
            String profileImageUrl = "";
            if (user.getPhotoUrl() != null) {
                profileImageUrl = user.getPhotoUrl().toString();
                // Convert to higher resolution image
                profileImageUrl = profileImageUrl.replace("s96-c", "s400-c");
            }

            userData.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
            userData.put("email", user.getEmail());
            userData.put("phone", "");
            userData.put("studentId", "");
            userData.put("profileImageUrl", profileImageUrl); // Save Google image
            userData.put("signInMethod", "google");
            userData.put("isVerified", true); // Google users are verified
        } else {
            // For email/password sign up
            userData.put("fullName", etFullName.getText().toString().trim());
            userData.put("email", etEmail.getText().toString().trim());
            userData.put("phone", etPhone.getText().toString().trim());
            userData.put("studentId", etStudentId.getText().toString().trim());
            userData.put("profileImageUrl", ""); // Empty for email signup
            userData.put("signInMethod", "email");
            userData.put("isVerified", user.isEmailVerified());
        }

        userData.put("uid", user.getUid()); // Changed to "uid" to match your User model
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("userType", "student"); // Add userType

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved successfully");
                    handleSignUpSuccess(user, isGoogleSignIn);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving user data", e);
                    handleSignUpSuccess(user, isGoogleSignIn);
                });
    }

    private void handleSignUpSuccess(FirebaseUser user, boolean isGoogleSignIn) {
        showProgressBar(false);

        if (isGoogleSignIn) {
            Toast.makeText(SignupActivity.this, "Google Sign In Successful!", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            Toast.makeText(SignupActivity.this, "Account created successfully! Please verify your email.", Toast.LENGTH_LONG).show();
            sendEmailVerification(user);
            navigateToLoginActivity();
        }
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Verification email sent to " + user.getEmail());
                            // Don't show Toast here as it might conflict with the success message
                        } else {
                            Log.w(TAG, "Failed to send verification email", task.getException());
                        }
                    }
                });
    }

    private void navigateToLoginActivity() {
        // Sign out the user first (for email/password sign up)
        mAuth.signOut();

        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("registration_success", true);
        intent.putExtra("user_email", etEmail.getText().toString().trim());
        startActivity(intent);
        finish();
    }

    private void navigateToMainActivity() {
        // For Google sign up, user is already signed in, so go directly to main app
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgressBar(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSignUp.setEnabled(false);
            btnGoogleSignIn.setEnabled(false);
            btnBack.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSignUp.setEnabled(true);
            btnGoogleSignIn.setEnabled(true);
            btnBack.setEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to main activity
            navigateToMainActivity();
        }
    }
}
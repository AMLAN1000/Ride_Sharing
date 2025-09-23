package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final String REQUIRED_EMAIL_DOMAIN = "@std.ewubd.edu";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private TextView tvSignUp;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        initializeFirebase();
        setupClickListeners();

        // Debug logging
        Log.d(TAG, "LoginActivity created successfully");
        Log.d(TAG, "Firebase Auth: " + (mAuth != null ? "Initialized" : "NULL"));
        Log.d(TAG, "Firestore: " + (db != null ? "Initialized" : "NULL"));
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In with Web Client ID from your JSON
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("482721895597-igo9hnlbbuutdglmgl0is2aba7oti3e1.apps.googleusercontent.com") // Web client ID
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showLoading(true);
        Log.d(TAG, "Attempting login with email: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "User logged in: " + user.getUid());
                                Log.d(TAG, "Email verified: " + user.isEmailVerified());

                                // Check if user exists in Firestore
                                checkUserInFirestore(user);
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "Authentication failed.";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        // Sign out first to ensure account picker shows
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            showLoading(false);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign in successful: " + account.getEmail());

                // Check if Google account email has correct domain
                if (account.getEmail() != null && account.getEmail().endsWith(REQUIRED_EMAIL_DOMAIN)) {
                    showLoading(true);
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Please use your @std.ewubd.edu email account", Toast.LENGTH_LONG).show();
                    mGoogleSignInClient.signOut(); // Sign out from Google
                    Log.d(TAG, "Invalid email domain: " + account.getEmail());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "Firebase user created: " + user.getUid());
                                checkUserInFirestore(user);
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Google Authentication Failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkUserInFirestore(FirebaseUser firebaseUser) {
        Log.d(TAG, "Checking user in Firestore: " + firebaseUser.getUid());

        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "User found in Firestore");
                            // User exists in Firestore, proceed to main activity
                            navigateToMainActivity();
                        } else {
                            Log.d(TAG, "User not found in Firestore, creating user document");
                            // User doesn't exist in Firestore, create new user
                            createUserInFirestore(firebaseUser);
                        }
                    } else {
                        Log.w(TAG, "Error getting user document", task.getException());
                        Toast.makeText(LoginActivity.this, "Error checking user data: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserInFirestore(FirebaseUser firebaseUser) {
        Map<String, Object> userData = new HashMap<>();

        userData.put("userId", firebaseUser.getUid());
        userData.put("fullName", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
        userData.put("email", firebaseUser.getEmail());
        userData.put("phone", ""); // Will be filled later
        userData.put("studentId", ""); // Will be filled later
        userData.put("signInMethod", "google"); // This login is through Google
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("isVerified", firebaseUser.isEmailVerified());

        if (firebaseUser.getPhotoUrl() != null) {
            userData.put("profileImageUrl", firebaseUser.getPhotoUrl().toString());
        }

        db.collection("users").document(firebaseUser.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created successfully");
                    Toast.makeText(LoginActivity.this, "Welcome! Profile created successfully.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                    Toast.makeText(LoginActivity.this, "Error creating user profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Even if Firestore fails, still navigate to main activity
                    navigateToMainActivity();
                });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        if (!email.endsWith(REQUIRED_EMAIL_DOMAIN)) {
            etEmail.setError("Email must be from @std.ewubd.edu domain");
            etEmail.requestFocus();
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

        return true;
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnGoogleSignIn.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnGoogleSignIn.setEnabled(true);
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and verified
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Current user found: " + currentUser.getUid());

            // Check if user is verified or signed in with Google
            boolean isGoogleUser = false;
            if (currentUser.getProviderData() != null) {
                for (com.google.firebase.auth.UserInfo userInfo : currentUser.getProviderData()) {
                    if ("google.com".equals(userInfo.getProviderId())) {
                        isGoogleUser = true;
                        break;
                    }
                }
            }

            // Only auto-login if user is verified or is a Google user
            if (currentUser.isEmailVerified() || isGoogleUser) {
                checkUserInFirestore(currentUser);
            } else {
                Log.d(TAG, "User email not verified, staying on login screen");
                Toast.makeText(this, "Please verify your email to continue", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
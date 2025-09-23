package com.example.ridesharing;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000; // 1 second
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Create a simple splash layout

        mAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthStatus();
            }
        }, SPLASH_DELAY);
    }

    private void checkAuthStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in, check verification status
            boolean isGoogleUser = isGoogleSignedInUser(currentUser);

            if (currentUser.isEmailVerified() || isGoogleUser) {
                // User is verified or Google user, go to MainActivity
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // User not verified, go to LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
        } else {
            // No user logged in, go to LoginActivity
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }

    private boolean isGoogleSignedInUser(FirebaseUser user) {
        if (user.getProviderData() != null) {
            for (com.google.firebase.auth.UserInfo userInfo : user.getProviderData()) {
                if ("google.com".equals(userInfo.getProviderId())) {
                    return true;
                }
            }
        }
        return false;
    }
}
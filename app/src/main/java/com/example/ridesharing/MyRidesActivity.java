package com.example.ridesharing;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MyRidesActivity extends AppCompatActivity {

    private TextView tabOngoing, tabCompleted;
    private View btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        // Setup bottom navigation here
        BottomNavigationHelper.setupBottomNavigation(this, "MY_RIDES");

        initializeViews();
        setupClickListeners();

        // Load completed rides by default
        switchToCompletedTab();
    }

    private void initializeViews() {
        tabOngoing = findViewById(R.id.tab_ongoing);
        tabCompleted = findViewById(R.id.tab_completed);
        btnClose = findViewById(R.id.btn_close);

    }

    private void setupClickListeners() {
        tabOngoing.setOnClickListener(v -> {
            // Switch to ongoing tab here
            switchToOngoingTab();
        });

        tabCompleted.setOnClickListener(v -> {
            // Switch to completed tab here
            switchToCompletedTab();
        });
        // Close button click listener
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close the activity
                finish();
            }
        });
    }

    private void switchToOngoingTab() {
        // Update tab colors and load ongoing rides here
        // tabOngoing.setTextColor(getResources().getColor(android.R.color.white));
        // tabOngoing.setBackgroundResource(R.drawable.tab_selected_background);
        // tabCompleted.setTextColor(getResources().getColor(android.R.color.darker_gray));
        // tabCompleted.setBackgroundResource(R.drawable.tab_unselected_background);

        // Load ongoing rides data here
    }

    private void switchToCompletedTab() {
        // Update tab colors and load completed rides here
        // tabCompleted.setTextColor(getResources().getColor(android.R.color.white));
        // tabCompleted.setBackgroundResource(R.drawable.tab_selected_background);
        // tabOngoing.setTextColor(getResources().getColor(android.R.color.darker_gray));
        // tabOngoing.setBackgroundResource(R.drawable.tab_unselected_background);

        // Load completed rides data here
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Refresh bottom navigation here
        BottomNavigationHelper.setupBottomNavigation(this, "MY_RIDES");
    }
}
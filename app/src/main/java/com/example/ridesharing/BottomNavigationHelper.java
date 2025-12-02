package com.example.ridesharing;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BottomNavigationHelper {

    private static final String TAG = "BottomNav";

    public static void setupBottomNavigation(Activity activity, String selectedItem) {
        Log.d(TAG, "=== SETTING UP BOTTOM NAV ===");

        // Method 1: Try to find by direct ID first
        setupBottomNavDirect(activity, selectedItem);
    }

    private static void setupBottomNavDirect(Activity activity, String selectedItem) {
        Log.d(TAG, "Trying direct method...");

        // Try to find each button directly
        setupButtonDirect(activity, R.id.nav_home, MainActivity.class, "HOME".equals(selectedItem));
        setupButtonDirect(activity, R.id.nav_post, PostActivity.class, "POST".equals(selectedItem));
        setupButtonDirect(activity, R.id.nav_rides, MyRidesActivity.class, "RIDES".equals(selectedItem));
        setupButtonDirect(activity, R.id.nav_profile, ProfileActivity.class, "PROFILE".equals(selectedItem));
    }

    private static void setupButtonDirect(Activity activity, int buttonId, Class<?> targetActivity, boolean isSelected) {
        LinearLayout button = activity.findViewById(buttonId);

        if (button == null) {
            Log.e(TAG, "❌ BUTTON NOT FOUND: " + activity.getResources().getResourceEntryName(buttonId));
            return;
        }

        Log.d(TAG, "✅ BUTTON FOUND: " + activity.getResources().getResourceEntryName(buttonId));

        // Set selected state
        setButtonAppearance(button, isSelected);

        // Set click listener
        button.setOnClickListener(v -> {
            Log.d(TAG, "🎯 CLICKED: " + targetActivity.getSimpleName());

            if (!activity.getClass().equals(targetActivity)) {
                try {
                    Intent intent = new Intent(activity, targetActivity);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                } catch (Exception e) {
                    Log.e(TAG, "💥 ERROR: " + e.getMessage());
                    Toast.makeText(activity, "Cannot open " + targetActivity.getSimpleName(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static void setButtonAppearance(LinearLayout button, boolean isSelected) {
        if (button.getChildCount() >= 2) {
            ImageView icon = (ImageView) button.getChildAt(0);
            TextView text = (TextView) button.getChildAt(1);

            int selectedColor = button.getContext().getResources().getColor(R.color.blue);
            int defaultColor = button.getContext().getResources().getColor(R.color.gray);

            int color = isSelected ? selectedColor : defaultColor;

            if (icon != null) {
                icon.setColorFilter(color);
            }
            if (text != null) {
                text.setTextColor(color);
                text.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }

            Log.d(TAG, "🎨 " + (isSelected ? "SELECTED" : "DESELECTED") + ": " + button.getResources().getResourceEntryName(button.getId()));
        }
    }
}
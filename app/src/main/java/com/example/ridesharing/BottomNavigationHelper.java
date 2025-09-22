package com.example.ridesharing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.ridesharing.R;
import com.example.ridesharing.MainActivity;
import com.example.ridesharing.PostActivity;
/*
import com.example.ridesharing.MyRidesActivity;
import com.example.ridesharing.ProfileActivity;
*/

/**
 * Helper class to manage bottom navigation across all activities
 * This ensures consistent behavior and reduces code duplication
 */
public class BottomNavigationHelper {

    public enum NavigationItem {
        HOME, POST, MY_RIDES, PROFILE
    }

    /**
     * Setup bottom navigation for any activity that includes the bottom navigation layout
     *
     * @param activity The current activity
     * @param selectedItem Which item should be highlighted as selected
     */
    public static void setupBottomNavigation(Activity activity, NavigationItem selectedItem) {
        View bottomNav = activity.findViewById(R.id.bottom_navigation_include);
        if (bottomNav == null) return;

        LinearLayout btnHome = bottomNav.findViewById(R.id.btn_home);
        LinearLayout btnPost = bottomNav.findViewById(R.id.btn_post);
        LinearLayout btnMyRides = bottomNav.findViewById(R.id.btn_my_rides);
        LinearLayout btnProfile = bottomNav.findViewById(R.id.btn_profile);

        // Set click listeners
        btnHome.setOnClickListener(v -> navigateToActivity(activity, MainActivity.class));
        btnPost.setOnClickListener(v -> navigateToActivity(activity, PostActivity.class));
       btnMyRides.setOnClickListener(v -> navigateToActivity(activity, MyRidesActivity.class));
       btnProfile.setOnClickListener(v -> navigateToActivity(activity, ProfileActivity.class));

        // Set selected state
        setSelectedItem(bottomNav, selectedItem);
    }

    /**
     * Navigate to the specified activity
     */
    private static void navigateToActivity(Activity currentActivity, Class<?> targetActivity) {
        if (!currentActivity.getClass().equals(targetActivity)) {
            Intent intent = new Intent(currentActivity, targetActivity);
            currentActivity.startActivity(intent);

        }
    }

    /**
     * Set the visual state for the selected navigation item
     */
    private static void setSelectedItem(View bottomNav, NavigationItem selectedItem) {
        // Get all navigation items
        LinearLayout btnHome = bottomNav.findViewById(R.id.btn_home);
        LinearLayout btnPost = bottomNav.findViewById(R.id.btn_post);
        LinearLayout btnMyRides = bottomNav.findViewById(R.id.btn_my_rides);
        LinearLayout btnProfile = bottomNav.findViewById(R.id.btn_profile);

        // Reset all items to unselected state
        setItemState(btnHome, false, bottomNav.getContext());
        setItemState(btnPost, false, bottomNav.getContext());
        setItemState(btnMyRides, false, bottomNav.getContext());
        setItemState(btnProfile, false, bottomNav.getContext());

        // Set selected item
        switch (selectedItem) {
            case HOME:
                setItemState(btnHome, true, bottomNav.getContext());
                break;
            case POST:
                setItemState(btnPost, true, bottomNav.getContext());
                break;
            case MY_RIDES:
                setItemState(btnMyRides, true, bottomNav.getContext());
                break;
            case PROFILE:
                setItemState(btnProfile, true, bottomNav.getContext());
                break;
        }
    }

    /**
     * Set the visual state (colors) for a navigation item
     */
    private static void setItemState(LinearLayout item, boolean isSelected, Context context) {
        ImageView icon = (ImageView) item.getChildAt(0);
        TextView text = (TextView) item.getChildAt(1);

        int colorResource = isSelected ? R.color.selected_blue : R.color.unselected_gray;
        int color = ContextCompat.getColor(context, colorResource);

        icon.setColorFilter(color);
        text.setTextColor(color);

        if (isSelected) {
            text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            text.setTypeface(text.getTypeface(), android.graphics.Typeface.NORMAL);
        }
    }
}

/*
Usage in any Activity:

public class PostActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Setup bottom navigation with POST selected
        BottomNavigationHelper.setupBottomNavigation(this, BottomNavigationHelper.NavigationItem.POST);
    }
}
*/
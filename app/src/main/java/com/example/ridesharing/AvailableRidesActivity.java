package com.example.ridesharing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AvailableRidesActivity extends AppCompatActivity implements RideAdapter.OnRideClickListener {

    private static final String TAG = "AvailableRides";

    private RecyclerView ridesRecyclerView;
    private RideAdapter rideAdapter;
    private View emptyStateLayout, searchPanel;
    private ProgressBar progressBar;
    private TextView ridesCountText, subtitleText;
    private Chip chipActiveFilter;
    private TextInputEditText etFromLocation, etToLocation;
    private ImageView filterButton;

    private List<Ride> rideList = new ArrayList<>();
    private List<Ride> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration ridesListener;
    private boolean isLoading = false;

    // Search State
    private boolean isSearchActive = false;
    private String currentFromFilter = "";
    private String currentToFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_rides);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadAvailableRides();
        ExpiredRequestsCleaner.cleanExpiredPendingRequests();

        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        ridesRecyclerView = findViewById(R.id.rides_recyclerview);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        progressBar = findViewById(R.id.progress_bar);
        searchPanel = findViewById(R.id.search_panel);
        chipActiveFilter = findViewById(R.id.chip_active_filter);
        etFromLocation = findViewById(R.id.et_from_location);
        etToLocation = findViewById(R.id.et_to_location);
        ridesCountText = findViewById(R.id.rides_count_text);
        subtitleText = findViewById(R.id.subtitle_text);
        filterButton = findViewById(R.id.filter_button);

        if (filterButton != null) {
            filterButton.setOnClickListener(v -> toggleSearchPanel());
        }
    }

    private void setupSearchFunctionality() {
        View btnConfirmSearch = findViewById(R.id.btn_confirm_search);
        if (btnConfirmSearch != null) {
            btnConfirmSearch.setOnClickListener(v -> applySearchFilter());
        }

        View btnCancelSearch = findViewById(R.id.btn_cancel_search);
        if (btnCancelSearch != null) {
            btnCancelSearch.setOnClickListener(v -> cancelSearch());
        }

        if (chipActiveFilter != null) {
            chipActiveFilter.setOnCloseIconClickListener(v -> clearSearchFilter());
        }

        if (etFromLocation != null) {
            etFromLocation.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (isSearchActive) {
                        applySearchFilter();
                    }
                }
            });
        }

        if (etToLocation != null) {
            etToLocation.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (isSearchActive) {
                        applySearchFilter();
                    }
                }
            });
        }
    }

    private void toggleSearchPanel() {
        if (searchPanel != null) {
            if (searchPanel.getVisibility() == View.VISIBLE) {
                hideSearchPanel();
            } else {
                showSearchPanel();
            }
        }
    }

    private void showSearchPanel() {
        if (searchPanel != null) {
            searchPanel.setVisibility(View.VISIBLE);
        }
        if (etFromLocation != null) {
            etFromLocation.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etFromLocation, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void hideSearchPanel() {
        if (searchPanel != null) {
            searchPanel.setVisibility(View.GONE);
        }
        if (etFromLocation != null) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etFromLocation.getWindowToken(), 0);
            }
        }
    }

    private void applySearchFilter() {
        String from = etFromLocation != null ? etFromLocation.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String to = etToLocation != null ? etToLocation.getText().toString().trim().toLowerCase(Locale.ROOT) : "";

        currentFromFilter = from;
        currentToFilter = to;

        filteredList.clear();
        for (Ride ride : rideList) {
            String pickup = ride.getPickupLocation();
            String drop = ride.getDropLocation();

            boolean matchesFrom = from.isEmpty() || (pickup != null && pickup.toLowerCase(Locale.ROOT).contains(from));
            boolean matchesTo = to.isEmpty() || (drop != null && drop.toLowerCase(Locale.ROOT).contains(to));

            if (matchesFrom && matchesTo) {
                filteredList.add(ride);
            }
        }

        isSearchActive = !from.isEmpty() || !to.isEmpty();

        if (isSearchActive && chipActiveFilter != null) {
            chipActiveFilter.setVisibility(View.VISIBLE);
            String filterText = "Filter: ";
            if (!from.isEmpty()) filterText += "From " + from;
            if (!to.isEmpty()) filterText += (from.isEmpty() ? "To " : " to ") + to;
            chipActiveFilter.setText(filterText);
        } else if (chipActiveFilter != null) {
            chipActiveFilter.setVisibility(View.GONE);
        }

        updateUIWithFilteredResults();
        hideSearchPanel();
    }

    private void cancelSearch() {
        hideSearchPanel();
    }

    private void clearSearchFilter() {
        if (etFromLocation != null) etFromLocation.setText("");
        if (etToLocation != null) etToLocation.setText("");
        currentFromFilter = "";
        currentToFilter = "";
        isSearchActive = false;
        if (chipActiveFilter != null) chipActiveFilter.setVisibility(View.GONE);
        updateUIWithFilteredResults();
    }

    private void updateUIWithFilteredResults() {
        List<Ride> displayList = isSearchActive ? filteredList : rideList;
        rideAdapter.updateRides(displayList);
        updateUIState();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        ridesRecyclerView.setLayoutManager(layoutManager);
        ridesRecyclerView.setHasFixedSize(true);
        rideAdapter = new RideAdapter(filteredList, this);
        ridesRecyclerView.setAdapter(rideAdapter);
    }

    private void loadAvailableRides() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping duplicate call");
            return;
        }

        isLoading = true;
        showLoadingState();

        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }

        Log.d(TAG, "Loading driver-posted rides...");

        // Query for driver-posted rides (isDriverPost = true, status = pending)
        ridesListener = db.collection("ride_requests")
                .whereEqualTo("isDriverPost", true)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    isLoading = false;

                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage(), error);
                        runOnUiThread(() -> {
                            hideLoadingState();
                            showEmptyState("Error loading rides");
                            Toast.makeText(this, "Connection error. Check your internet.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        Log.e(TAG, "QueryDocumentSnapshots is null");
                        runOnUiThread(() -> {
                            hideLoadingState();
                            showEmptyState("No data received");
                        });
                        return;
                    }

                    Log.d(TAG, "Received " + queryDocumentSnapshots.size() + " driver-posted rides");

                    List<Ride> newRides = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Ride ride = parseRideFromFirestore(document);
                            if (ride != null && !"carpool".equals(document.getString("type"))) {
                                newRides.add(ride);
                                Log.d(TAG, "Successfully parsed: " + ride.getDriverName());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document " + document.getId(), e);
                        }
                    }

                    runOnUiThread(() -> {
                        rideList.clear();
                        rideList.addAll(newRides);

                        if (isSearchActive) {
                            applySearchFilter();
                        } else {
                            filteredList.clear();
                            filteredList.addAll(rideList);
                            rideAdapter.updateRides(filteredList);
                        }

                        hideLoadingState();
                        updateUIState();
                    });
                });
    }

    private Ride parseRideFromFirestore(QueryDocumentSnapshot document) {
        try {
            String id = document.getId();
            String driverId = document.getString("driverId");
            String driverName = document.getString("driverName");
            String driverPhone = document.getString("driverPhone");
            Double rating = document.getDouble("driverRating");
            String vehicleType = document.getString("vehicleType");
            Long passengersLong = document.getLong("passengers");
            String pickupLocation = document.getString("pickupLocation");
            String dropLocation = document.getString("dropLocation");
            Long departureTime = document.getLong("departureTime");
            Double fare = document.getDouble("fare");
            Double distance = document.getDouble("distance");
            Boolean isFareFair = document.getBoolean("isFareFair");

            // Format departure time
            String departureTimeStr = "Now";
            if (departureTime != null) {
                try {
                    departureTimeStr = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(new java.util.Date(departureTime));
                } catch (Exception e) {
                    departureTimeStr = "Now";
                }
            }

            // Calculate available seats
            int availableSeats = passengersLong != null ? passengersLong.intValue() : 1;

            return new Ride(
                    id,
                    driverName != null ? driverName : "Driver",
                    vehicleType != null ? vehicleType.toUpperCase() : "CAR",
                    availableSeats,
                    rating != null ? rating : 4.5,
                    pickupLocation != null ? pickupLocation : "Pickup",
                    dropLocation != null ? dropLocation : "Drop",
                    departureTimeStr,
                    "", // arrivalTime
                    fare != null ? fare : 0.0,
                    driverId,
                    driverPhone != null ? driverPhone : "",
                    distance != null ? distance : 0.0,
                    isFareFair != null ? isFareFair : true,
                    availableSeats
            );
        } catch (Exception e) {
            Log.e(TAG, "Critical error parsing ride", e);
            return null;
        }
    }

    private void updateUIState() {
        List<Ride> displayList = isSearchActive ? filteredList : rideList;

        if (displayList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            ridesRecyclerView.setVisibility(View.GONE);

            if (isSearchActive) {
                ridesCountText.setText("No matching rides");
                if (subtitleText != null) {
                    subtitleText.setText("Try different search terms");
                }
            } else {
                ridesCountText.setText("No rides available");
                if (subtitleText != null) {
                    subtitleText.setText("No driver-posted rides at the moment");
                }
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            ridesRecyclerView.setVisibility(View.VISIBLE);
            ridesCountText.setText(displayList.size() + " rides available");
            if (subtitleText != null) {
                if (isSearchActive) {
                    subtitleText.setText("Found " + displayList.size() + " matching rides");
                } else {
                    subtitleText.setText(displayList.size() + " rides near you");
                }
            }
        }
    }

    private void showLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
        ridesRecyclerView.setVisibility(View.GONE);
        ridesCountText.setText("Loading rides...");
    }

    private void hideLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
        ridesRecyclerView.setVisibility(View.GONE);
        ridesCountText.setText(message);
    }

    @Override
    public void onRideRequestClick(Ride ride) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to request rides", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Prevent drivers from accepting their own ride posts
        if (currentUserId.equals(ride.getDriverId())) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cannot Accept Own Ride")
                    .setMessage("You cannot accept your own posted ride. This feature is for passengers to find rides.")
                    .setPositiveButton("OK", null)
                    .show();
            Log.w(TAG, "Attempted self-acceptance by driver: " + currentUserId);
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Request Ride")
                .setMessage("Request ride with " + ride.getDriverName() +
                        "\n\nFare: ৳" + String.format(Locale.getDefault(), "%.0f", ride.getFare()) +
                        "\nVehicle: " + ride.getVehicleModel() +
                        "\nAvailable Seats: " + ride.getAvailableSeats())
                .setPositiveButton("Confirm", (dialog, which) -> acceptRide(ride))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRideCallClick(Ride ride) {
        callDriver(ride);
    }

    @Override
    public void onRideMessageClick(Ride ride) {
        Toast.makeText(this, "Messaging feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProfileViewClick(Ride ride) {
        String profileInfo = "Name: " + ride.getDriverName() +
                "\n\nRating: " + String.format(Locale.getDefault(), "%.1f", ride.getRating()) + " ⭐" +
                "\n\nVehicle: " + ride.getVehicleModel() +
                "\n\nAvailable Seats: " + ride.getAvailableSeats();

        if (ride.getDriverPhone() != null && !ride.getDriverPhone().isEmpty()) {
            profileInfo += "\n\n📞 Driver Phone: " + ride.getDriverPhone();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Driver Profile")
                .setMessage(profileInfo)
                .setPositiveButton("OK", null)
                .setNeutralButton("Call Driver", (dialog, which) -> callDriver(ride))
                .show();
    }

    private void callDriver(Ride ride) {
        if (ride.getDriverPhone() != null && !ride.getDriverPhone().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ride.getDriverPhone()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Driver phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void acceptRide(Ride ride) {
        String passengerId = mAuth.getCurrentUser().getUid();
        Toast.makeText(this, "Accepting ride...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(passengerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String passengerName = "Passenger";
                    String passengerPhone = "";

                    if (documentSnapshot.exists()) {
                        passengerName = documentSnapshot.getString("fullName");
                        if (passengerName == null || passengerName.trim().isEmpty()) {
                            passengerName = mAuth.getCurrentUser().getDisplayName();
                            if (passengerName == null || passengerName.trim().isEmpty()) {
                                passengerName = "Passenger";
                            }
                        }
                        passengerPhone = documentSnapshot.getString("phone");
                        if (passengerPhone == null) {
                            passengerPhone = "";
                        }
                    }

                    db.collection("ride_requests").document(ride.getId())
                            .update(
                                    "status", "accepted",
                                    "passengerId", passengerId,
                                    "passengerName", passengerName,
                                    "passengerPhone", passengerPhone,
                                    "acceptedAt", System.currentTimeMillis(),
                                    "notificationShown", false,
                                    "notificationSentBy", passengerId
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Ride accepted! Contact " +
                                        ride.getDriverName(), Toast.LENGTH_LONG).show();
                                Log.d(TAG, "✅ Ride accepted, StatusMonitor will notify driver");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error accepting ride", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching passenger info", e);
                    Toast.makeText(this, "Failed to get passenger details.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "RIDES");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ridesListener != null) {
            ridesListener.remove();
            ridesListener = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSearchPanel();
    }
}
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AvailableRequestsActivity extends AppCompatActivity implements RideRequestAdapter.OnRequestClickListener {

    private static final String TAG = "AvailableRequests";

    // UI Components
    private RecyclerView requestsRecyclerView;
    private RideRequestAdapter requestAdapter;
    private View emptyStateLayout, searchPanel;
    private ProgressBar progressBar;
    private TextView requestsCountText, subtitleText;
    private Chip chipActiveFilter;
    private TextInputEditText etFromLocation, etToLocation;
    private ImageView filterButton;
    private View refreshButton;

    // Data Lists
    private List<RideRequest> requestList = new ArrayList<>();
    private List<RideRequest> filteredList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration requestsListener;

    // Search State
    private boolean isSearchActive = false;
    private String currentFromFilter = "";
    private String currentToFilter = "";

    // Loading state
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_requests);

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadRideRequests();

        try {
            BottomNavigationHelper.setupBottomNavigation(this, "HOME");
        } catch (Exception e) {
            Log.e(TAG, "Bottom navigation setup failed", e);
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        requestsRecyclerView = findViewById(R.id.requests_recyclerview);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        progressBar = findViewById(R.id.progress_bar);
        searchPanel = findViewById(R.id.search_panel);
        chipActiveFilter = findViewById(R.id.chip_active_filter);
        requestsCountText = findViewById(R.id.requests_count_text);
        subtitleText = findViewById(R.id.subtitle_text);
        etFromLocation = findViewById(R.id.et_from_location);
        etToLocation = findViewById(R.id.et_to_location);
        filterButton = findViewById(R.id.filter_button);

        if (filterButton != null) {
            filterButton.setOnClickListener(v -> toggleSearchPanel());
        }

        refreshButton = emptyStateLayout != null ? emptyStateLayout.findViewById(R.id.btn_refresh) : null;
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadRideRequests());
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
        for (RideRequest request : requestList) {
            String source = request.getSource();
            String destination = request.getDestination();

            boolean matchesFrom = from.isEmpty() || (source != null && source.toLowerCase(Locale.ROOT).contains(from));
            boolean matchesTo = to.isEmpty() || (destination != null && destination.toLowerCase(Locale.ROOT).contains(to));

            if (matchesFrom && matchesTo) {
                filteredList.add(request);
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

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        requestsRecyclerView.setLayoutManager(layoutManager);
        requestAdapter = new RideRequestAdapter(filteredList, this);
        requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void loadRideRequests() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping duplicate call");
            return;
        }

        isLoading = true;
        showLoadingState();

        // Remove existing listener
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }

        Log.d(TAG, "Starting to load ride requests...");

        // Simple query without orderBy to avoid index issues
        requestsListener = db.collection("ride_requests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {

                    isLoading = false;

                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage(), error);
                        runOnUiThread(() -> {
                            hideLoadingState();
                            showEmptyState("Error: " + error.getMessage());
                            Toast.makeText(AvailableRequestsActivity.this,
                                    "Connection error. Check your internet.", Toast.LENGTH_LONG).show();
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

                    Log.d(TAG, "Received " + queryDocumentSnapshots.size() + " documents");

                    List<RideRequest> newRequests = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d(TAG, "Parsing document: " + document.getId());
                            RideRequest request = parseRideRequestFromFirestore(document);
                            if (request != null) {
                                newRequests.add(request);
                                Log.d(TAG, "Successfully parsed: " + request.getPassengerName());
                            } else {
                                Log.w(TAG, "Request parsed as null for document: " + document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception parsing document " + document.getId() + ": " + e.getMessage(), e);
                        }
                    }

                    // Sort by createdAt in memory (newest first)
                    // NOTE: This assumes 'createdAt' field is available and comparable (e.g., Long)
                    Collections.sort(newRequests, new Comparator<RideRequest>() {
                        @Override
                        public int compare(RideRequest r1, RideRequest r2) {
                            // If you store createdAt in RideRequest, compare them here:
                            // return Long.compare(r2.getCreatedAt(), r1.getCreatedAt()); // Newest first
                            return 0; // Defaulting to no specific sort for this example
                        }
                    });

                    runOnUiThread(() -> {
                        requestList.clear();
                        requestList.addAll(newRequests);

                        Log.d(TAG, "Updated request list with " + requestList.size() + " items");

                        if (isSearchActive) {
                            applySearchFilter();
                        } else {
                            filteredList.clear();
                            filteredList.addAll(requestList);
                            if (requestAdapter != null) {
                                requestAdapter.updateRequests(filteredList);
                            }
                        }

                        hideLoadingState();
                        updateUIState();
                    });
                });
    }

    private RideRequest parseRideRequestFromFirestore(QueryDocumentSnapshot document) {
        try {
            String id = document.getId();
            Log.d(TAG, "Parsing document ID: " + id);

            String passengerId = document.contains("passengerId") ? document.getString("passengerId") : "";

            // Get all fields with extensive null checking
            String passengerName = document.contains("passengerName") ? document.getString("passengerName") : null;
            if (passengerName == null || passengerName.trim().isEmpty()) {
                passengerName = "Anonymous Passenger";
            }

            String passengerPhoto = document.contains("passengerPhoto") ? document.getString("passengerPhoto") : "";
            String passengerPhone = document.contains("passengerPhone") ? document.getString("passengerPhone") : "";

            Double rating = document.contains("passengerRating") ? document.getDouble("passengerRating") : null;
            if (rating == null) rating = 4.5;

            String pickupLocation = document.contains("pickupLocation") ? document.getString("pickupLocation") : null;
            if (pickupLocation == null || pickupLocation.trim().isEmpty()) {
                pickupLocation = "Pickup Location";
            }

            String dropLocation = document.contains("dropLocation") ? document.getString("dropLocation") : null;
            if (dropLocation == null || dropLocation.trim().isEmpty()) {
                dropLocation = "Drop Location";
            }

            Double pickupLat = document.contains("pickupLat") ? document.getDouble("pickupLat") : null;
            Double pickupLng = document.contains("pickupLng") ? document.getDouble("pickupLng") : null;
            Double dropLat = document.contains("dropLat") ? document.getDouble("dropLat") : null;
            Double dropLng = document.contains("dropLng") ? document.getDouble("dropLng") : null;

            Double fare = document.contains("fare") ? document.getDouble("fare") : null;
            if (fare == null) fare = 0.0;

            String vehicleType = document.contains("vehicleType") ? document.getString("vehicleType") : null;
            if (vehicleType == null || vehicleType.trim().isEmpty()) {
                vehicleType = "car";
            }

            Long passengersLong = document.contains("passengers") ? document.getLong("passengers") : null;
            int passengers = passengersLong != null ? passengersLong.intValue() : 1;

            Long departureTime = document.contains("departureTime") ? document.getLong("departureTime") : null;
            Long createdAt = document.contains("createdAt") ? document.getLong("createdAt") : null;

            String specialRequest = document.contains("specialRequest") ? document.getString("specialRequest") : "";
            if (specialRequest == null) specialRequest = "";

            Double distance = document.contains("distance") ? document.getDouble("distance") : null;
            Double duration = document.contains("duration") ? document.getDouble("duration") : null;
            String trafficLevel = document.contains("trafficLevel") ? document.getString("trafficLevel") : "Unknown";

            // Calculate time remaining
            long currentTime = System.currentTimeMillis();
            long timeRemaining = 0;
            if (departureTime != null) {
                timeRemaining = departureTime - currentTime;
            }
            String timeRemainingStr = formatTimeRemaining(timeRemaining);

            // Format departure time
            String departureTimeStr = "Now";
            if (departureTime != null) {
                try {
                    departureTimeStr = new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(new java.util.Date(departureTime));
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting departure time", e);
                    departureTimeStr = "Now";
                }
            }

            String userType = "Passenger";

            Log.d(TAG, "Successfully created RideRequest for: " + passengerName);

            return new RideRequest(
                    id,
                    passengerName,
                    userType,
                    rating,
                    pickupLocation,
                    dropLocation,
                    departureTimeStr,
                    timeRemainingStr,
                    fare,
                    passengerId, // Pass passengerId to the RideRequest model
                    passengers,
                    specialRequest,
                    passengerPhoto,
                    passengerPhone,
                    vehicleType,
                    pickupLat, pickupLng, dropLat, dropLng,
                    distance, duration, trafficLevel
            );
        } catch (Exception e) {
            Log.e(TAG, "Critical error parsing document: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }

    private String formatTimeRemaining(long milliseconds) {
        if (milliseconds <= 0) {
            return "Now";
        }

        long hours = milliseconds / (1000 * 60 * 60);
        long minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60);

        if (hours > 24) {
            long days = hours / 24;
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + " min";
        } else {
            return "Now";
        }
    }

    private void updateUIWithFilteredResults() {
        if (isSearchActive) {
            if (requestAdapter != null) {
                requestAdapter.updateRequests(filteredList);
            }
        } else {
            filteredList.clear();
            filteredList.addAll(requestList);
            if (requestAdapter != null) {
                requestAdapter.updateRequests(filteredList);
            }
        }
        updateUIState();
    }

    private void updateUIState() {
        List<RideRequest> displayList = isSearchActive ? filteredList : requestList;

        if (displayList.isEmpty()) {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
            if (requestsRecyclerView != null) requestsRecyclerView.setVisibility(View.GONE);

            if (subtitleText != null) {
                if (isSearchActive) {
                    subtitleText.setText("No matching ride requests found");
                } else {
                    subtitleText.setText("No ride requests available at the moment");
                }
            }
        } else {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
            if (requestsRecyclerView != null) requestsRecyclerView.setVisibility(View.VISIBLE);

            if (subtitleText != null) {
                if (isSearchActive) {
                    subtitleText.setText("Found " + displayList.size() + " matching requests");
                } else {
                    subtitleText.setText(displayList.size() + " ride requests available");
                }
            }
        }
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
            if (requestsRecyclerView != null) {
                requestsRecyclerView.setVisibility(View.GONE);
            }
            if (subtitleText != null) {
                subtitleText.setText("Loading ride requests...");
            }
        });
    }

    private void hideLoadingState() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showEmptyState(String message) {
        runOnUiThread(() -> {
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
            if (requestsRecyclerView != null) {
                requestsRecyclerView.setVisibility(View.GONE);
            }
            if (subtitleText != null) {
                subtitleText.setText(message);
            }
        });
    }

    /**
     * Guardrail against self-acceptance implemented here.
     */
    @Override
    public void onAcceptRequestClick(RideRequest request) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to accept requests", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // --- CRITICAL CHECK: Prevent self-acceptance ---
        if (currentUserId.equals(request.getPassengerId())) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cannot Accept Own Request")
                    .setMessage("You cannot accept your own ride request. This feature is for drivers to find passengers.")
                    .setPositiveButton("OK", null)
                    .show();
            Log.w(TAG, "Attempted self-acceptance by user: " + currentUserId);
            return;
        }
        // ---------------------------------------------

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Accept Ride Request")
                .setMessage("Accept ride from " + request.getPassengerName() +
                        "\n\nFare: ৳" + String.format(Locale.getDefault(), "%.0f", request.getOfferedFare()) +
                        "\nVehicle: " + (request.getVehicleType() != null ? request.getVehicleType().toUpperCase() : "CAR") +
                        "\nPassengers: " + request.getPassengers() +
                        "\nDistance: " + (request.getDistance() != null ? String.format(Locale.getDefault(), "%.1f km", request.getDistance()) : "N/A"))
                .setPositiveButton("Accept", (dialog, which) -> acceptRideRequest(request))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptRideRequest(RideRequest request) {
        String driverId = mAuth.getCurrentUser().getUid();

        // Removed: String driverName = mAuth.getCurrentUser().getDisplayName();
        // This is now fetched from Firestore to get 'fullName'.

        Toast.makeText(this, "Accepting request...", Toast.LENGTH_SHORT).show();

        // Get driver's name and phone number from Firestore
        db.collection("users").document(driverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String driverName = "Driver"; // Default value
                    String driverPhone = "";

                    if (documentSnapshot.exists()) {
                        // Retrieve the correct full name from the 'users' document
                        driverName = documentSnapshot.getString("fullName");
                        if (driverName == null || driverName.trim().isEmpty()) {
                            // Fallback if fullName is missing in the document
                            driverName = mAuth.getCurrentUser().getDisplayName();
                            if (driverName == null || driverName.trim().isEmpty()) {
                                driverName = "Driver";
                            }
                        }

                        // Retrieve phone number
                        driverPhone = documentSnapshot.getString("phone");
                        if (driverPhone == null) {
                            driverPhone = "";
                        }
                    } else {
                        Log.w(TAG, "Driver user document not found for ID: " + driverId);
                    }

                    // Update ride request with driver info
                    db.collection("ride_requests").document(request.getId())
                            .update(
                                    "status", "accepted",
                                    "driverId", driverId,
                                    // Use the retrieved driverName (fullName) here
                                    "driverName", driverName,
                                    "driverPhone", driverPhone,
                                    "acceptedAt", System.currentTimeMillis(),
                                    "notificationShown", false
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "✅ Request accepted! Contact " +
                                        request.getPassengerName(), Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error accepting request", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching driver info", e);
                    Toast.makeText(this, "Failed to get driver details.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onPassengerProfileClick(RideRequest request) {
        String profileInfo = "Name: " + request.getPassengerName() +
                "\n\nRating: " + String.format(Locale.getDefault(), "%.1f", request.getRating()) + " ⭐" +
                "\n\nType: " + request.getUserType();

        if (request.getPassengerPhone() != null && !request.getPassengerPhone().isEmpty()) {
            profileInfo += "\n\nPhone: " + request.getPassengerPhone();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Passenger Profile")
                .setMessage(profileInfo)
                .setPositiveButton("OK", null)
                .setNeutralButton("Call", (dialog, which) -> onCallPassengerClick(request))
                .show();
    }

    @Override
    public void onMessagePassengerClick(RideRequest request) {
        Toast.makeText(this, "Messaging feature coming soon for " + request.getPassengerName(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCallPassengerClick(RideRequest request) {
        if (request.getPassengerPhone() != null && !request.getPassengerPhone().isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + request.getPassengerPhone()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open phone dialer", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error opening dialer", e);
            }
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewMapClick(RideRequest request) {
        if (request.getPickupLat() != null && request.getPickupLng() != null &&
                request.getDropLat() != null && request.getDropLng() != null) {
            try {
                // Corrected the Google Maps directions URI to use the standard 'daddr' and 'saddr' parameters
                String uri = String.format(Locale.ENGLISH,
                        "http://maps.google.com/maps?saddr=%f,%f&daddr=%f,%f",
                        request.getPickupLat(), request.getPickupLng(),
                        request.getDropLat(), request.getDropLng());

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            } catch (Exception e) {
                // Fallback to general intent view
                String uri = String.format(Locale.ENGLISH,
                        "http://maps.google.com/maps?saddr=%f,%f&daddr=%f,%f",
                        request.getPickupLat(), request.getPickupLng(),
                        request.getDropLat(), request.getDropLng());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        } else {
            Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            BottomNavigationHelper.setupBottomNavigation(this, "HOME");
        } catch (Exception e) {
            Log.e(TAG, "Bottom navigation setup failed", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSearchPanel();
    }
}
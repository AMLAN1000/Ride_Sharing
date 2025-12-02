package com.example.ridesharing;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AvailableRequestsActivity extends AppCompatActivity implements RideRequestAdapter.OnRequestClickListener {

    private RecyclerView requestsRecyclerView;
    private RideRequestAdapter requestAdapter;
    private View emptyStateLayout, searchPanel;
    private TextView requestsCountText, subtitleText;
    private Chip chipActiveFilter;
    private TextInputEditText etFromLocation, etToLocation;
    private List<RideRequest> requestList = new ArrayList<>();
    private List<RideRequest> filteredList = new ArrayList<>();
    private boolean isSearchActive = false;
    private String currentFromFilter = "";
    private String currentToFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_requests);

        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadSampleRequests();

        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
    }

    private void initializeViews() {
        requestsRecyclerView = findViewById(R.id.requests_recyclerview);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        searchPanel = findViewById(R.id.search_panel);
        chipActiveFilter = findViewById(R.id.chip_active_filter);
        requestsCountText = findViewById(R.id.requests_count_text);
        subtitleText = findViewById(R.id.subtitle_text);
        etFromLocation = findViewById(R.id.et_from_location);
        etToLocation = findViewById(R.id.et_to_location);

        // Setup filter button
        ImageView filterButton = findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> toggleSearchPanel());

        // Setup refresh button
        View refreshButton = emptyStateLayout.findViewById(R.id.btn_refresh);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadSampleRequests());
        }
    }

    private void setupSearchFunctionality() {
        // Confirm search button
        View btnConfirmSearch = findViewById(R.id.btn_confirm_search);
        btnConfirmSearch.setOnClickListener(v -> applySearchFilter());

        // Cancel search button
        View btnCancelSearch = findViewById(R.id.btn_cancel_search);
        btnCancelSearch.setOnClickListener(v -> cancelSearch());

        // Close active filter chip
        chipActiveFilter.setOnCloseIconClickListener(v -> clearSearchFilter());

        // Real-time search as user types (optional)
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

    private void toggleSearchPanel() {
        if (searchPanel.getVisibility() == View.VISIBLE) {
            hideSearchPanel();
        } else {
            showSearchPanel();
        }
    }

    private void showSearchPanel() {
        searchPanel.setVisibility(View.VISIBLE);
        // Auto-focus on from field
        etFromLocation.requestFocus();
        // Show keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etFromLocation, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearchPanel() {
        searchPanel.setVisibility(View.GONE);
        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etFromLocation.getWindowToken(), 0);
    }

    private void applySearchFilter() {
        String from = etFromLocation.getText().toString().trim().toLowerCase(Locale.ROOT);
        String to = etToLocation.getText().toString().trim().toLowerCase(Locale.ROOT);

        currentFromFilter = from;
        currentToFilter = to;

        // Filter the list
        filteredList.clear();
        for (RideRequest request : requestList) {
            boolean matchesFrom = from.isEmpty() ||
                    request.getSource().toLowerCase(Locale.ROOT).contains(from);
            boolean matchesTo = to.isEmpty() ||
                    request.getDestination().toLowerCase(Locale.ROOT).contains(to);

            if (matchesFrom && matchesTo) {
                filteredList.add(request);
            }
        }

        isSearchActive = !from.isEmpty() || !to.isEmpty();

        if (isSearchActive) {
            chipActiveFilter.setVisibility(View.VISIBLE);
            String filterText = "Filter: ";
            if (!from.isEmpty()) filterText += "From " + from;
            if (!to.isEmpty()) filterText += (from.isEmpty() ? "To " : " to ") + to;
            chipActiveFilter.setText(filterText);
        } else {
            chipActiveFilter.setVisibility(View.GONE);
        }

        updateUIWithFilteredResults();
        hideSearchPanel();
    }

    private void cancelSearch() {
        hideSearchPanel();
        // Clear filters but keep the text in fields
        isSearchActive = false;
        chipActiveFilter.setVisibility(View.GONE);
        updateUIWithFilteredResults();
    }

    private void clearSearchFilter() {
        etFromLocation.setText("");
        etToLocation.setText("");
        currentFromFilter = "";
        currentToFilter = "";
        isSearchActive = false;
        chipActiveFilter.setVisibility(View.GONE);
        updateUIWithFilteredResults();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        requestsRecyclerView.setLayoutManager(layoutManager);

        requestAdapter = new RideRequestAdapter(filteredList, this);
        requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void loadSampleRequests() {
        showLoadingState();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<RideRequest> sampleRequests = createSampleRequests();
            requestList.clear();
            requestList.addAll(sampleRequests);

            // Apply current filter if any
            if (isSearchActive) {
                applySearchFilter();
            } else {
                filteredList.clear();
                filteredList.addAll(requestList);
                requestAdapter.updateRequests(filteredList);
                updateUIState();
            }
        }, 1000);
    }

    private List<RideRequest> createSampleRequests() {
        List<RideRequest> requests = new ArrayList<>();

        requests.add(new RideRequest("1", "Alice Johnson", "Student", 4.8,
                "EWU Main Campus, Aftabnagar", "Bashundhara City Mall",
                "2:30 PM", "3:15 PM", 120.0, "alice123", 2, "Urgent: Exam tomorrow"));

        requests.add(new RideRequest("2", "Bob Smith", "Professional", 4.9,
                "EWU Permanent Campus", "Jamuna Future Park",
                "4:00 PM", "4:45 PM", 150.0, "bob456", 1, "Shopping trip"));

        requests.add(new RideRequest("3", "Carol Davis", "Teacher", 4.7,
                "Rampura Bridge", "Airport Area",
                "5:30 PM", "6:15 PM", 200.0, "carol789", 1, "Going to class"));

        requests.add(new RideRequest("4", "David Wilson", "Engineer", 4.6,
                "Bashundhara R/A", "EWU Main Campus",
                "6:00 PM", "6:40 PM", 80.0, "david101", 3, "Pickup needed"));

        return requests;
    }

    private void updateUIWithFilteredResults() {
        if (isSearchActive) {
            requestAdapter.updateRequests(filteredList);
        } else {
            filteredList.clear();
            filteredList.addAll(requestList);
            requestAdapter.updateRequests(filteredList);
        }
        updateUIState();
    }

    private void updateUIState() {
        List<RideRequest> displayList = isSearchActive ? filteredList : requestList;

        if (displayList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);

            if (isSearchActive) {
                subtitleText.setText("No matching ride requests found");
            } else {
                subtitleText.setText("No ride requests available at the moment");
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);

            if (isSearchActive) {
                subtitleText.setText("Found " + displayList.size() + " matching requests");
            } else {
                subtitleText.setText(displayList.size() + " ride requests near you");
            }
        }
    }

    private void showLoadingState() {
        emptyStateLayout.setVisibility(View.GONE);
        requestsRecyclerView.setVisibility(View.VISIBLE);
        subtitleText.setText("Loading ride requests...");
    }

    // ... rest of your existing methods (onAcceptRequestClick, etc.)

    @Override
    public void onAcceptRequestClick(RideRequest request) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Accept Ride Request")
                .setMessage("Accept ride request from " + request.getPassengerName() + " for ৳" + request.getOfferedFare() + "?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    android.widget.Toast.makeText(this, "Ride request accepted! Contacting " + request.getPassengerName(), android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onPassengerProfileClick(RideRequest request) {
        android.widget.Toast.makeText(this, "Viewing " + request.getPassengerName() + "'s profile", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessagePassengerClick(RideRequest request) {
        android.widget.Toast.makeText(this, "Messaging " + request.getPassengerName(), android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "HOME");
    }
}
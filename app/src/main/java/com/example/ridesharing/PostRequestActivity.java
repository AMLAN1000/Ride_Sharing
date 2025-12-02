package com.example.ridesharing;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostRequestActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    // UI Components
    private TextView tvPickupLocation, tvDropLocation, tvDepartureTime, tvPassengersCount, tvDistance, tvDuration, tvTrafficInfo;
    private EditText etFare, etSpecialRequest;
    private Button btnCheckFare, btnPostRequest, btnDecreasePassengers, btnIncreasePassengers;
    private com.google.android.material.card.MaterialCardView cardRoutePreview;
    private android.widget.LinearLayout layoutTrafficInfo, layoutFareAnalysis;
    private TextView tvFairRange, tvFairnessMessage, tvDetailedReason, tvSuggestion;

    // Map
    private GoogleMap mMap;
    private Marker pickupMarker, dropMarker;
    private Polyline routePolyline;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private final Calendar calendar = Calendar.getInstance();
    private double calculatedFairFare = 0;
    private double minFairFare = 0;
    private double maxFairFare = 0;
    private LatLng pickupLatLng, dropLatLng;
    private String currentSelectionType = "pickup";
    private int passengersCount = 1;

    // Route data
    private double routeDistance = 0;
    private double routeDuration = 0;
    private double trafficDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_request);

        initializeViews();
        setupFirebase();
        setupMap();
        setupClickListeners();

        // Setup bottom navigation
        BottomNavigationHelper.setupBottomNavigation(this, "POST");
    }

    private void initializeViews() {
        // Location TextViews
        tvPickupLocation = findViewById(R.id.tv_pickup_location);
        tvDropLocation = findViewById(R.id.tv_drop_location);

        // Route Preview
        cardRoutePreview = findViewById(R.id.card_route_preview);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);
        tvTrafficInfo = findViewById(R.id.tv_traffic_info);
        layoutTrafficInfo = findViewById(R.id.layout_traffic_info);

        // Time
        tvDepartureTime = findViewById(R.id.tv_departure_time);

        // Fare
        etFare = findViewById(R.id.et_fare);
        btnCheckFare = findViewById(R.id.btn_check_fare);
        btnPostRequest = findViewById(R.id.btn_post_request);

        // Fare Analysis
        layoutFareAnalysis = findViewById(R.id.layout_fare_analysis);
        tvFairRange = findViewById(R.id.tv_fair_range);
        tvFairnessMessage = findViewById(R.id.tv_fairness_message);
        tvDetailedReason = findViewById(R.id.tv_detailed_reason);
        tvSuggestion = findViewById(R.id.tv_suggestion);

        // Passengers
        tvPassengersCount = findViewById(R.id.tv_passengers_count);
        btnDecreasePassengers = findViewById(R.id.btn_decrease_passengers);
        btnIncreasePassengers = findViewById(R.id.btn_increase_passengers);

        // Special Request
        etSpecialRequest = findViewById(R.id.et_special_request);

        // Set default values
        tvPassengersCount.setText(String.valueOf(passengersCount));

        // Initially disable buttons
        btnCheckFare.setEnabled(false);
        btnPostRequest.setEnabled(false);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupClickListeners() {
        // Location selection
        tvPickupLocation.setOnClickListener(v -> {
            currentSelectionType = "pickup";
            showMapSelectionDialog("Select Pickup Location");
        });

        tvDropLocation.setOnClickListener(v -> {
            currentSelectionType = "drop";
            showMapSelectionDialog("Select Drop Location");
        });

        // Time selection
        tvDepartureTime.setOnClickListener(v -> showDateTimePicker());

        // Fare checking
        btnCheckFare.setOnClickListener(v -> checkFareFairness());

        // Post request
        btnPostRequest.setOnClickListener(v -> postRideRequest());

        // Passenger counter
        btnDecreasePassengers.setOnClickListener(v -> {
            if (passengersCount > 1) {
                passengersCount--;
                tvPassengersCount.setText(String.valueOf(passengersCount));
                validateFormCompleteness();
            }
        });

        btnIncreasePassengers.setOnClickListener(v -> {
            if (passengersCount < 4) {
                passengersCount++;
                tvPassengersCount.setText(String.valueOf(passengersCount));
                validateFormCompleteness();
            }
        });

        // Real-time form validation
        android.text.TextWatcher formValidator = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                validateFormCompleteness();
            }
        };

        etFare.addTextChangedListener(formValidator);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupMapSettings();

        // Set default location (Dhaka)
        LatLng dhaka = new LatLng(23.8103, 90.4125);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dhaka, 12));
    }

    private void setupMapSettings() {
        if (mMap != null) {
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    private void showMapSelectionDialog(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Inflate custom layout
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_map_selection, null);
        builder.setView(dialogView);

        FrameLayout mapContainer = dialogView.findViewById(R.id.map_container);

        AlertDialog dialog = builder.create();

        // Setup map in dialog
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(mapContainer.getId(), mapFragment)
                .commit();

        mapFragment.getMapAsync(googleMap -> {
            GoogleMap dialogMap = googleMap;
            setupDialogMapSettings(dialogMap);
            dialogMap.setOnMapClickListener(latLng -> {
                // Handle map click in dialog
                onMapClick(latLng);
                dialog.dismiss(); // Close dialog after selection
            });

            // Move to default location
            LatLng dhaka = new LatLng(23.8103, 90.4125);
            dialogMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dhaka, 12));
        });

        builder.setPositiveButton("Confirm Selection", (dialogInterface, which) -> {
            // Location is already handled by onMapClick
        });

        builder.setNegativeButton("Cancel", (dialogInterface, which) -> {
            // Dialog will be dismissed automatically
        });

        dialog.show();
    }

    private void setupDialogMapSettings(GoogleMap map) {
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Add marker at clicked location
        if (currentSelectionType.equals("pickup")) {
            if (pickupMarker != null) pickupMarker.remove();
            pickupMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Pickup Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            pickupLatLng = latLng;
            updateLocationText(latLng, "pickup");
        } else {
            if (dropMarker != null) dropMarker.remove();
            dropMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Drop Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            dropLatLng = latLng;
            updateLocationText(latLng, "drop");
        }

        // If both locations are selected, calculate route
        if (pickupLatLng != null && dropLatLng != null) {
            calculateRouteAndTraffic();
        }

        validateFormCompleteness();
    }

    private void updateLocationText(LatLng latLng, String type) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        String address = addresses.get(0).getAddressLine(0);
                        if (type.equals("pickup")) {
                            tvPickupLocation.setText(address);
                        } else {
                            tvDropLocation.setText(address);
                        }
                    } else {
                        // Fallback to coordinates
                        String shortAddress = String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f",
                                latLng.latitude, latLng.longitude);
                        if (type.equals("pickup")) {
                            tvPickupLocation.setText(shortAddress);
                        } else {
                            tvDropLocation.setText(shortAddress);
                        }
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    String shortAddress = String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f",
                            latLng.latitude, latLng.longitude);
                    if (type.equals("pickup")) {
                        tvPickupLocation.setText(shortAddress);
                    } else {
                        tvDropLocation.setText(shortAddress);
                    }
                });
            }
        }).start();
    }

    private void calculateRouteAndTraffic() {
        if (pickupLatLng == null || dropLatLng == null) return;

        // Show route preview
        cardRoutePreview.setVisibility(android.view.View.VISIBLE);

        // Calculate direct distance using Haversine
        routeDistance = calculateHaversineDistance(pickupLatLng, dropLatLng);

        // Simulate traffic data
        simulateTrafficAndRouteCalculation();
    }

    private void simulateTrafficAndRouteCalculation() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // Base duration calculation (1 km ≈ 2 minutes in normal traffic)
        routeDuration = routeDistance * 2;

        // Apply traffic multiplier
        double trafficMultiplier = getTrafficMultiplier();
        trafficDuration = routeDuration * trafficMultiplier;

        // Update UI
        runOnUiThread(() -> {
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", routeDistance));
            tvDuration.setText(String.format(Locale.getDefault(), "%.0f min", trafficDuration));

            // Show traffic information
            layoutTrafficInfo.setVisibility(android.view.View.VISIBLE);
            String trafficLevel = getTrafficLevel(hour);
            tvTrafficInfo.setText("Traffic: " + trafficLevel);

            // Draw route on map
            drawRouteOnMap();
        });
    }

    private void drawRouteOnMap() {
        if (mMap == null || pickupLatLng == null || dropLatLng == null) return;

        // Clear previous polyline
        if (routePolyline != null) {
            routePolyline.remove();
        }

        // Create a simple straight line for demo
        routePolyline = mMap.addPolyline(new PolylineOptions()
                .add(pickupLatLng, dropLatLng)
                .width(8)
                .color(getResources().getColor(android.R.color.holo_blue_dark))
                .geodesic(true));

        // Fit map to show both markers with padding
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(dropLatLng);
        LatLngBounds bounds = builder.build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private String getTrafficLevel(int hour) {
        if ((hour >= 8 && hour <= 9) || (hour >= 17 && hour <= 18)) {
            return "Heavy";
        } else if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19)) {
            return "Moderate";
        } else {
            return "Light";
        }
    }

    private void validateFormCompleteness() {
        boolean pickupSelected = pickupLatLng != null;
        boolean dropSelected = dropLatLng != null;
        boolean fareEntered = !etFare.getText().toString().trim().isEmpty();
        boolean timeSelected = !tvDepartureTime.getText().toString().equals("Tap to select date & time");

        boolean isComplete = pickupSelected && dropSelected && fareEntered && timeSelected;

        btnCheckFare.setEnabled(isComplete);
        btnCheckFare.setBackgroundColor(isComplete ?
                getResources().getColor(android.R.color.holo_blue_dark) :
                getResources().getColor(android.R.color.darker_gray));
    }

    private void showDateTimePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    updateTimeButton();
                    validateFormCompleteness();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void updateTimeButton() {
        String dateFormat = "MMM dd, yyyy hh:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        tvDepartureTime.setText(sdf.format(calendar.getTime()));
    }

    private void checkFareFairness() {
        // Show loading
        btnCheckFare.setText("Calculating...");
        btnCheckFare.setEnabled(false);

        // Show fare analysis section
        layoutFareAnalysis.setVisibility(android.view.View.VISIBLE);

        // Calculate fare fairness
        calculateFareFairness();
    }

    private void calculateFareFairness() {
        if (pickupLatLng == null || dropLatLng == null) {
            showFareResult("Please select valid locations", false, "");
            resetCheckFareButton();
            return;
        }

        double userFare;
        try {
            userFare = Double.parseDouble(etFare.getText().toString().trim());
        } catch (NumberFormatException e) {
            showFareResult("Please enter a valid fare amount", false, "");
            resetCheckFareButton();
            return;
        }

        // Calculate fair fare
        calculateFairFare(routeDistance, passengersCount);

        // Check if user fare is within fair range
        if (userFare >= minFairFare && userFare <= maxFairFare) {
            String reason = getFareFairReason(userFare);
            showFareResult("✓ Fare is fair! You can post your request.", true, reason);
            enablePostButton();
        } else {
            String reason = getFareUnfairReason(userFare, routeDistance);
            String suggestion = getFareSuggestion(userFare);
            showFareResult("Fare needs adjustment", false, reason, suggestion);
            disablePostButton();
        }

        // Show fair range
        tvFairRange.setText(String.format(Locale.getDefault(), "Fair range: ৳%d - ৳%d (Distance: %.1f km)",
                (int)minFairFare, (int)maxFairFare, routeDistance));

        resetCheckFareButton();
    }

    private void calculateFairFare(double distance, int passengers) {
        // Base fare calculation
        double baseFare = 60.0;
        double farePerKm = 15.0;

        calculatedFairFare = baseFare + (distance * farePerKm);

        // Apply multipliers
        calculatedFairFare *= getTimeMultiplier();
        calculatedFairFare *= getTrafficMultiplier();
        calculatedFairFare *= getPassengerMultiplier(passengers);
        calculatedFairFare *= getDistanceMultiplier(distance);

        // Apply traffic duration factor
        double trafficFactor = Math.max(1.0, trafficDuration / routeDuration);
        calculatedFairFare *= trafficFactor;

        // Set fair range
        double range = Math.max(50, calculatedFairFare * 0.2);
        minFairFare = Math.max(calculatedFairFare - range, baseFare);
        maxFairFare = calculatedFairFare + range;

        // Round to nearest 10
        minFairFare = Math.round(minFairFare / 10) * 10;
        maxFairFare = Math.round(maxFairFare / 10) * 10;
        calculatedFairFare = Math.round(calculatedFairFare / 10) * 10;
    }

    private String getFareFairReason(double userFare) {
        return String.format(Locale.getDefault(),
                "Your fare of ৳%.0f is perfect for:\n• %.1f km distance\n• %s\n• %s traffic conditions\n• %d passenger%s",
                userFare, routeDistance, getTimeOfDayDescription(),
                getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)),
                passengersCount, passengersCount > 1 ? "s" : "");
    }

    private String getFareUnfairReason(double userFare, double distance) {
        if (userFare < minFairFare) {
            return String.format(Locale.getDefault(),
                    "Your fare of ৳%.0f is too low because:\n• Distance: %.1f km\n• Time: %s\n• Traffic: %s\n• Additional %d passenger%s\n• Current traffic adds %.0f min extra travel time",
                    userFare, distance, getTimeOfDayDescription(), getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)),
                    passengersCount - 1, passengersCount > 2 ? "s" : "", trafficDuration - routeDuration);
        } else {
            return String.format(Locale.getDefault(),
                    "Your fare of ৳%.0f is too high because:\n• Distance: %.1f km\n• Time: %s\n• Traffic: %s\n• Drivers may not accept this fare for the current conditions",
                    userFare, distance, getTimeOfDayDescription(), getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));
        }
    }

    private String getFareSuggestion(double userFare) {
        if (userFare < minFairFare) {
            return String.format(Locale.getDefault(),
                    "💡 Suggestion: Increase fare to ৳%d - ৳%d range to account for %s traffic and %s timing",
                    (int)minFairFare, (int)maxFairFare, getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)).toLowerCase(),
                    getTimeOfDayDescription());
        } else {
            return String.format(Locale.getDefault(),
                    "💡 Suggestion: Decrease fare to ৳%d - ৳%d range to attract more drivers for this %s trip",
                    (int)minFairFare, (int)maxFairFare, routeDistance > 10 ? "long" : "short");
        }
    }

    private void showFareResult(String message, boolean isSuccess, String detailedReason, String... suggestion) {
        tvFairnessMessage.setText(message);
        tvFairnessMessage.setTextColor(isSuccess ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));

        tvDetailedReason.setText(detailedReason);
        tvDetailedReason.setVisibility(android.view.View.VISIBLE);

        if (suggestion.length > 0) {
            tvSuggestion.setText(suggestion[0]);
            tvSuggestion.setVisibility(android.view.View.VISIBLE);
        } else {
            tvSuggestion.setVisibility(android.view.View.GONE);
        }
    }

    private void enablePostButton() {
        btnPostRequest.setEnabled(true);
        btnPostRequest.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    private void disablePostButton() {
        btnPostRequest.setEnabled(false);
        btnPostRequest.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }

    private void resetCheckFareButton() {
        btnCheckFare.setText("Check Fare Fairness");
        btnCheckFare.setEnabled(true);
    }

    private void postRideRequest() {
        if (!validateForm()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showFareResult("Please login to post a request", false, "You need to be logged in to post ride requests.");
            return;
        }

        // Show loading
        btnPostRequest.setText("Posting...");
        btnPostRequest.setEnabled(false);

        // Create ride request object
        Map<String, Object> rideRequest = createRideRequestData(currentUser);

        // Save to Firebase
        db.collection("ride_requests")
                .add(rideRequest)
                .addOnSuccessListener(documentReference -> {
                    showFareResult("✓ Ride request posted successfully!", true, "Your request is now visible to drivers.");
                    clearForm();

                    // Navigate back after success
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(PostRequestActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }, 2000);
                })
                .addOnFailureListener(e -> {
                    showFareResult("Failed to post ride request", false, "Error: " + e.getMessage());
                    btnPostRequest.setText("Post Request");
                    btnPostRequest.setEnabled(true);
                });
    }

    private Map<String, Object> createRideRequestData(FirebaseUser currentUser) {
        Map<String, Object> rideRequest = new HashMap<>();
        rideRequest.put("passengerId", currentUser.getUid());
        rideRequest.put("passengerName", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Anonymous");
        rideRequest.put("pickupLocation", tvPickupLocation.getText().toString());
        rideRequest.put("dropLocation", tvDropLocation.getText().toString());
        rideRequest.put("pickupLat", pickupLatLng.latitude);
        rideRequest.put("pickupLng", pickupLatLng.longitude);
        rideRequest.put("dropLat", dropLatLng.latitude);
        rideRequest.put("dropLng", dropLatLng.longitude);
        rideRequest.put("fare", Double.parseDouble(etFare.getText().toString().trim()));
        rideRequest.put("passengers", passengersCount);
        rideRequest.put("departureTime", calendar.getTimeInMillis());
        rideRequest.put("specialRequest", etSpecialRequest.getText().toString().trim());
        rideRequest.put("status", "pending");
        rideRequest.put("createdAt", System.currentTimeMillis());
        rideRequest.put("calculatedFairFare", calculatedFairFare);
        rideRequest.put("minFairFare", minFairFare);
        rideRequest.put("maxFairFare", maxFairFare);
        rideRequest.put("distance", routeDistance);
        rideRequest.put("duration", trafficDuration);
        rideRequest.put("isFareFair", true);
        rideRequest.put("trafficLevel", getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));

        return rideRequest;
    }

    private boolean validateForm() {
        if (pickupLatLng == null) {
            showFareResult("Please select pickup location", false, "Tap on 'Pickup Location' to select from map.");
            return false;
        }

        if (dropLatLng == null) {
            showFareResult("Please select drop location", false, "Tap on 'Drop Location' to select from map.");
            return false;
        }

        if (etFare.getText().toString().trim().isEmpty()) {
            showFareResult("Please enter fare amount", false, "Enter your proposed fare in the fare field.");
            return false;
        }

        return true;
    }

    private void clearForm() {
        // Clear location selections
        tvPickupLocation.setText("Tap to select pickup point on map");
        tvDropLocation.setText("Tap to select drop point on map");
        pickupLatLng = null;
        dropLatLng = null;

        // Clear time
        tvDepartureTime.setText("Tap to select date & time");

        // Clear fare and analysis
        etFare.setText("");
        layoutFareAnalysis.setVisibility(android.view.View.GONE);

        // Reset passengers
        passengersCount = 1;
        tvPassengersCount.setText("1");

        // Clear special request
        etSpecialRequest.setText("");

        // Clear map
        if (mMap != null) {
            mMap.clear();
        }
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        // Hide route preview
        cardRoutePreview.setVisibility(android.view.View.GONE);
        layoutTrafficInfo.setVisibility(android.view.View.GONE);

        // Reset buttons
        btnPostRequest.setEnabled(false);
        btnPostRequest.setText("Post Request");
        btnCheckFare.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.setupBottomNavigation(this, "POST");
    }

    // Helper methods
    private double calculateHaversineDistance(LatLng point1, LatLng point2) {
        double lat1 = point1.latitude;
        double lon1 = point1.longitude;
        double lat2 = point2.latitude;
        double lon2 = point2.longitude;

        double earthRadius = 6371; // kilometers

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        return Math.max(distance, 1.0); // Minimum 1 km
    }

    private String getTimeOfDayDescription() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 22 || hour <= 5) return "late night";
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 20)) return "peak hours";
        return "normal hours";
    }

    private double getTimeMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            return 1.2;
        }
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 20)) {
            return 1.3;
        }
        if (hour >= 22 || hour <= 5) {
            return 1.4;
        }
        return 1.0;
    }

    private double getTrafficMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if ((hour >= 8 && hour <= 9) || (hour >= 17 && hour <= 18)) {
            return 1.25;
        }
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19)) {
            return 1.15;
        }
        return 1.0;
    }

    private double getPassengerMultiplier(int passengers) {
        return 1.0 + (passengers - 1) * 0.15;
    }

    private double getDistanceMultiplier(double distance) {
        if (distance > 20) {
            return 0.9;
        } else if (distance > 10) {
            return 0.95;
        }
        return 1.0;
    }
}
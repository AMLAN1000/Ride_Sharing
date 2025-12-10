package com.example.ridesharing;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class CarpoolPostActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "CarpoolPostActivity";
    private static final String GOOGLE_MAPS_API_KEY = "AIzaSyCD-k7OlWsemXLHwBXyBoQNO8r9rxRc9nM";
    private static final String OPENWEATHER_API_KEY = "acc6e705fe0c4b7c67e77a98cfc26122";
    private static final String OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    // UI Components
    private AutocompleteSupportFragment autocompleteStart, autocompleteEnd;
    private TextView tvDepartureTime, tvSeatsCount;
    private TextView tvDistance, tvDuration, tvTrafficInfo, tvWeatherInfo;
    private EditText etTotalFare, etSpecialRequest;
    private Button btnCheckFare, btnPostCarpool, btnDecreaseSeats, btnIncreaseSeats;
    private Button btnAddStop, btnRemoveStop;
    private MaterialCardView cardRoutePreview;
    private LinearLayout layoutTrafficInfo, layoutFareAnalysis, layoutWeatherInfo;
    private LinearLayout middleStopsContainer;
    private TextView tvFairRange, tvFairnessMessage, tvDetailedReason, tvSuggestion;
    private RadioGroup radioGroupVehicleType;
    private RadioButton rbCar;

    // Map
    private GoogleMap mMap;
    private Polyline routePolyline;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data
    private final Calendar calendar = Calendar.getInstance();
    private double calculatedTotalFare = 0;
    private double farePerPassenger = 0;
    private double minFairFare = 0;
    private double maxFairFare = 0;
    private String pickupAddress = "";
    private String dropAddress = "";
    private int seatsAvailable = 3; // Default seats for carpool
    private double routeDistance = 0;
    private double routeDuration = 0;
    private double trafficDuration = 0;
    private String vehicleType = "car"; // default

    // Weather data
    private String currentWeather = "Clear";
    private String weatherDescription = "";
    private double weatherMultiplier = 1.0;
    private double temperature = 0;

    // Route management
    private List<RouteStop> routeStops = new ArrayList<>();
    private List<AutocompleteSupportFragment> middleStopFragments = new ArrayList<>();
    private Map<String, AutocompleteSupportFragment> autocompleteFragments = new HashMap<>();

    // Track if autocomplete is being cleared
    private boolean isClearingAutocomplete = false;

    // RouteStop class
    private static class RouteStop {
        private String address;
        private LatLng latLng;
        private int orderIndex;
        private String autocompleteFragmentId; // Track which fragment created this stop

        public RouteStop(String address, LatLng latLng, int orderIndex, String fragmentId) {
            this.address = address;
            this.latLng = latLng;
            this.orderIndex = orderIndex;
            this.autocompleteFragmentId = fragmentId;
        }

        public String getAddress() { return address; }
        public LatLng getLatLng() { return latLng; }
        public int getOrderIndex() { return orderIndex; }
        public String getAutocompleteFragmentId() { return autocompleteFragmentId; }

        public void setAddress(String address) { this.address = address; }
        public void setLatLng(LatLng latLng) { this.latLng = latLng; }
        public void setAutocompleteFragmentId(String fragmentId) { this.autocompleteFragmentId = fragmentId; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpool_post);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), GOOGLE_MAPS_API_KEY);
        }

        initializeViews();
        setupFirebase();
        setupMap();
        setupAutocomplete();
        setupClickListeners();

        try {
            BottomNavigationHelper.setupBottomNavigation(this, "POST");
        } catch (Exception e) {
            Log.e(TAG, "Bottom navigation setup failed", e);
        }
    }

    private void initializeViews() {
        cardRoutePreview = findViewById(R.id.card_route_preview);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);
        tvTrafficInfo = findViewById(R.id.tv_traffic_info);
        tvWeatherInfo = findViewById(R.id.tv_weather_info);
        layoutTrafficInfo = findViewById(R.id.layout_traffic_info);
        layoutWeatherInfo = findViewById(R.id.layout_weather_info);
        tvDepartureTime = findViewById(R.id.tv_departure_time);
        etTotalFare = findViewById(R.id.et_total_fare);
        btnCheckFare = findViewById(R.id.btn_check_fare);
        btnPostCarpool = findViewById(R.id.btn_post_carpool);
        layoutFareAnalysis = findViewById(R.id.layout_fare_analysis);
        tvFairRange = findViewById(R.id.tv_fair_range);
        tvFairnessMessage = findViewById(R.id.tv_fairness_message);
        tvDetailedReason = findViewById(R.id.tv_detailed_reason);
        tvSuggestion = findViewById(R.id.tv_suggestion);
        tvSeatsCount = findViewById(R.id.tv_seats_count);
        middleStopsContainer = findViewById(R.id.middle_stops_container);
        btnAddStop = findViewById(R.id.btn_add_stop);
        btnRemoveStop = findViewById(R.id.btn_remove_stop);
        btnDecreaseSeats = findViewById(R.id.btn_decrease_seats);
        btnIncreaseSeats = findViewById(R.id.btn_increase_seats);
        etSpecialRequest = findViewById(R.id.et_special_request);

        // Vehicle type selection - Only car for carpool
        radioGroupVehicleType = findViewById(R.id.radio_group_vehicle_type);
        rbCar = findViewById(R.id.rb_car);
        rbCar.setChecked(true); // Force car for carpool

        // Hide bike option
        RadioButton rbBike = findViewById(R.id.rb_bike);
        if (rbBike != null) {
            rbBike.setVisibility(View.GONE);
        }

        tvSeatsCount.setText(String.valueOf(seatsAvailable));
        btnCheckFare.setEnabled(false);
        btnPostCarpool.setEnabled(false);
        btnRemoveStop.setVisibility(View.GONE);

        // Hide weather layout initially
        layoutWeatherInfo.setVisibility(View.GONE);
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

    private void setupAutocomplete() {
        // Start Point Autocomplete
        autocompleteStart = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_start);
        if (autocompleteStart != null) {
            setupStartAutocomplete(autocompleteStart);
        }

        // End Point Autocomplete
        autocompleteEnd = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_end);
        if (autocompleteEnd != null) {
            setupEndAutocomplete(autocompleteEnd);
        }
    }

    private void setupStartAutocomplete(AutocompleteSupportFragment fragment) {
        fragment.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        ));
        fragment.setHint("Starting point");
        fragment.setCountries("BD");

        fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (isClearingAutocomplete) {
                    return; // Ignore if we're clearing
                }

                LatLng latLng = place.getLatLng();
                String address = place.getAddress() != null ? place.getAddress() : place.getName();

                // Check if start point already exists
                RouteStop existingStart = null;
                for (RouteStop stop : routeStops) {
                    if (stop.getOrderIndex() == 0) {
                        existingStart = stop;
                        break;
                    }
                }

                if (existingStart != null) {
                    // Update existing start point
                    existingStart.setAddress(address);
                    existingStart.setLatLng(latLng);
                    existingStart.setAutocompleteFragmentId("start");
                } else {
                    // Create new start point
                    RouteStop newStop = new RouteStop(address, latLng, 0, "start");
                    routeStops.add(0, newStop);
                }

                updateMapMarkers();
                validateFormCompleteness();
            }

            @Override
            public void onError(com.google.android.gms.common.api.Status status) {
                Log.e(TAG, "Start point error: " + status);
                Toast.makeText(CarpoolPostActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
            }
        });

        // Store fragment reference
        autocompleteFragments.put("start", fragment);

        // Setup text watcher for clearing detection
        new android.os.Handler().postDelayed(() -> {
            setupAutocompleteTextWatcher(fragment, "start");
        }, 200);
    }

    private void setupEndAutocomplete(AutocompleteSupportFragment fragment) {
        fragment.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        ));
        fragment.setHint("End point");
        fragment.setCountries("BD");

        fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (isClearingAutocomplete) {
                    return; // Ignore if we're clearing
                }

                LatLng latLng = place.getLatLng();
                String address = place.getAddress() != null ? place.getAddress() : place.getName();

                // Find the end point (highest order index or specifically marked)
                RouteStop existingEnd = null;
                int maxIndex = -1;
                for (RouteStop stop : routeStops) {
                    if (stop.getOrderIndex() > maxIndex) {
                        maxIndex = stop.getOrderIndex();
                        existingEnd = stop;
                    }
                }

                if (existingEnd != null && existingEnd.getOrderIndex() == maxIndex) {
                    // Update existing end point
                    existingEnd.setAddress(address);
                    existingEnd.setLatLng(latLng);
                    existingEnd.setAutocompleteFragmentId("end");
                } else {
                    // Create new end point
                    int newIndex = routeStops.isEmpty() ? 1 : maxIndex + 1;
                    RouteStop newStop = new RouteStop(address, latLng, newIndex, "end");
                    routeStops.add(newStop);
                }

                updateMapMarkers();
                validateFormCompleteness();
            }

            @Override
            public void onError(com.google.android.gms.common.api.Status status) {
                Log.e(TAG, "End point error: " + status);
                Toast.makeText(CarpoolPostActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
            }
        });

        // Store fragment reference
        autocompleteFragments.put("end", fragment);

        // Setup text watcher for clearing detection
        new android.os.Handler().postDelayed(() -> {
            setupAutocompleteTextWatcher(fragment, "end");
        }, 200);
    }

    private void setupAutocompleteTextWatcher(AutocompleteSupportFragment fragment, String fragmentId) {
        if (fragment.getView() != null) {
            EditText editText = fragment.getView().findViewById(
                    com.google.android.libraries.places.R.id.places_autocomplete_search_input);
            if (editText != null) {
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s.toString().isEmpty() && !isClearingAutocomplete) {
                            // Text was cleared - remove the associated stop
                            removeStopByFragmentId(fragmentId);
                        }
                    }
                });
            }
        }
    }

    private void setupClickListeners() {
        tvDepartureTime.setOnClickListener(v -> showDateTimePicker());
        btnCheckFare.setOnClickListener(v -> checkFareFairness());
        btnPostCarpool.setOnClickListener(v -> postCarpool());
        btnAddStop.setOnClickListener(v -> addMiddleStop());
        btnRemoveStop.setOnClickListener(v -> removeLastMiddleStop());

        radioGroupVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_car) {
                vehicleType = "car";
            }
            validateFormCompleteness();
        });

        btnDecreaseSeats.setOnClickListener(v -> {
            if (seatsAvailable > 1) {
                seatsAvailable--;
                tvSeatsCount.setText(String.valueOf(seatsAvailable));
                validateFormCompleteness();
            }
        });

        btnIncreaseSeats.setOnClickListener(v -> {
            if (seatsAvailable < 6) { // Max 6 seats for carpool
                seatsAvailable++;
                tvSeatsCount.setText(String.valueOf(seatsAvailable));
                validateFormCompleteness();
            }
        });

        etTotalFare.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                validateFormCompleteness();
            }
        });
    }

    private void addMiddleStop() {
        if (middleStopFragments.size() >= 3) {
            Toast.makeText(this, "Maximum 3 middle stops allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        if (routeStops.isEmpty()) {
            Toast.makeText(this, "Please select starting point first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create container for new stop
        LinearLayout stopLayout = new LinearLayout(this);
        stopLayout.setOrientation(LinearLayout.HORIZONTAL);
        stopLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        stopLayout.setLayoutParams(params);

        // Add icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_mylocation);
        icon.setColorFilter(getResources().getColor(android.R.color.holo_orange_light));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
        iconParams.setMarginEnd(16);
        icon.setLayoutParams(iconParams);
        stopLayout.addView(icon);

        // Add autocomplete fragment
        FrameLayout fragmentContainer = new FrameLayout(this);
        int containerId = View.generateViewId();
        fragmentContainer.setId(containerId);

        // Use simple ID instead of tag for fragment
        String fragmentId = "middle_" + (middleStopFragments.size() + 1);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        fragmentContainer.setLayoutParams(containerParams);
        stopLayout.addView(fragmentContainer);

        middleStopsContainer.addView(stopLayout);

        // Create and add fragment
        AutocompleteSupportFragment fragment = new AutocompleteSupportFragment();

        // Commit the fragment transaction immediately
        getSupportFragmentManager().beginTransaction()
                .add(containerId, fragment)
                .commitNow();

        // Now setup the fragment AFTER it's been committed
        setupMiddleStopFragment(fragment, fragmentId);
        middleStopFragments.add(fragment);
        btnRemoveStop.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Middle stop added! Select location", Toast.LENGTH_SHORT).show();
    }

    private void setupMiddleStopFragment(AutocompleteSupportFragment fragment, String fragmentId) {
        fragment.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
        ));
        fragment.setHint("Stop " + (middleStopFragments.size()));
        fragment.setCountries("BD");

        // Set up place selection listener
        fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (isClearingAutocomplete) {
                    return; // Ignore if we're clearing
                }

                LatLng latLng = place.getLatLng();
                String address = place.getAddress() != null ? place.getAddress() : place.getName();

                // Check if a stop with this fragment already exists
                RouteStop existingStop = findStopByFragmentId(fragmentId);

                if (existingStop != null) {
                    // Update existing stop
                    existingStop.setAddress(address);
                    existingStop.setLatLng(latLng);
                } else {
                    // Find the correct order index for this middle stop
                    int newOrderIndex = 1; // Start after the pickup (index 0)
                    for (RouteStop stop : routeStops) {
                        if (stop.getOrderIndex() >= newOrderIndex) {
                            newOrderIndex = stop.getOrderIndex() + 1;
                        }
                    }

                    // Adjust if this would be after the end point
                    RouteStop endStop = null;
                    int maxIndex = -1;
                    for (RouteStop stop : routeStops) {
                        if (stop.getOrderIndex() > maxIndex) {
                            maxIndex = stop.getOrderIndex();
                            endStop = stop;
                        }
                    }

                    if (endStop != null && "end".equals(endStop.getAutocompleteFragmentId())) {
                        // End point exists, place middle stop before it
                        newOrderIndex = Math.min(newOrderIndex, endStop.getOrderIndex());
                    }

                    RouteStop newStop = new RouteStop(address, latLng, newOrderIndex, fragmentId);
                    routeStops.add(newStop);

                    // Reorder stops to maintain sequence
                    reorderStops();
                }

                updateMapMarkers();
                validateFormCompleteness();
            }

            @Override
            public void onError(com.google.android.gms.common.api.Status status) {
                Log.e(TAG, "Middle stop error: " + status);
                Toast.makeText(CarpoolPostActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
            }
        });

        // Store fragment reference with its ID
        autocompleteFragments.put(fragmentId, fragment);

        // Add text watcher to detect clearing
        new android.os.Handler().postDelayed(() -> {
            setupAutocompleteTextWatcher(fragment, fragmentId);
        }, 200);
    }

    private RouteStop findStopByFragmentId(String fragmentId) {
        for (RouteStop stop : routeStops) {
            if (fragmentId.equals(stop.getAutocompleteFragmentId())) {
                return stop;
            }
        }
        return null;
    }

    private void removeStopByFragmentId(String fragmentId) {
        RouteStop stopToRemove = findStopByFragmentId(fragmentId);
        if (stopToRemove != null) {
            routeStops.remove(stopToRemove);

            // Reorder remaining stops
            reorderStops();
            updateMapMarkers();
            validateFormCompleteness();

            Toast.makeText(this, "Location cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void reorderStops() {
        // Sort by order index first
        routeStops.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));

        // Reassign order indices sequentially
        for (int i = 0; i < routeStops.size(); i++) {
            routeStops.get(i).orderIndex = i;
        }
    }

    private void removeLastMiddleStop() {
        if (middleStopFragments.isEmpty()) return;

        isClearingAutocomplete = true;

        // Get the last fragment
        AutocompleteSupportFragment lastFragment = middleStopFragments.get(middleStopFragments.size() - 1);

        // Find the fragment ID
        String fragmentId = null;
        for (Map.Entry<String, AutocompleteSupportFragment> entry : autocompleteFragments.entrySet()) {
            if (entry.getValue() == lastFragment && entry.getKey().startsWith("middle_")) {
                fragmentId = entry.getKey();
                break;
            }
        }

        if (fragmentId != null) {
            // Remove the stop associated with this fragment
            removeStopByFragmentId(fragmentId);
        }

        // Remove fragment
        try {
            getSupportFragmentManager().beginTransaction()
                    .remove(lastFragment)
                    .commitNow();
        } catch (Exception e) {
            Log.e(TAG, "Error removing fragment: " + e.getMessage());
            // Try commit instead
            getSupportFragmentManager().beginTransaction()
                    .remove(lastFragment)
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
        }

        // Remove from lists
        middleStopFragments.remove(lastFragment);

        // Remove from autocompleteFragments map
        if (fragmentId != null) {
            autocompleteFragments.remove(fragmentId);
        }

        // Remove the layout
        if (middleStopsContainer.getChildCount() > 0) {
            middleStopsContainer.removeViewAt(middleStopsContainer.getChildCount() - 1);
        }

        if (middleStopFragments.isEmpty()) {
            btnRemoveStop.setVisibility(View.GONE);
        }

        isClearingAutocomplete = false;

        updateMapMarkers();
        validateFormCompleteness();

        Toast.makeText(this, "Middle stop removed", Toast.LENGTH_SHORT).show();
    }

    private void updateMapMarkers() {
        if (mMap == null) return;

        // Clear all markers
        mMap.clear();
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        // Sort stops by order
        routeStops.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));

        // Add markers for each stop
        for (RouteStop stop : routeStops) {
            float color;
            String title;

            if (stop.getOrderIndex() == 0) {
                color = BitmapDescriptorFactory.HUE_GREEN;
                title = "Start: " + stop.getAddress();
            } else if (stop.getOrderIndex() == routeStops.size() - 1) {
                color = BitmapDescriptorFactory.HUE_RED;
                title = "End: " + stop.getAddress();
            } else {
                color = BitmapDescriptorFactory.HUE_ORANGE;
                title = "Stop " + stop.getOrderIndex() + ": " + stop.getAddress();
            }

            mMap.addMarker(new MarkerOptions()
                    .position(stop.getLatLng())
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
        }

        // Calculate route if we have at least 2 stops
        if (routeStops.size() >= 2) {
            calculateMultiStopRoute();
            fitMapToAllStops();
        } else {
            cardRoutePreview.setVisibility(View.GONE);
            layoutTrafficInfo.setVisibility(View.GONE);
            layoutWeatherInfo.setVisibility(View.GONE);
        }
    }

    private void calculateMultiStopRoute() {
        if (routeStops.size() < 2) return;

        cardRoutePreview.setVisibility(View.VISIBLE);
        tvDistance.setText("Calculating...");
        tvDuration.setText("...");

        new Thread(() -> {
            try {
                // Build waypoints string
                StringBuilder waypointsBuilder = new StringBuilder();
                for (int i = 1; i < routeStops.size() - 1; i++) {
                    if (i > 1) waypointsBuilder.append("|");
                    LatLng point = routeStops.get(i).getLatLng();
                    waypointsBuilder.append(point.latitude).append(",").append(point.longitude);
                }

                String origin = routeStops.get(0).getLatLng().latitude + "," +
                        routeStops.get(0).getLatLng().longitude;
                String destination = routeStops.get(routeStops.size() - 1).getLatLng().latitude + "," +
                        routeStops.get(routeStops.size() - 1).getLatLng().longitude;

                String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin +
                        "&destination=" + destination +
                        (waypointsBuilder.length() > 0 ? "&waypoints=" + waypointsBuilder.toString() : "") +
                        "&mode=driving" +
                        "&departure_time=now" +
                        "&traffic_model=best_guess" +
                        "&key=" + GOOGLE_MAPS_API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                parseDirectionsResponse(jsonResponse);

                // Fetch weather after route calculation
                fetchWeatherData(routeStops.get(0).getLatLng());

            } catch (Exception e) {
                Log.e(TAG, "Error fetching multi-stop directions", e);
                runOnUiThread(() -> {
                    routeDistance = calculateTotalRouteDistance();
                    simulateTrafficAndRouteCalculation();
                });
            }
        }).start();
    }

    private double calculateTotalRouteDistance() {
        double total = 0;
        for (int i = 0; i < routeStops.size() - 1; i++) {
            total += calculateHaversineDistance(
                    routeStops.get(i).getLatLng(),
                    routeStops.get(i + 1).getLatLng()
            );
        }
        return total;
    }

    private void fitMapToAllStops() {
        if (routeStops.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (RouteStop stop : routeStops) {
            builder.include(stop.getLatLng());
        }

        try {
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            Log.e(TAG, "Error fitting map to stops", e);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }

        LatLng dhaka = new LatLng(23.8103, 90.4125);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dhaka, 12));
    }

    private void parseDirectionsResponse(JSONObject jsonResponse) {
        try {
            JSONArray routes = jsonResponse.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");

                double totalDistance = 0;
                double totalDuration = 0;
                double totalTrafficDuration = 0;

                for (int i = 0; i < legs.length(); i++) {
                    JSONObject leg = legs.getJSONObject(i);

                    JSONObject distance = leg.getJSONObject("distance");
                    totalDistance += distance.getDouble("value") / 1000.0;

                    JSONObject duration = leg.getJSONObject("duration");
                    totalDuration += duration.getDouble("value") / 60.0;

                    if (leg.has("duration_in_traffic")) {
                        JSONObject durationInTraffic = leg.getJSONObject("duration_in_traffic");
                        totalTrafficDuration += durationInTraffic.getDouble("value") / 60.0;
                    } else {
                        totalTrafficDuration += (duration.getDouble("value") / 60.0) * getTrafficMultiplier();
                    }
                }

                routeDistance = totalDistance;
                routeDuration = totalDuration;
                trafficDuration = totalTrafficDuration;

                JSONObject polyline = route.getJSONObject("overview_polyline");
                String encodedPolyline = polyline.getString("points");

                runOnUiThread(() -> {
                    updateRouteUI();
                    drawEncodedPolyline(encodedPolyline);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing directions", e);
            runOnUiThread(() -> {
                routeDistance = calculateTotalRouteDistance();
                simulateTrafficAndRouteCalculation();
            });
        }
    }

    private void simulateTrafficAndRouteCalculation() {
        routeDuration = routeDistance * 2;
        trafficDuration = routeDuration * getTrafficMultiplier();
        updateRouteUI();
        drawStraightLineBetweenStops();
    }

    private void drawStraightLineBetweenStops() {
        if (mMap == null || routeStops.size() < 2) return;

        if (routePolyline != null) {
            routePolyline.remove();
        }

        List<LatLng> points = new ArrayList<>();
        for (RouteStop stop : routeStops) {
            points.add(stop.getLatLng());
        }

        routePolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(8)
                .color(getResources().getColor(android.R.color.holo_blue_dark))
                .geodesic(true));
    }

    private void updateRouteUI() {
        tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", routeDistance));
        tvDuration.setText(String.format(Locale.getDefault(), "%.0f min", trafficDuration));

        layoutTrafficInfo.setVisibility(View.VISIBLE);
        String trafficLevel = getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY));
        tvTrafficInfo.setText("Traffic: " + trafficLevel);
    }

    private void drawEncodedPolyline(String encodedPolyline) {
        if (mMap == null) return;

        if (routePolyline != null) {
            routePolyline.remove();
        }

        List<LatLng> points = decodePolyline(encodedPolyline);
        routePolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(8)
                .color(getResources().getColor(android.R.color.holo_blue_dark))
                .geodesic(true));

        fitMapToRoute();
    }

    private void fitMapToRoute() {
        if (routeStops.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (RouteStop stop : routeStops) {
            builder.include(stop.getLatLng());
        }
        LatLngBounds bounds = builder.build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    selectedTime.set(Calendar.SECOND, 0);
                    selectedTime.set(Calendar.MILLISECOND, 0);

                    Calendar maxTime = Calendar.getInstance();
                    maxTime.add(Calendar.HOUR_OF_DAY, 1);

                    if (selectedTime.before(now)) {
                        Toast.makeText(this,
                                "❌ Departure time cannot be in the past.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (selectedTime.after(maxTime)) {
                        Toast.makeText(this,
                                "❌ Departure time cannot be more than 1 hour from now.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    calendar.setTimeInMillis(selectedTime.getTimeInMillis());
                    updateTimeButton();
                    validateFormCompleteness();

                    Toast.makeText(this,
                            "✅ Departure time set successfully",
                            Toast.LENGTH_SHORT).show();
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.setTitle("Select Departure Time (Max 1 hour from now)");
        timePickerDialog.show();
    }

    private void updateTimeButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        tvDepartureTime.setText(sdf.format(calendar.getTime()));
    }

    private void validateFormCompleteness() {
        boolean pickupSelected = !routeStops.isEmpty() && routeStops.get(0).getOrderIndex() == 0;
        boolean dropSelected = routeStops.size() >= 2;
        boolean fareEntered = !etTotalFare.getText().toString().trim().isEmpty();
        boolean timeSelected = !tvDepartureTime.getText().toString().equals("Tap to select date & time");

        boolean isComplete = pickupSelected && dropSelected && fareEntered && timeSelected;

        btnCheckFare.setEnabled(isComplete);
        btnCheckFare.setBackgroundColor(isComplete ?
                getResources().getColor(android.R.color.holo_blue_dark) :
                getResources().getColor(android.R.color.darker_gray));
    }

    private void fetchWeatherData(LatLng location) {
        new Thread(() -> {
            try {
                String urlString = OPENWEATHER_BASE_URL + "?" +
                        "lat=" + location.latitude +
                        "&lon=" + location.longitude +
                        "&appid=" + OPENWEATHER_API_KEY +
                        "&units=metric";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                parseWeatherResponse(jsonResponse);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching weather data", e);
                runOnUiThread(() -> {
                    currentWeather = "Clear";
                    weatherDescription = "Weather data unavailable";
                    temperature = 25.0;
                    weatherMultiplier = 1.0;

                    tvWeatherInfo.setText("Weather: Data unavailable");
                    layoutWeatherInfo.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void parseWeatherResponse(JSONObject jsonResponse) {
        try {
            JSONArray weatherArray = jsonResponse.getJSONArray("weather");
            if (weatherArray.length() > 0) {
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                currentWeather = weatherObj.getString("main");
                weatherDescription = weatherObj.getString("description");
            }

            JSONObject mainObj = jsonResponse.getJSONObject("main");
            temperature = mainObj.getDouble("temp");

            weatherMultiplier = calculateWeatherMultiplier(currentWeather, temperature);

            runOnUiThread(() -> {
                updateWeatherUI();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error parsing weather data", e);
            runOnUiThread(() -> {
                currentWeather = "Clear";
                weatherDescription = "Weather data unavailable";
                temperature = 25.0;
                weatherMultiplier = 1.0;

                tvWeatherInfo.setText("Weather: Data unavailable");
                layoutWeatherInfo.setVisibility(View.VISIBLE);
            });
        }
    }

    private double calculateWeatherMultiplier(String weatherCondition, double temperature) {
        switch (weatherCondition.toLowerCase()) {
            case "thunderstorm":
                return 1.10;
            case "rain":
            case "drizzle":
                return 1.08;
            case "snow":
                return 1.15;
            case "mist":
            case "fog":
            case "haze":
                return 1.05;
            case "clouds":
                return 1.02;
            case "extreme":
                return 1.12;
            case "clear":
            default:
                if (temperature > 35) {
                    return 1.04;
                } else if (temperature < 10) {
                    return 1.04;
                }
                return 1.0;
        }
    }

    private void updateWeatherUI() {
        String weatherEmoji = getWeatherEmoji(currentWeather);
        String tempString = String.format(Locale.getDefault(), "%.0f°C", temperature);

        String weatherText = String.format(Locale.getDefault(),
                "%s %s | %s | Temp: %s",
                weatherEmoji, currentWeather, weatherDescription, tempString);

        tvWeatherInfo.setText(weatherText);
        layoutWeatherInfo.setVisibility(View.VISIBLE);

        if (weatherMultiplier > 1.0) {
            String impactText = String.format(Locale.getDefault(),
                    "Note: Weather increases fare by %.0f%%",
                    (weatherMultiplier - 1.0) * 100);
            Toast.makeText(this, impactText, Toast.LENGTH_LONG).show();
        }
    }

    private String getWeatherEmoji(String weatherCondition) {
        switch (weatherCondition.toLowerCase()) {
            case "thunderstorm": return "⛈️";
            case "rain": case "drizzle": return "🌧️";
            case "snow": return "❄️";
            case "mist": case "fog": case "haze": return "🌫️";
            case "clouds": return "☁️";
            case "clear": return "☀️";
            case "extreme": return "⚠️";
            default: return "🌤️";
        }
    }

    private void checkFareFairness() {
        btnCheckFare.setText("Analyzing...");
        btnCheckFare.setEnabled(false);
        layoutFareAnalysis.setVisibility(View.VISIBLE);

        tvFairnessMessage.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvFairnessMessage.setText("🔍 Analyzing fare fairness...");
        tvDetailedReason.setVisibility(View.VISIBLE);
        tvSuggestion.setVisibility(View.GONE);

        new Thread(() -> {
            calculateFareFairness();
            runOnUiThread(() -> resetCheckFareButton());
        }).start();
    }

    private void calculateFareFairness() {
        if (routeStops.size() < 2) {
            runOnUiThread(() -> showFareResult("Please select valid locations", false, ""));
            return;
        }

        double userFare;
        try {
            userFare = Double.parseDouble(etTotalFare.getText().toString().trim());
        } catch (NumberFormatException e) {
            runOnUiThread(() -> showFareResult("Please enter a valid fare amount", false, ""));
            return;
        }

        calculateFairFare(routeDistance);
        storeFareBreakdown();

        final double finalUserFare = userFare;
        runOnUiThread(() -> {
            if (finalUserFare >= minFairFare && finalUserFare <= maxFairFare) {
                // Calculate per passenger fare
                farePerPassenger = finalUserFare / seatsAvailable;
                farePerPassenger = Math.round(farePerPassenger / 5) * 5;

                String reason = getFareFairReason(finalUserFare);
                showFareResult("✓ Fare is fair! Per passenger: ৳" +
                        String.format("%.0f", farePerPassenger), true, reason);
                enablePostButton();
            } else {
                String reason = getFareUnfairReason(finalUserFare, routeDistance);
                String suggestion = getFareSuggestion(finalUserFare);
                showFareResult("Fare needs adjustment", false, reason, suggestion);
                disablePostButton();
            }

            String infoText = String.format(Locale.getDefault(),
                    "Distance: %.1f km • Seats: %d • Traffic: %s • Weather: %s",
                    routeDistance, seatsAvailable,
                    getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)),
                    currentWeather);

            if (weatherMultiplier > 1.0) {
                infoText += String.format(Locale.getDefault(),
                        " (+%.0f%%)", (weatherMultiplier - 1.0) * 100);
            }

            tvFairRange.setText(infoText);
        });
    }

    private void calculateFairFare(double distance) {
        // OFFICIAL BANGLADESH GOVERNMENT RATES for 1500cc AC Taxis
        double first2KmFare = 85.0;
        double perKmRate = 34.0;
        double waitingPer2Min = 8.5;
        double callServiceFee = 20.0;

        // Calculate official AC car fare
        if (distance <= 2.0) {
            calculatedTotalFare = first2KmFare;
        } else {
            double extraKm = distance - 2.0;
            double extraFare = extraKm * perKmRate;
            calculatedTotalFare = first2KmFare + extraFare;
        }

        // Apply minimum charge
        double officialMinimum = 85.0;
        calculatedTotalFare = Math.max(calculatedTotalFare, officialMinimum);

        // Add waiting time charges
        double estimatedWaitingMinutes = Math.max(0, (trafficDuration - routeDuration) * 0.7);
        if (estimatedWaitingMinutes > 5) {
            double waitingCharges = Math.ceil(estimatedWaitingMinutes / 3.0) * 8.5;
            calculatedTotalFare += waitingCharges;
        }

        // Add call service fee
        calculatedTotalFare += callServiceFee;

        // Apply fairness adjustments for AC Car
        calculatedTotalFare *= getACCarTimeMultiplier();
        calculatedTotalFare *= getACCarWeatherMultiplier();

        // Traffic delay compensation
        double trafficDelayFactor = Math.max(1.0, trafficDuration / Math.max(routeDuration, 1.0));
        if (trafficDelayFactor > 1.3) {
            calculatedTotalFare *= Math.min(trafficDelayFactor, 1.12);
        }

        // Set fair range for AC Car (±10% or ৳40, whichever is higher)
        double range = Math.max(40, calculatedTotalFare * 0.10);
        minFairFare = Math.max(calculatedTotalFare - range, 105.0);
        maxFairFare = calculatedTotalFare + range;

        // Round to nearest 5 taka
        minFairFare = Math.round(minFairFare / 5) * 5;
        maxFairFare = Math.round(maxFairFare / 5) * 5;
        calculatedTotalFare = Math.round(calculatedTotalFare / 5) * 5;
    }

    private double getACCarTimeMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            return 1.05;
        }

        if ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19)) {
            return 1.08;
        }

        if (hour >= 22 || hour <= 6) {
            return 1.10;
        }

        return 1.0;
    }

    private double getACCarWeatherMultiplier() {
        return Math.min(weatherMultiplier, 1.06);
    }

    private void storeFareBreakdown() {
        StringBuilder breakdown = new StringBuilder();

        breakdown.append("🚗 Carpool (AC Car) - Official Rate Based\n");
        breakdown.append("═══════════════════════════════════\n");
        breakdown.append("Official Bangladesh Government Rates:\n");

        if (routeDistance <= 2.0) {
            breakdown.append("• First 2 km: ৳85.00\n");
        } else {
            breakdown.append("• First 2 km: ৳85.00\n");
            breakdown.append(String.format("• Additional %.1f km × ৳34/km: ৳%.0f\n",
                    routeDistance - 2.0, (routeDistance - 2.0) * 34.0));
        }

        double estimatedWaitingMinutes = Math.max(0, trafficDuration - routeDuration);
        if (estimatedWaitingMinutes > 0) {
            breakdown.append(String.format("• Waiting (%.0f min): ৳%.0f\n",
                    estimatedWaitingMinutes, Math.ceil(estimatedWaitingMinutes / 2.0) * 8.5));
        }

        breakdown.append("• App booking: ৳20.00\n");

        // Show adjustments
        breakdown.append("\nFairness Adjustments:\n");
        double timeMult = getACCarTimeMultiplier();
        double weatherMult = getACCarWeatherMultiplier();

        if (timeMult > 1.0) {
            breakdown.append(String.format("• %s: +%.0f%%\n", getTimeDescription(), (timeMult - 1.0) * 100));
        }
        if (weatherMult > 1.0) {
            breakdown.append(String.format("• %s weather: +%.0f%%\n", currentWeather, (weatherMult - 1.0) * 100));
        }

        breakdown.append(String.format("\n• Total Seats: %d", seatsAvailable));
        breakdown.append(String.format("\n• Per Passenger Fare: ৳%.0f", calculatedTotalFare / seatsAvailable));
        breakdown.append(String.format("\n\n✅ Fair Total Fare Range: ৳%.0f - ৳%.0f", minFairFare, maxFairFare));
        breakdown.append(String.format("\n💡 Suggested Total: ৳%.0f", calculatedTotalFare));

        runOnUiThread(() -> {
            tvDetailedReason.setText(breakdown.toString());
            tvDetailedReason.setVisibility(View.VISIBLE);
        });
    }

    private String getTimeDescription() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            return "Weekend";
        }

        if ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19)) {
            return "Peak hours";
        }

        if (hour >= 22 || hour <= 6) {
            return "Late night";
        }

        return "Normal hours";
    }

    private String getFareFairReason(double userFare) {
        double perPassenger = userFare / seatsAvailable;

        String reason = String.format(Locale.getDefault(),
                "Your fare is appropriate for:\n• %.1f km distance\n• %d passengers\n• %s\n• %s traffic",
                routeDistance, seatsAvailable, getTimeOfDayDescription(),
                getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));

        if (weatherMultiplier > 1.0) {
            reason += String.format(Locale.getDefault(),
                    "\n• %s weather (increased fare by %.0f%%)",
                    currentWeather, (weatherMultiplier - 1.0) * 100);
        }

        reason += String.format(Locale.getDefault(),
                "\n\n💰 Total: ৳%.0f | Per passenger: ৳%.0f",
                userFare, perPassenger);

        return reason;
    }

    private String getFareUnfairReason(double userFare, double distance) {
        if (userFare < minFairFare) {
            return String.format(Locale.getDefault(),
                    "Your total fare is below the fair market rate for:\n• Distance: %.1f km\n• %d passengers\n• Time: %s",
                    distance, seatsAvailable, getTimeOfDayDescription());
        } else {
            return String.format(Locale.getDefault(),
                    "Your total fare is above what passengers typically expect for:\n• Distance: %.1f km\n• %d passengers\n• This might reduce interest",
                    distance, seatsAvailable);
        }
    }

    private String getFareSuggestion(double userFare) {
        if (userFare < minFairFare) {
            return "💡 Consider a higher total fare to cover your costs";
        } else {
            return "💡 Consider a lower total fare to attract more passengers";
        }
    }

    private void showFareResult(String message, boolean isSuccess, String detailedReason, String... suggestion) {
        tvFairnessMessage.setText(message);
        tvFairnessMessage.setTextColor(isSuccess ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));

        tvDetailedReason.setText(detailedReason);
        tvDetailedReason.setVisibility(View.VISIBLE);

        if (suggestion.length > 0) {
            tvSuggestion.setText(suggestion[0]);
            tvSuggestion.setVisibility(View.VISIBLE);
        } else {
            tvSuggestion.setVisibility(View.GONE);
        }
    }

    private void enablePostButton() {
        btnPostCarpool.setEnabled(true);
        btnPostCarpool.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    private void disablePostButton() {
        btnPostCarpool.setEnabled(false);
        btnPostCarpool.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }

    private void resetCheckFareButton() {
        btnCheckFare.setText("Validate Price");
        btnCheckFare.setEnabled(true);
    }

    private void postCarpool() {
        if (!validateForm()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to post", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPostCarpool.setText("Posting...");
        btnPostCarpool.setEnabled(false);

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> carpoolRequest = createCarpoolRequestData(currentUser, documentSnapshot);

                    db.collection("ride_requests")
                            .add(carpoolRequest)
                            .addOnSuccessListener(documentReference -> {
                                long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

                                // Schedule auto-deletion if not accepted by departure time
                                if (delay > 0) {
                                    new android.os.Handler().postDelayed(() -> {
                                        db.collection("ride_requests")
                                                .document(documentReference.getId())
                                                .get()
                                                .addOnSuccessListener(doc -> {
                                                    if (doc.exists() && "pending".equals(doc.getString("status"))) {
                                                        documentReference.delete()
                                                                .addOnSuccessListener(aVoid -> {
                                                                    Log.d(TAG, "Auto-deleted expired pending carpool: " + documentReference.getId());
                                                                });
                                                    }
                                                });
                                    }, delay);
                                } else {
                                    // If departure time is in the past, check and delete immediately
                                    db.collection("ride_requests")
                                            .document(documentReference.getId())
                                            .get()
                                            .addOnSuccessListener(doc -> {
                                                if (doc.exists() && "pending".equals(doc.getString("status"))) {
                                                    documentReference.delete();
                                                }
                                            });
                                }

                                Toast.makeText(this, "✅ Carpool posted successfully!", Toast.LENGTH_SHORT).show();
                                clearForm();

                                new android.os.Handler().postDelayed(() -> {
                                    Intent intent = new Intent(CarpoolPostActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }, 2000);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnPostCarpool.setText("Post Carpool");
                                btnPostCarpool.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    btnPostCarpool.setText("Post Carpool");
                    btnPostCarpool.setEnabled(true);
                });
    }

    private Map<String, Object> createCarpoolRequestData(FirebaseUser currentUser,
                                                         com.google.firebase.firestore.DocumentSnapshot userDoc) {
        Map<String, Object> carpoolRequest = new HashMap<>();

        // Mark as carpool
        carpoolRequest.put("type", "carpool");
        carpoolRequest.put("isDriverPost", true);

        // Driver info
        carpoolRequest.put("driverId", currentUser.getUid());
        carpoolRequest.put("driverName", userDoc.exists() ? userDoc.getString("fullName") : "Anonymous");
        carpoolRequest.put("driverPhone", userDoc.exists() ? userDoc.getString("phone") : "");
        carpoolRequest.put("driverPhoto", userDoc.exists() ? userDoc.getString("profileImageUrl") : "");
        carpoolRequest.put("driverRating", 4.5);

        // Trip details
        if (!routeStops.isEmpty()) {
            RouteStop firstStop = routeStops.get(0);
            RouteStop lastStop = routeStops.get(routeStops.size() - 1);

            carpoolRequest.put("pickupLocation", firstStop.getAddress());
            carpoolRequest.put("dropLocation", lastStop.getAddress());
            carpoolRequest.put("pickupLat", firstStop.getLatLng().latitude);
            carpoolRequest.put("pickupLng", firstStop.getLatLng().longitude);
            carpoolRequest.put("dropLat", lastStop.getLatLng().latitude);
            carpoolRequest.put("dropLng", lastStop.getLatLng().longitude);
        }

        // Store all route stops
        List<Map<String, Object>> stopsData = new ArrayList<>();
        for (RouteStop stop : routeStops) {
            Map<String, Object> stopData = new HashMap<>();
            stopData.put("address", stop.getAddress());
            stopData.put("lat", stop.getLatLng().latitude);
            stopData.put("lng", stop.getLatLng().longitude);
            stopData.put("orderIndex", stop.getOrderIndex());
            stopsData.add(stopData);
        }
        carpoolRequest.put("routeStops", stopsData);

        // Fare and vehicle
        double totalFare = Double.parseDouble(etTotalFare.getText().toString().trim());
        carpoolRequest.put("fare", totalFare);
        carpoolRequest.put("totalFare", totalFare);
        carpoolRequest.put("farePerPassenger", farePerPassenger);
        carpoolRequest.put("vehicleType", vehicleType);

        // Carpool-specific
        carpoolRequest.put("seatsAvailable", seatsAvailable);
        carpoolRequest.put("maxSeats", seatsAvailable);
        carpoolRequest.put("passengerIds", new ArrayList<String>());
        carpoolRequest.put("passengerNames", new ArrayList<String>());
        carpoolRequest.put("passengerCount", 0);

        // Timing
        carpoolRequest.put("departureTime", calendar.getTimeInMillis());
        carpoolRequest.put("createdAt", System.currentTimeMillis());

        // Weather info
        carpoolRequest.put("weatherCondition", currentWeather);
        carpoolRequest.put("weatherDescription", weatherDescription);
        carpoolRequest.put("temperature", temperature);
        carpoolRequest.put("weatherMultiplier", weatherMultiplier);

        // Additional info
        carpoolRequest.put("specialRequest", etSpecialRequest.getText().toString().trim());
        carpoolRequest.put("status", "pending");
        carpoolRequest.put("calculatedFairFare", calculatedTotalFare);
        carpoolRequest.put("minFairFare", minFairFare);
        carpoolRequest.put("maxFairFare", maxFairFare);
        carpoolRequest.put("distance", routeDistance);
        carpoolRequest.put("duration", trafficDuration);
        carpoolRequest.put("isFareFair", true);
        carpoolRequest.put("trafficLevel", getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));

        return carpoolRequest;
    }

    private boolean validateForm() {
        if (routeStops.size() < 2) {
            Toast.makeText(this, "Select at least pickup and drop locations", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etTotalFare.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Enter total fare amount", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void clearForm() {
        isClearingAutocomplete = true;

        // Clear all autocomplete fragments
        if (autocompleteStart != null && autocompleteStart.getView() != null) {
            EditText editText = autocompleteStart.getView().findViewById(
                    com.google.android.libraries.places.R.id.places_autocomplete_search_input);
            if (editText != null) {
                editText.setText("");
            }
        }

        if (autocompleteEnd != null && autocompleteEnd.getView() != null) {
            EditText editText = autocompleteEnd.getView().findViewById(
                    com.google.android.libraries.places.R.id.places_autocomplete_search_input);
            if (editText != null) {
                editText.setText("");
            }
        }

        // Clear middle stops
        for (AutocompleteSupportFragment fragment : middleStopFragments) {
            if (fragment.getView() != null) {
                EditText editText = fragment.getView().findViewById(
                        com.google.android.libraries.places.R.id.places_autocomplete_search_input);
                if (editText != null) {
                    editText.setText("");
                }
                try {
                    getSupportFragmentManager().beginTransaction().remove(fragment).commitNow();
                } catch (Exception e) {
                    Log.e(TAG, "Error removing fragment: " + e.getMessage());
                }
            }
        }

        isClearingAutocomplete = false;

        // Clear all data
        middleStopFragments.clear();
        middleStopsContainer.removeAllViews();
        routeStops.clear();
        autocompleteFragments.clear();

        tvDepartureTime.setText("Tap to select date & time");
        etTotalFare.setText("");
        layoutFareAnalysis.setVisibility(View.GONE);
        seatsAvailable = 3;
        tvSeatsCount.setText("3");
        etSpecialRequest.setText("");

        if (mMap != null) mMap.clear();
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        cardRoutePreview.setVisibility(View.GONE);
        layoutTrafficInfo.setVisibility(View.GONE);
        layoutWeatherInfo.setVisibility(View.GONE);
        btnPostCarpool.setEnabled(false);
        btnPostCarpool.setText("Post Carpool");
        btnCheckFare.setEnabled(false);
        btnRemoveStop.setVisibility(View.GONE);

        currentWeather = "Clear";
        weatherDescription = "";
        weatherMultiplier = 1.0;
        temperature = 0;
    }

    private double calculateHaversineDistance(LatLng point1, LatLng point2) {
        double earthRadius = 6371;
        double dLat = Math.toRadians(point2.latitude - point1.latitude);
        double dLon = Math.toRadians(point2.longitude - point1.longitude);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.max(earthRadius * c, 1.0);
    }

    private String getTimeOfDayDescription() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 22 || hour <= 5) return "late night";
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 20)) return "peak hours";
        return "normal hours";
    }

    private String getTrafficLevel(int hour) {
        if ((hour >= 8 && hour <= 9) || (hour >= 17 && hour <= 18)) return "Heavy";
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19)) return "Moderate";
        return "Light";
    }

    private double getTrafficMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if ((hour >= 8 && hour <= 9) || (hour >= 17 && hour <= 18))
            return 1.08;
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19))
            return 1.04;
        return 1.0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            BottomNavigationHelper.setupBottomNavigation(this, "POST");
        } catch (Exception e) {
            Log.e(TAG, "Bottom navigation setup failed", e);
        }
    }
}
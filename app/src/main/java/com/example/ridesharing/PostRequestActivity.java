package com.example.ridesharing;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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

public class PostRequestActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PostRequestActivity";
    private static final String GOOGLE_MAPS_API_KEY = "AIzaSyCD-k7OlWsemXLHwBXyBoQNO8r9rxRc9nM";
    private static final String OPENWEATHER_API_KEY = "acc6e705fe0c4b7c67e77a98cfc26122"; // Add your key here
    private static final String OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    // UI Components
    private AutocompleteSupportFragment autocompletePickup, autocompleteDrop;
    private TextView tvDepartureTime, tvPassengersCount;
    private TextView tvDistance, tvDuration, tvTrafficInfo, tvWeatherInfo;
    private EditText etFare, etSpecialRequest;
    private Button btnCheckFare, btnPostRequest, btnDecreasePassengers, btnIncreasePassengers;
    private MaterialCardView cardRoutePreview;
    private LinearLayout layoutTrafficInfo, layoutFareAnalysis, layoutWeatherInfo;
    private TextView tvFairRange, tvFairnessMessage, tvDetailedReason, tvSuggestion;
    private RadioGroup radioGroupVehicleType;
    private RadioButton rbCar, rbBike;

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
    private String pickupAddress = "";
    private String dropAddress = "";
    private int passengersCount = 1;
    private double routeDistance = 0;
    private double routeDuration = 0;
    private double trafficDuration = 0;
    private String vehicleType = "car"; // default

    // Weather data
    private String currentWeather = "Clear";
    private String weatherDescription = "";
    private double weatherMultiplier = 1.0;
    private double temperature = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_request);

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
        etFare = findViewById(R.id.et_fare);
        btnCheckFare = findViewById(R.id.btn_check_fare);
        btnPostRequest = findViewById(R.id.btn_post_request);
        layoutFareAnalysis = findViewById(R.id.layout_fare_analysis);
        tvFairRange = findViewById(R.id.tv_fair_range);
        tvFairnessMessage = findViewById(R.id.tv_fairness_message);
        tvDetailedReason = findViewById(R.id.tv_detailed_reason);
        tvSuggestion = findViewById(R.id.tv_suggestion);
        tvPassengersCount = findViewById(R.id.tv_passengers_count);
        btnDecreasePassengers = findViewById(R.id.btn_decrease_passengers);
        btnIncreasePassengers = findViewById(R.id.btn_increase_passengers);
        etSpecialRequest = findViewById(R.id.et_special_request);

        // Vehicle type selection
        radioGroupVehicleType = findViewById(R.id.radio_group_vehicle_type);
        rbCar = findViewById(R.id.rb_car);
        rbBike = findViewById(R.id.rb_bike);

        tvPassengersCount.setText(String.valueOf(passengersCount));
        btnCheckFare.setEnabled(false);
        btnPostRequest.setEnabled(false);

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
        // Pickup Autocomplete
        autocompletePickup = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_pickup);

        if (autocompletePickup != null) {
            autocompletePickup.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            ));
            autocompletePickup.setHint("Pickup location");
            autocompletePickup.setCountries("BD");

            autocompletePickup.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    pickupLatLng = place.getLatLng();
                    pickupAddress = place.getAddress() != null ? place.getAddress() : place.getName();

                    if (pickupMarker != null) pickupMarker.remove();
                    pickupMarker = mMap.addMarker(new MarkerOptions()
                            .position(pickupLatLng)
                            .title("Pickup")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 15));

                    if (pickupLatLng != null && dropLatLng != null) {
                        calculateRouteWithDirectionsAPI();
                    }
                    validateFormCompleteness();
                }

                @Override
                public void onError(com.google.android.gms.common.api.Status status) {
                    Log.e(TAG, "Pickup error: " + status);
                    Toast.makeText(PostRequestActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Drop Autocomplete
        autocompleteDrop = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_drop);

        if (autocompleteDrop != null) {
            autocompleteDrop.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            ));
            autocompleteDrop.setHint("Drop-off location");
            autocompleteDrop.setCountries("BD");

            autocompleteDrop.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    dropLatLng = place.getLatLng();
                    dropAddress = place.getAddress() != null ? place.getAddress() : place.getName();

                    if (dropMarker != null) dropMarker.remove();
                    dropMarker = mMap.addMarker(new MarkerOptions()
                            .position(dropLatLng)
                            .title("Drop-off")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropLatLng, 15));

                    if (pickupLatLng != null && dropLatLng != null) {
                        calculateRouteWithDirectionsAPI();
                    }
                    validateFormCompleteness();
                }

                @Override
                public void onError(com.google.android.gms.common.api.Status status) {
                    Log.e(TAG, "Drop error: " + status);
                    Toast.makeText(PostRequestActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupClickListeners() {
        tvDepartureTime.setOnClickListener(v -> showDateTimePicker());
        btnCheckFare.setOnClickListener(v -> checkFareFairness());
        btnPostRequest.setOnClickListener(v -> postRideRequest());

        radioGroupVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_car) {
                vehicleType = "car";
                btnIncreasePassengers.setEnabled(true);
            } else if (checkedId == R.id.rb_bike) {
                vehicleType = "bike";
                passengersCount = 1;
                tvPassengersCount.setText("1");
                btnIncreasePassengers.setEnabled(false);
            }
            validateFormCompleteness();
        });

        btnDecreasePassengers.setOnClickListener(v -> {
            if (passengersCount > 1) {
                passengersCount--;
                tvPassengersCount.setText(String.valueOf(passengersCount));
                validateFormCompleteness();
            }
        });

        btnIncreasePassengers.setOnClickListener(v -> {
            if (vehicleType.equals("car") && passengersCount < 4) {
                passengersCount++;
                tvPassengersCount.setText(String.valueOf(passengersCount));
                validateFormCompleteness();
            }
        });

        etFare.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                validateFormCompleteness();
            }
        });
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

    private void calculateRouteWithDirectionsAPI() {
        if (pickupLatLng == null || dropLatLng == null) return;

        cardRoutePreview.setVisibility(View.VISIBLE);
        tvDistance.setText("Calculating...");
        tvDuration.setText("...");

        new Thread(() -> {
            try {
                String origin = pickupLatLng.latitude + "," + pickupLatLng.longitude;
                String destination = dropLatLng.latitude + "," + dropLatLng.longitude;

                String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin +
                        "&destination=" + destination +
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
                fetchWeatherData(pickupLatLng);

            } catch (Exception e) {
                Log.e(TAG, "Error fetching directions", e);
                runOnUiThread(() -> {
                    routeDistance = calculateHaversineDistance(pickupLatLng, dropLatLng);
                    simulateTrafficAndRouteCalculation();
                });
            }
        }).start();
    }

    private void fetchWeatherData(LatLng location) {
        new Thread(() -> {
            try {
                String urlString = OPENWEATHER_BASE_URL + "?" +
                        "lat=" + location.latitude +
                        "&lon=" + location.longitude +
                        "&appid=" + OPENWEATHER_API_KEY +
                        "&units=metric"; // For Celsius temperature

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
                    // Default weather if API fails
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
            // Get main weather
            JSONArray weatherArray = jsonResponse.getJSONArray("weather");
            if (weatherArray.length() > 0) {
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                currentWeather = weatherObj.getString("main");
                weatherDescription = weatherObj.getString("description");
            }

            // Get temperature
            JSONObject mainObj = jsonResponse.getJSONObject("main");
            temperature = mainObj.getDouble("temp");

            // Calculate weather multiplier based on conditions
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
                return 1.25; // 25% increase for thunderstorms
            case "rain":
            case "drizzle":
                return 1.2; // 20% increase for rain
            case "snow":
                return 1.3; // 30% increase for snow
            case "mist":
            case "fog":
            case "haze":
                return 1.15; // 15% increase for poor visibility
            case "clouds":
                return 1.05; // 5% increase for cloudy
            case "extreme": // Extreme conditions
                return 1.4; // 40% increase
            case "clear":
            default:
                // Check temperature extremes
                if (temperature > 35) { // Very hot
                    return 1.1;
                } else if (temperature < 10) { // Very cold
                    return 1.1;
                }
                return 1.0; // Normal weather
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

        // Show weather impact on fare
        if (weatherMultiplier > 1.0) {
            String impactText = String.format(Locale.getDefault(),
                    "Note: Weather increases fare by %.0f%%",
                    (weatherMultiplier - 1.0) * 100);
            Toast.makeText(this, impactText, Toast.LENGTH_LONG).show();
        }
    }

    private String getWeatherEmoji(String weatherCondition) {
        switch (weatherCondition.toLowerCase()) {
            case "thunderstorm":
                return "⛈️";
            case "rain":
            case "drizzle":
                return "🌧️";
            case "snow":
                return "❄️";
            case "mist":
            case "fog":
            case "haze":
                return "🌫️";
            case "clouds":
                return "☁️";
            case "clear":
                return "☀️";
            case "extreme":
                return "⚠️";
            default:
                return "🌤️";
        }
    }

    private void parseDirectionsResponse(JSONObject jsonResponse) {
        try {
            JSONArray routes = jsonResponse.getJSONArray("routes");
            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");

                if (legs.length() > 0) {
                    JSONObject leg = legs.getJSONObject(0);

                    JSONObject distance = leg.getJSONObject("distance");
                    routeDistance = distance.getDouble("value") / 1000.0;

                    JSONObject duration = leg.getJSONObject("duration");
                    routeDuration = duration.getDouble("value") / 60.0;

                    if (leg.has("duration_in_traffic")) {
                        JSONObject durationInTraffic = leg.getJSONObject("duration_in_traffic");
                        trafficDuration = durationInTraffic.getDouble("value") / 60.0;
                    } else {
                        trafficDuration = routeDuration * getTrafficMultiplier();
                    }

                    JSONObject polyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = polyline.getString("points");

                    runOnUiThread(() -> {
                        updateRouteUI();
                        drawEncodedPolyline(encodedPolyline);
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing directions", e);
            runOnUiThread(() -> {
                routeDistance = calculateHaversineDistance(pickupLatLng, dropLatLng);
                simulateTrafficAndRouteCalculation();
            });
        }
    }

    private void simulateTrafficAndRouteCalculation() {
        routeDuration = routeDistance * 2;
        trafficDuration = routeDuration * getTrafficMultiplier();
        updateRouteUI();
        drawStraightLine();
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

    private void drawStraightLine() {
        if (mMap == null || pickupLatLng == null || dropLatLng == null) return;

        if (routePolyline != null) {
            routePolyline.remove();
        }

        routePolyline = mMap.addPolyline(new PolylineOptions()
                .add(pickupLatLng, dropLatLng)
                .width(8)
                .color(getResources().getColor(android.R.color.holo_blue_dark))
                .geodesic(true));

        fitMapToRoute();
    }

    private void fitMapToRoute() {
        if (pickupLatLng == null || dropLatLng == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(dropLatLng);
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
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        tvDepartureTime.setText(sdf.format(calendar.getTime()));
    }

    private void validateFormCompleteness() {
        boolean pickupSelected = pickupLatLng != null;
        boolean dropSelected = dropLatLng != null;
        boolean fareEntered = !etFare.getText().toString().trim().isEmpty();
        boolean timeSelected = !tvDepartureTime.getText().toString().equals("Tap to select date & time");
        boolean vehicleSelected = radioGroupVehicleType.getCheckedRadioButtonId() != -1;

        boolean isComplete = pickupSelected && dropSelected && fareEntered && timeSelected && vehicleSelected;

        btnCheckFare.setEnabled(isComplete);
        btnCheckFare.setBackgroundColor(isComplete ?
                getResources().getColor(android.R.color.holo_blue_dark) :
                getResources().getColor(android.R.color.darker_gray));
    }

    private void checkFareFairness() {
        btnCheckFare.setText("Calculating...");
        btnCheckFare.setEnabled(false);
        layoutFareAnalysis.setVisibility(View.VISIBLE);

        new Thread(() -> {
            calculateFareFairness();
            runOnUiThread(this::resetCheckFareButton);
        }).start();
    }

    private void calculateFareFairness() {
        if (pickupLatLng == null || dropLatLng == null) {
            runOnUiThread(() -> showFareResult("Please select valid locations", false, ""));
            return;
        }

        double userFare;
        try {
            userFare = Double.parseDouble(etFare.getText().toString().trim());
        } catch (NumberFormatException e) {
            runOnUiThread(() -> showFareResult("Please enter a valid fare amount", false, ""));
            return;
        }

        calculateFairFare(routeDistance, passengersCount);

        final double finalUserFare = userFare;
        runOnUiThread(() -> {
            if (finalUserFare >= minFairFare && finalUserFare <= maxFairFare) {
                String reason = getFareFairReason(finalUserFare);
                showFareResult("✓ Fare is fair! You can post your request.", true, reason);
                enablePostButton();
            } else {
                String reason = getFareUnfairReason(finalUserFare, routeDistance);
                String suggestion = getFareSuggestion(finalUserFare);
                showFareResult("Fare needs adjustment", false, reason, suggestion);
                disablePostButton();
            }

            // Show distance, traffic and weather info
            String infoText = String.format(Locale.getDefault(),
                    "Distance: %.1f km • Traffic: %s • Weather: %s",
                    routeDistance, getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)),
                    currentWeather);

            if (weatherMultiplier > 1.0) {
                infoText += String.format(Locale.getDefault(),
                        " (+%.0f%%)", (weatherMultiplier - 1.0) * 100);
            }

            tvFairRange.setText(infoText);
        });
    }

    private void calculateFairFare(double distance, int passengers) {
        double baseFare = 60.0;
        double farePerKm = 15.0;

        calculatedFairFare = baseFare + (distance * farePerKm);
        calculatedFairFare *= getTimeMultiplier();
        calculatedFairFare *= getTrafficMultiplier();
        calculatedFairFare *= weatherMultiplier; // Apply weather multiplier
        calculatedFairFare *= getPassengerMultiplier(passengers);
        calculatedFairFare *= getDistanceMultiplier(distance);

        double trafficFactor = Math.max(1.0, trafficDuration / Math.max(routeDuration, 1.0));
        calculatedFairFare *= trafficFactor;

        double range = Math.max(50, calculatedFairFare * 0.2);
        minFairFare = Math.max(calculatedFairFare - range, baseFare);
        maxFairFare = calculatedFairFare + range;

        minFairFare = Math.round(minFairFare / 10) * 10;
        maxFairFare = Math.round(maxFairFare / 10) * 10;
        calculatedFairFare = Math.round(calculatedFairFare / 10) * 10;
    }

    private String getFareFairReason(double userFare) {
        String reason = String.format(Locale.getDefault(),
                "Your fare is appropriate for:\n• %.1f km distance\n• %s\n• %s traffic",
                routeDistance, getTimeOfDayDescription(),
                getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));

        // Add weather info if it affects fare
        if (weatherMultiplier > 1.0) {
            reason += String.format(Locale.getDefault(),
                    "\n• %s weather (increased fare by %.0f%%)",
                    currentWeather, (weatherMultiplier - 1.0) * 100);
        }

        reason += String.format(Locale.getDefault(),
                "\n• %d passenger%s",
                passengersCount, passengersCount > 1 ? "s" : "");

        return reason;
    }

    private String getFareUnfairReason(double userFare, double distance) {
        if (userFare < minFairFare) {
            String reason = String.format(Locale.getDefault(),
                    "Your fare is below the fair market rate for:\n• Distance: %.1f km\n• Time: %s\n• Traffic: %s",
                    distance, getTimeOfDayDescription(),
                    getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));

            // Add weather info if it affects fare
            if (weatherMultiplier > 1.0) {
                reason += String.format(Locale.getDefault(),
                        "\n• %s weather (requires %.0f%% increase)",
                        currentWeather, (weatherMultiplier - 1.0) * 100);
            }

            return reason;
        } else {
            return String.format(Locale.getDefault(),
                    "Your fare is above what drivers typically expect for:\n• Distance: %.1f km\n• This might reduce your chance of getting a ride",
                    distance);
        }
    }

    private String getFareSuggestion(double userFare) {
        if (userFare < minFairFare) {
            String suggestion = "💡 Consider a higher fare to get better driver matches";

            // Add specific weather note if applicable
            if (weatherMultiplier > 1.0) {
                suggestion += " (especially in " + currentWeather.toLowerCase() + " weather)";
            }

            return suggestion;
        } else {
            return "💡 Consider a lower fare to attract more drivers";
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
            Toast.makeText(this, "Please login to post", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPostRequest.setText("Posting...");
        btnPostRequest.setEnabled(false);

        // Get user data from Firestore first
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> rideRequest = createRideRequestData(currentUser, documentSnapshot);

                    db.collection("ride_requests")
                            .add(rideRequest)
                            .addOnSuccessListener(documentReference -> {
                                // Schedule auto-deletion at departure time
                                long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

                                // Only schedule if departure time is in the future
                                if (delay > 0) {
                                    new android.os.Handler().postDelayed(() -> {
                                        db.collection("ride_requests")
                                                .document(documentReference.getId())
                                                .delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "Auto-deleted expired ride request: " + documentReference.getId());
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to auto-delete ride request: " + e.getMessage());
                                                });
                                    }, delay);
                                } else {
                                    // If departure time has already passed, delete immediately
                                    documentReference.delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Deleted past ride request: " + documentReference.getId());
                                            });
                                }

                                Toast.makeText(this, "✓ Posted successfully!", Toast.LENGTH_SHORT).show();
                                clearForm();

                                new android.os.Handler().postDelayed(() -> {
                                    Intent intent = new Intent(PostRequestActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }, 2000);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnPostRequest.setText("Post Request");
                                btnPostRequest.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    btnPostRequest.setText("Post Request");
                    btnPostRequest.setEnabled(true);
                });
    }

    private Map<String, Object> createRideRequestData(FirebaseUser currentUser,
                                                      com.google.firebase.firestore.DocumentSnapshot userDoc) {
        Map<String, Object> rideRequest = new HashMap<>();

        // Passenger info
        rideRequest.put("passengerId", currentUser.getUid());
        rideRequest.put("passengerName", userDoc.exists() ? userDoc.getString("fullName") : "Anonymous");
        rideRequest.put("passengerPhone", userDoc.exists() ? userDoc.getString("phone") : "");
        rideRequest.put("passengerPhoto", userDoc.exists() ? userDoc.getString("profileImageUrl") : "");
        rideRequest.put("passengerRating", 4.5); // Default, should come from reviews later

        // Trip details
        rideRequest.put("pickupLocation", pickupAddress);
        rideRequest.put("dropLocation", dropAddress);
        rideRequest.put("pickupLat", pickupLatLng.latitude);
        rideRequest.put("pickupLng", pickupLatLng.longitude);
        rideRequest.put("dropLat", dropLatLng.latitude);
        rideRequest.put("dropLng", dropLatLng.longitude);

        // Fare and vehicle
        rideRequest.put("fare", Double.parseDouble(etFare.getText().toString().trim()));
        rideRequest.put("vehicleType", vehicleType);
        rideRequest.put("passengers", passengersCount);

        // Timing
        rideRequest.put("departureTime", calendar.getTimeInMillis());
        rideRequest.put("createdAt", System.currentTimeMillis());

        // Weather info
        rideRequest.put("weatherCondition", currentWeather);
        rideRequest.put("weatherDescription", weatherDescription);
        rideRequest.put("temperature", temperature);
        rideRequest.put("weatherMultiplier", weatherMultiplier);

        // Additional info
        rideRequest.put("specialRequest", etSpecialRequest.getText().toString().trim());
        rideRequest.put("status", "pending");
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
            Toast.makeText(this, "Select pickup location", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (dropLatLng == null) {
            Toast.makeText(this, "Select drop location", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etFare.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Enter fare amount", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (radioGroupVehicleType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Select vehicle type", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void clearForm() {
        if (autocompletePickup != null) autocompletePickup.setText("");
        if (autocompleteDrop != null) autocompleteDrop.setText("");
        pickupLatLng = null;
        dropLatLng = null;
        pickupAddress = "";
        dropAddress = "";
        tvDepartureTime.setText("Tap to select date & time");
        etFare.setText("");
        layoutFareAnalysis.setVisibility(View.GONE);
        passengersCount = 1;
        tvPassengersCount.setText("1");
        etSpecialRequest.setText("");
        radioGroupVehicleType.clearCheck();

        if (mMap != null) mMap.clear();
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        cardRoutePreview.setVisibility(View.GONE);
        layoutTrafficInfo.setVisibility(View.GONE);
        layoutWeatherInfo.setVisibility(View.GONE);
        btnPostRequest.setEnabled(false);
        btnPostRequest.setText("Post Request");
        btnCheckFare.setEnabled(false);

        // Reset weather data
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

    private double getTimeMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) return 1.2;
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 20)) return 1.3;
        if (hour >= 22 || hour <= 5) return 1.4;
        return 1.0;
    }

    private double getTrafficMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if ((hour >= 8 && hour <= 9) || (hour >= 17 && hour <= 18)) return 1.25;
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19)) return 1.15;
        return 1.0;
    }

    private double getPassengerMultiplier(int passengers) {
        return 1.0 + (passengers - 1) * 0.15;
    }

    private double getDistanceMultiplier(double distance) {
        if (distance > 20) return 0.9;
        if (distance > 10) return 0.95;
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
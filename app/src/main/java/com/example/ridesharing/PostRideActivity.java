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

public class PostRideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "PostRideActivity";
    private static final String GOOGLE_MAPS_API_KEY = "AIzaSyCD-k7OlWsemXLHwBXyBoQNO8r9rxRc9nM";
    private static final String OPENWEATHER_API_KEY = "acc6e705fe0c4b7c67e77a98cfc26122";
    private static final String OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    // UI Components
    private AutocompleteSupportFragment autocompletePickup, autocompleteDrop;
    private TextView tvDepartureTime, tvPassengersCount;
    private TextView tvDistance, tvDuration, tvTrafficInfo, tvWeatherInfo;
    private EditText etFare, etSpecialRequest;
    private Button btnCheckFare, btnPostRequest, btnDecreasePassengers, btnIncreasePassengers;
    private MaterialCardView cardRoutePreview;
    private LinearLayout layoutTrafficInfo, layoutFareAnalysis, layoutWeatherInfo;
    private TextView tvFairRange, tvFairnessMessage, tvDetailedReason, tvSuggestion, tvPassengerNote;
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
        setContentView(R.layout.activity_post_ride);

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
        tvPassengerNote = findViewById(R.id.tv_passenger_note);

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
                    Toast.makeText(PostRideActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PostRideActivity.this, "Error selecting location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupClickListeners() {
        tvDepartureTime.setOnClickListener(v -> showDateTimePicker());
        btnCheckFare.setOnClickListener(v -> checkFareFairness());
        btnPostRequest.setOnClickListener(v -> postRideOffer());

        radioGroupVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_car) {
                vehicleType = "car";
                // Enable passenger increase up to 6
                btnIncreasePassengers.setEnabled(true);
                btnDecreasePassengers.setEnabled(true);
                // Update passenger note
                if (tvPassengerNote != null) {
                    tvPassengerNote.setText("(Max 6 for Car)");
                    tvPassengerNote.setVisibility(View.VISIBLE);
                }
                // Reset to minimum 1 passenger
                if (passengersCount > 6) {
                    passengersCount = 1;
                    tvPassengersCount.setText("1");
                }
            } else if (checkedId == R.id.rb_bike) {
                vehicleType = "bike";
                // Force single passenger for bike
                passengersCount = 1;
                tvPassengersCount.setText("1");
                btnIncreasePassengers.setEnabled(false);
                btnDecreasePassengers.setEnabled(false);
                // Update passenger note
                if (tvPassengerNote != null) {
                    tvPassengerNote.setText("(Only 1 for Bike)");
                    tvPassengerNote.setVisibility(View.VISIBLE);
                }
            }
            validateFormCompleteness();
        });

        btnDecreasePassengers.setOnClickListener(v -> {
            if (vehicleType.equals("car") && passengersCount > 1) {
                passengersCount--;
                tvPassengersCount.setText(String.valueOf(passengersCount));
                validateFormCompleteness();
            }
        });

        btnIncreasePassengers.setOnClickListener(v -> {
            if (vehicleType.equals("car") && passengersCount < 6) {
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
                return 1.10; // Just 10% - safety concern
            case "rain":
            case "drizzle":
                return 1.08;  // Just 8% - minor inconvenience
            case "snow":
                return 1.15;  // 15% - rare in Bangladesh but difficult
            case "mist":
            case "fog":
            case "haze":
                return 1.05;  // Just 5% - visibility issue
            case "clouds":
                return 1.02;  // Just 2% - almost normal
            case "extreme":
                return 1.12;  // Just 12% - rare extreme
            case "clear":
            default:
                if (temperature > 35) { // Very hot
                    return 1.04; // Just 4% for AC usage
                } else if (temperature < 10) { // Very cold
                    return 1.04; // Just 4% for heating
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
        storeFareBreakdown(0, 0, passengersCount);

        final double finalUserFare = userFare;
        runOnUiThread(() -> {
            if (finalUserFare >= minFairFare && finalUserFare <= maxFairFare) {
                String reason = getFareFairReason(finalUserFare);
                showFareResult("✓ Fare is fair! You can post your offer.", true, reason);
                enablePostButton();
            } else {
                String reason = getFareUnfairReason(finalUserFare, routeDistance);
                String suggestion = getFareSuggestion(finalUserFare);
                showFareResult("Fare needs adjustment", false, reason, suggestion);
                disablePostButton();
            }

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
        // OFFICIAL BANGLADESH GOVERNMENT RATES for 1500cc AC Taxis
        double first2KmFare = 85.0;      // First 2 km: ৳85
        double perKmRate = 34.0;         // Each additional km: ৳34
        double waitingPer2Min = 8.5;     // Waiting per 2 min
        double callServiceFee = 20.0;    // Call service fee

        // Calculate official AC car fare
        double officialACFare;
        if (distance <= 2.0) {
            officialACFare = first2KmFare;
        } else {
            double extraKm = distance - 2.0;
            double extraFare = extraKm * perKmRate;
            officialACFare = first2KmFare + extraFare;
        }

        // Apply minimum charge
        double officialMinimum = 85.0;
        officialACFare = Math.max(officialACFare, officialMinimum);

        // Add waiting time charges - Use only EXTRA traffic time
        double estimatedWaitingMinutes = Math.max(0, (trafficDuration - routeDuration) * 0.7); // Only count 70% of extra time
        if (estimatedWaitingMinutes > 5) { // Only charge if more than 5 minutes
            double waitingCharges = Math.ceil(estimatedWaitingMinutes / 3.0) * 8.5; // Per 3 min (was 2 min)
            officialACFare += waitingCharges;
        }

        // Add call service fee
        officialACFare += callServiceFee;

        // VEHICLE-SPECIFIC CALCULATIONS
        if (vehicleType.equalsIgnoreCase("car")) {
            // AC CAR - Based on official rates with fairness adjustments
            calculatedFairFare = officialACFare;

            // Apply fairness adjustments for AC Car
            calculatedFairFare *= getACCarTimeMultiplier();
            calculatedFairFare *= getACCarWeatherMultiplier();
            calculatedFairFare *= getACCarPassengerMultiplier(passengers);

            // Traffic delay compensation (beyond waiting charges)
            double trafficDelayFactor = Math.max(1.0, trafficDuration / Math.max(routeDuration, 1.0));
            if (trafficDelayFactor > 1.3) { // Only if 30%+ extra time
                calculatedFairFare *= Math.min(trafficDelayFactor, 1.12); // Max 12% extra
            }

            // Set fair range for AC Car (±10% or ৳40, whichever is higher)
            double range = Math.max(40, calculatedFairFare * 0.10);
            minFairFare = Math.max(calculatedFairFare - range, 105.0); // ৳85 + ৳20 minimum
            maxFairFare = calculatedFairFare + range;

        } else if (vehicleType.equalsIgnoreCase("bike")) {
            // BIKE - Based on AC car rate with significant discount
            // Start with 45% of AC car fare (55% cheaper)
            calculatedFairFare = officialACFare * 0.45;

            // Apply bike-specific adjustments
            calculatedFairFare *= getBikeTimeMultiplier();
            calculatedFairFare *= getBikeWeatherMultiplier();

            // Traffic affects bikes less
            double trafficDelayFactor = Math.max(1.0, trafficDuration / Math.max(routeDuration, 1.0));
            if (trafficDelayFactor > 1.6) { // Only if 60%+ extra time (was 40%)
                calculatedFairFare *= Math.min(trafficDelayFactor, 1.05); // Max 5% extra (was 8%)
            }

            // Set fair range for Bike (±15% or ৳25, whichever is higher)
            double range = Math.max(25, calculatedFairFare * 0.15);
            minFairFare = Math.max(calculatedFairFare - range, 60.0); // Bike minimum ৳60
            maxFairFare = calculatedFairFare + range;
        }

        // Round to nearest 5 taka
        minFairFare = Math.round(minFairFare / 5) * 5;
        maxFairFare = Math.round(maxFairFare / 5) * 5;
        calculatedFairFare = Math.round(calculatedFairFare / 5) * 5;
    }

    // AC Car Multipliers
    private double getACCarTimeMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Weekend premium - smaller
        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            return 1.05; // Just 5% on weekends (was 8%)
        }

        // Peak hours (Dhaka specific) - smaller
        if ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19)) {
            return 1.08; // Just 8% during peak hours (was 12%)
        }

        // Late night (safety/convenience) - smaller
        if (hour >= 22 || hour <= 6) {
            return 1.10; // Just 10% for late night (was 15%)
        }

        return 1.0;
    }

    private double getACCarWeatherMultiplier() {
        // Weather affects AC cars less
        return Math.min(weatherMultiplier, 1.06); // Max 6% for AC cars
    }

    private double getACCarPassengerMultiplier(int passengers) {
        // AC Car can take 1-6 passengers - SMALL increases
        switch (passengers) {
            case 1: return 1.0;
            case 2: return 1.05;  // Just 5% for 2 passengers (was 10%)
            case 3: return 1.08;  // Just 8% for 3 passengers (was 18%)
            case 4: return 1.10;  // Just 10% for 4 passengers (was 25%)
            case 5: return 1.12;  // Just 12% for 5 passengers (was 30%)
            case 6: return 1.15;  // Just 15% for 6 passengers (was 35%)
            default: return 1.15; // Max 15% increase
        }
    }

    // Bike Multipliers
    private double getBikeTimeMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // Bike has reasonable premiums
        if (hour >= 22 || hour <= 6) {
            return 1.12; // Just 12% late night (was 20%)
        }

        if ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 19)) {
            return 1.08; // Just 8% peak hours (was 15%)
        }

        return 1.0;
    }

    private double getBikeWeatherMultiplier() {
        // Weather affects bikes significantly
        return Math.min(weatherMultiplier, 1.10); // Max 10% for bikes
    }

    private void storeFareBreakdown(double officialACFare, double waitingMinutes, int passengers) {
        StringBuilder breakdown = new StringBuilder();

        if (vehicleType.equalsIgnoreCase("car")) {
            breakdown.append("🚗 Car (1500cc) - Official Rate Based\n");
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
            double passengerMult = getACCarPassengerMultiplier(passengers);

            if (timeMult > 1.0) {
                breakdown.append(String.format("• %s: +%.0f%%\n", getTimeDescription(), (timeMult - 1.0) * 100));
            }
            if (weatherMult > 1.0) {
                breakdown.append(String.format("• %s weather: +%.0f%%\n", currentWeather, (weatherMult - 1.0) * 100));
            }
            if (passengerMult > 1.0) {
                breakdown.append(String.format("• %d passengers: +%.0f%%\n", passengers, (passengerMult - 1.0) * 100));
            }

        } else {
            breakdown.append("🏍️ Motorcycle - Fair Market Rate\n");
            breakdown.append("═══════════════════════════════════\n");
            breakdown.append("Based on 1500cc AC Car Official Rate:\n");
            breakdown.append("• Bike discount: 55% (standard market rate)\n");

            // Show bike adjustments
            breakdown.append("\nBike-Specific Adjustments:\n");
            double timeMult = getBikeTimeMultiplier();
            double weatherMult = getBikeWeatherMultiplier();

            if (timeMult > 1.0) {
                breakdown.append(String.format("• %s: +%.0f%%\n", getTimeDescription(), (timeMult - 1.0) * 100));
            }
            if (weatherMult > 1.0) {
                breakdown.append(String.format("• %s weather: +%.0f%%\n", currentWeather, (weatherMult - 1.0) * 100));
            }

            breakdown.append("• Single passenger only\n");
        }

        breakdown.append(String.format("\n✅ Fair Fare Range: ৳%.0f - ৳%.0f", minFairFare, maxFairFare));
        breakdown.append(String.format("\n💡 Suggested: ৳%.0f", calculatedFairFare));

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
        String reason = String.format(Locale.getDefault(),
                "Your fare is appropriate for:\n• %.1f km distance\n• %s\n• %s traffic",
                routeDistance, getTimeOfDayDescription(),
                getTrafficLevel(calendar.get(Calendar.HOUR_OF_DAY)));

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

            if (weatherMultiplier > 1.0) {
                reason += String.format(Locale.getDefault(),
                        "\n• %s weather (requires %.0f%% increase)",
                        currentWeather, (weatherMultiplier - 1.0) * 100);
            }

            return reason;
        } else {
            return String.format(Locale.getDefault(),
                    "Your fare is above what passengers typically expect for:\n• Distance: %.1f km\n• This might reduce your chance of getting passengers",
                    distance);
        }
    }

    private String getFareSuggestion(double userFare) {
        if (userFare < minFairFare) {
            String suggestion = "💡 Consider a higher fare to cover your costs";

            if (weatherMultiplier > 1.0) {
                suggestion += " (especially in " + currentWeather.toLowerCase() + " weather)";
            }

            return suggestion;
        } else {
            return "💡 Consider a lower fare to attract more passengers";
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

    // CHANGED: Method name from postRideRequest() to postRideOffer()
    private void postRideOffer() {
        if (!validateForm()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to post", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPostRequest.setText("Posting...");
        btnPostRequest.setEnabled(false);

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> rideRequest = createRideRequestData(currentUser, documentSnapshot);

                    db.collection("ride_requests")
                            .add(rideRequest)
                            .addOnSuccessListener(documentReference -> {
                                long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

                                if (delay > 0) {
                                    new android.os.Handler().postDelayed(() -> {
                                        db.collection("ride_requests")
                                                .document(documentReference.getId())
                                                .get()
                                                .addOnSuccessListener(documentSnapshot1 -> {
                                                    if (documentSnapshot1.exists()) {
                                                        String status = documentSnapshot1.getString("status");

                                                        if ("pending".equals(status)) {
                                                            documentReference.delete()
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Log.d(TAG, "Auto-deleted expired pending ride offer: " + documentReference.getId());
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e(TAG, "Failed to auto-delete ride offer: " + e.getMessage());
                                                                    });
                                                        } else {
                                                            Log.d(TAG, "Ride offer " + documentReference.getId() + " has status '" + status + "', not deleting.");
                                                        }
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to check status before auto-delete: " + e.getMessage());
                                                });
                                    }, delay);
                                } else {
                                    db.collection("ride_requests")
                                            .document(documentReference.getId())
                                            .get()
                                            .addOnSuccessListener(documentSnapshot1 -> {
                                                if (documentSnapshot1.exists()) {
                                                    String status = documentSnapshot1.getString("status");

                                                    if ("pending".equals(status)) {
                                                        documentReference.delete()
                                                                .addOnSuccessListener(aVoid -> {
                                                                    Log.d(TAG, "Deleted past pending ride offer: " + documentReference.getId());
                                                                });
                                                    } else {
                                                        Log.d(TAG, "Ride offer has status '" + status + "', not deleting.");
                                                    }
                                                }
                                            });
                                }

                                Toast.makeText(this, "✅ Ride offer posted successfully!", Toast.LENGTH_SHORT).show();
                                clearForm();

                                new android.os.Handler().postDelayed(() -> {
                                    Intent intent = new Intent(PostRideActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }, 2000);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                btnPostRequest.setText("Post Ride Offer");
                                btnPostRequest.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    btnPostRequest.setText("Post Ride Offer");
                    btnPostRequest.setEnabled(true);
                });
    }

    private Map<String, Object> createRideRequestData(FirebaseUser currentUser,
                                                      com.google.firebase.firestore.DocumentSnapshot userDoc) {
        Map<String, Object> rideRequest = new HashMap<>();

        // ✅ DRIVER POST: Set to true
        rideRequest.put("isDriverPost", true);

        // Driver info (since this is a driver posting a ride)
        rideRequest.put("driverId", currentUser.getUid());
        rideRequest.put("driverName", userDoc.exists() ? userDoc.getString("fullName") : "Anonymous");
        rideRequest.put("driverPhone", userDoc.exists() ? userDoc.getString("phone") : "");
        rideRequest.put("driverPhoto", userDoc.exists() ? userDoc.getString("profileImageUrl") : "");
        rideRequest.put("driverRating", 4.5);

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
        if (tvPassengerNote != null) {
            tvPassengerNote.setText("");
            tvPassengerNote.setVisibility(View.GONE);
        }

        cardRoutePreview.setVisibility(View.GONE);
        layoutTrafficInfo.setVisibility(View.GONE);
        layoutWeatherInfo.setVisibility(View.GONE);
        btnPostRequest.setEnabled(false);
        btnPostRequest.setText("Post Ride Offer");
        btnCheckFare.setEnabled(false);

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

    // Old methods (keeping for backward compatibility, but using new ones instead)
    private double getTimeMultiplier() {
        return 1.0; // Replaced by getACCarTimeMultiplier() and getBikeTimeMultiplier()
    }

    private double getTrafficMultiplier() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if ((hour >= 8 && hour <= 9) || (hour >= 17 && hour <= 18))
            return 1.08; // Just 8% for heavy traffic (was 25%)
        if ((hour >= 7 && hour <= 10) || (hour >= 16 && hour <= 19))
            return 1.04; // Just 4% for moderate traffic (was 15%)
        return 1.0;
    }

    private double getPassengerMultiplier(int passengers) {
        return 1.0; // Replaced by getACCarPassengerMultiplier()
    }

    private double getDistanceMultiplier(double distance) {
        return 1.0; // Not used in new logic
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
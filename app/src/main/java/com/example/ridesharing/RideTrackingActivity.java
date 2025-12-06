package com.example.ridesharing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RideTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "RideTrackingActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private String rideId;
    private String currentUserId;
    private boolean isDriver;
    private String otherPersonName;
    private String pickupLocation;
    private String dropLocation;

    private Marker myMarker;
    private Marker otherPersonMarker;
    private Polyline routeLine;

    private ListenerRegistration rideListener;
    private ListenerRegistration otherLocationListener;

    private TextView tvMyLocation, tvOtherLocation, tvRideStatus, tvDistance;
    private View btnCenterMap, btnClose;

    private boolean isTrackingActive = false;
    private LatLng lastMyLocation = null;
    private LatLng lastOtherLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_tracking);

        // Get intent data
        rideId = getIntent().getStringExtra("rideId");
        otherPersonName = getIntent().getStringExtra("otherPersonName");
        pickupLocation = getIntent().getStringExtra("pickupLocation");
        dropLocation = getIntent().getStringExtra("dropLocation");
        isDriver = getIntent().getBooleanExtra("isDriver", false);

        if (rideId == null) {
            Toast.makeText(this, "Error: Ride ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "🗺️ Tracking started for ride: " + rideId + " | isDriver: " + isDriver);

        initializeViews();
        initializeFirebase();
        setupMap();
        setupListeners();
        checkLocationPermission();
    }

    private void initializeViews() {
        tvMyLocation = findViewById(R.id.tv_my_location);
        tvOtherLocation = findViewById(R.id.tv_other_location);
        tvRideStatus = findViewById(R.id.tv_ride_status);
        tvDistance = findViewById(R.id.tv_distance);
        btnCenterMap = findViewById(R.id.btn_center_map);
        btnClose = findViewById(R.id.btn_close);

        String myRole = isDriver ? "Driver" : "Passenger";
        String otherRole = isDriver ? "Passenger" : "Driver";

        tvMyLocation.setText("📍 My Location (" + myRole + ")");
        tvOtherLocation.setText("📍 " + otherPersonName + " (" + otherRole + ")");
        tvRideStatus.setText("🚗 Tracking: " + pickupLocation + " → " + dropLocation);

        btnClose.setOnClickListener(v -> finish());
        btnCenterMap.setOnClickListener(v -> centerMapOnBothLocations());
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupListeners() {
        // Listen for ride status changes
        rideListener = db.collection("ride_requests")
                .document(rideId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to ride", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if ("completed".equals(status) || "cancelled".equals(status)) {
                            Toast.makeText(this, "Ride " + status + ". Tracking ended.", Toast.LENGTH_SHORT).show();
                            stopTracking();
                            finish();
                        }
                    }
                });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            startTracking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(false); // We'll use custom markers
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
        }

        // Center on Dhaka by default
        LatLng dhaka = new LatLng(23.8103, 90.4125);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dhaka, 12));
    }

    private void startTracking() {
        if (isTrackingActive) return;
        isTrackingActive = true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Create location request
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000) // Update every 5 seconds
                .setMinUpdateIntervalMillis(3000)
                .build();

        // Location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateMyLocation(location);
                }
            }
        };

        // Start location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Listen to other person's location
        listenToOtherPersonLocation();

        Log.d(TAG, "✅ Tracking started");
    }

    private void updateMyLocation(Location location) {
        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        lastMyLocation = myLatLng;

        // Update marker on map
        if (myMarker == null && mMap != null) {
            MarkerOptions options = new MarkerOptions()
                    .position(myLatLng)
                    .title(isDriver ? "You (Driver)" : "You (Passenger)")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            isDriver ? BitmapDescriptorFactory.HUE_BLUE : BitmapDescriptorFactory.HUE_GREEN));
            myMarker = mMap.addMarker(options);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
        } else if (myMarker != null) {
            myMarker.setPosition(myLatLng);
        }

        // Upload location to Firestore
        uploadLocationToFirestore(location);

        // Draw route if both locations are available
        if (lastOtherLocation != null) {
            fetchAndDrawRoute(myLatLng, lastOtherLocation);
            updateDistance();
        }
    }

    private void uploadLocationToFirestore(Location location) {
        if (currentUserId == null || rideId == null) return;

        String locationField = isDriver ? "driverLocation" : "passengerLocation";

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());

        db.collection("ride_requests")
                .document(rideId)
                .update(locationField, locationData)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }

    private void listenToOtherPersonLocation() {
        String locationField = isDriver ? "passengerLocation" : "driverLocation";

        otherLocationListener = db.collection("ride_requests")
                .document(rideId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to other location", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> locationData = (Map<String, Object>) snapshot.get(locationField);
                        if (locationData != null) {
                            double lat = (double) locationData.get("latitude");
                            double lng = (double) locationData.get("longitude");
                            updateOtherPersonLocation(new LatLng(lat, lng));
                        }
                    }
                });
    }

    private void updateOtherPersonLocation(LatLng latLng) {
        if (mMap == null) return;

        lastOtherLocation = latLng;

        // Update or create marker
        if (otherPersonMarker == null) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(otherPersonName + (isDriver ? " (Passenger)" : " (Driver)"))
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            isDriver ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_BLUE));
            otherPersonMarker = mMap.addMarker(options);
        } else {
            otherPersonMarker.setPosition(latLng);
        }

        // Draw route between both locations
        if (lastMyLocation != null) {
            fetchAndDrawRoute(lastMyLocation, lastOtherLocation);
            updateDistance();
        }
    }

    private void fetchAndDrawRoute(LatLng origin, LatLng destination) {
        new Thread(() -> {
            try {
                String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin.latitude + "," + origin.longitude +
                        "&destination=" + destination.latitude + "," + destination.longitude +
                        "&key=AIzaSyCD-k7OlWsemXLHwBXyBoQNO8r9rxRc9nM"; // Replace with your actual API key

                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.getString("status");

                if ("OK".equals(status)) {
                    JSONArray routes = jsonResponse.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPolyline = overviewPolyline.getString("points");

                        List<LatLng> decodedPath = decodePolyline(encodedPolyline);

                        runOnUiThread(() -> drawRoute(decodedPath));
                    }
                } else {
                    Log.e(TAG, "Directions API error: " + status);
                    // Fallback to straight line
                    runOnUiThread(() -> drawStraightLine(origin, destination));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching route", e);
                // Fallback to straight line
                runOnUiThread(() -> drawStraightLine(origin, destination));
            }
        }).start();
    }

    private void drawRoute(List<LatLng> path) {
        if (mMap == null || path == null || path.isEmpty()) return;

        // Remove old route
        if (routeLine != null) {
            routeLine.remove();
        }

        // Draw new route
        PolylineOptions options = new PolylineOptions()
                .addAll(path)
                .width(10)
                .color(Color.parseColor("#2196F3"))
                .geodesic(true);

        routeLine = mMap.addPolyline(options);

        // Auto-center map
        centerMapOnBothLocations();
    }

    private void drawStraightLine(LatLng origin, LatLng destination) {
        if (mMap == null) return;

        // Remove old route
        if (routeLine != null) {
            routeLine.remove();
        }

        // Draw straight line as fallback
        PolylineOptions options = new PolylineOptions()
                .add(origin, destination)
                .width(8)
                .color(Color.parseColor("#FF9800"))
                .geodesic(true);

        routeLine = mMap.addPolyline(options);
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

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void updateDistance() {
        if (lastMyLocation == null || lastOtherLocation == null || tvDistance == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                lastMyLocation.latitude, lastMyLocation.longitude,
                lastOtherLocation.latitude, lastOtherLocation.longitude,
                results
        );

        float distanceInMeters = results[0];
        String distanceText;

        if (distanceInMeters < 1000) {
            distanceText = String.format("📏 Distance: %.0f m", distanceInMeters);
        } else {
            distanceText = String.format("📏 Distance: %.2f km", distanceInMeters / 1000);
        }

        tvDistance.setText(distanceText);
        tvDistance.setVisibility(View.VISIBLE);
    }

    private void centerMapOnBothLocations() {
        if (mMap == null || myMarker == null || otherPersonMarker == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(myMarker.getPosition());
        builder.include(otherPersonMarker.getPosition());

        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
    }

    private void stopTracking() {
        isTrackingActive = false;

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (rideListener != null) {
            rideListener.remove();
        }

        if (otherLocationListener != null) {
            otherLocationListener.remove();
        }

        // Delete location data from Firestore
        if (rideId != null && currentUserId != null) {
            String locationField = isDriver ? "driverLocation" : "passengerLocation";
            Map<String, Object> deleteData = new HashMap<>();
            deleteData.put(locationField, null);

            db.collection("ride_requests")
                    .document(rideId)
                    .update(deleteData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Location data deleted"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete location", e));
        }

        Log.d(TAG, "🛑 Tracking stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTracking();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep tracking in background for now
    }
}
package com.example.ridesharing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SafetyActivity extends AppCompatActivity {

    private static final String TAG = "SafetyActivity";
    private static final int SMS_PERMISSION_REQUEST = 101;
    private static final int LOCATION_PERMISSION_REQUEST = 102;
    private static final int CALL_PERMISSION_REQUEST = 103;
    private static final String EMERGENCY_NUMBER_BD = "999";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private String rideId;
    private String pickupLocation;
    private String dropLocation;
    private String driverName;
    private long rideStartTime;

    private TextView tvRideTimer, tvRideInfo, tvDriverInfo, tvEmptyContacts;
    private MaterialButton btnSOS, btnCall999, btnShareLocation, btnManageContacts;
    // 🔥 REMOVED: MaterialCardView declarations - not needed
    private RecyclerView recyclerEmergencyContacts;
    private View layoutEmptyContacts;

    private EmergencyContactsAdapter adapter;
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety);

        Log.d(TAG, "🛡️ SafetyActivity started");

        // Get ride data from intent
        rideId = getIntent().getStringExtra("rideId");
        pickupLocation = getIntent().getStringExtra("pickupLocation");
        dropLocation = getIntent().getStringExtra("dropLocation");
        driverName = getIntent().getStringExtra("driverName");
        rideStartTime = getIntent().getLongExtra("rideStartTime", System.currentTimeMillis());

        if (rideId == null) {
            Toast.makeText(this, "Error: Ride information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        initializeFirebase();
        setupClickListeners();
        loadEmergencyContacts();
        startRideTimer();
        checkAndRequestLocationPermission();
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            getCurrentLocation();
        }
    }

    private void initializeViews() {
        tvRideTimer = findViewById(R.id.tv_ride_timer);
        tvRideInfo = findViewById(R.id.tv_ride_info);
        tvDriverInfo = findViewById(R.id.tv_driver_info);
        tvEmptyContacts = findViewById(R.id.tv_empty_contacts);
        btnSOS = findViewById(R.id.btn_sos);
        btnCall999 = findViewById(R.id.btn_call_999);
        btnShareLocation = findViewById(R.id.btn_share_location);
        btnManageContacts = findViewById(R.id.btn_manage_contacts);

        // 🔥 REMOVED: CardView assignments - they're not used anywhere in the code
        // cardRideInfo = findViewById(R.id.card_ride_info);
        // cardEmergency = findViewById(R.id.card_emergency);

        recyclerEmergencyContacts = findViewById(R.id.recycler_emergency_contacts);
        layoutEmptyContacts = findViewById(R.id.layout_empty_contacts);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Set ride info with null checks
        if (tvRideInfo != null) {
            tvRideInfo.setText("📍 " + (pickupLocation != null ? pickupLocation : "Unknown") +
                    " → " + (dropLocation != null ? dropLocation : "Unknown"));
        }
        if (tvDriverInfo != null) {
            tvDriverInfo.setText("🚗 Driver: " + (driverName != null ? driverName : "Unknown"));
        }

        // Setup RecyclerView
        if (recyclerEmergencyContacts != null) {
            recyclerEmergencyContacts.setLayoutManager(new LinearLayoutManager(this));
            adapter = new EmergencyContactsAdapter(emergencyContacts, new EmergencyContactsAdapter.OnContactClickListener() {
                @Override
                public void onCallClick(EmergencyContact contact) {
                    callEmergencyContact(contact);
                }

                @Override
                public void onShareLocationClick(EmergencyContact contact) {
                    shareLocationWithContact(contact);
                }
            });
            recyclerEmergencyContacts.setAdapter(adapter);
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupClickListeners() {
        if (btnSOS != null) btnSOS.setOnClickListener(v -> showSOSConfirmation());
        if (btnCall999 != null) btnCall999.setOnClickListener(v -> callEmergencyServices());
        if (btnShareLocation != null) btnShareLocation.setOnClickListener(v -> shareLocationWithAll());
        if (btnManageContacts != null) btnManageContacts.setOnClickListener(v -> openManageContacts());
    }

    private void startRideTimer() {
        if (tvRideTimer == null) return;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - rideStartTime;
                long hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60;

                String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                tvRideTimer.setText("⏱️ Ride Duration: " + timeString);

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission not granted");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "✅ Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                    } else {
                        Log.d(TAG, "⚠️ Location is null");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get location", e);
                });
    }

    private void loadEmergencyContacts() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    emergencyContacts.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        EmergencyContact contact = document.toObject(EmergencyContact.class);
                        contact.setId(document.getId());
                        emergencyContacts.add(contact);
                    }

                    if (adapter != null) adapter.notifyDataSetChanged();

                    if (layoutEmptyContacts != null && recyclerEmergencyContacts != null) {
                        if (emergencyContacts.isEmpty()) {
                            layoutEmptyContacts.setVisibility(View.VISIBLE);
                            recyclerEmergencyContacts.setVisibility(View.GONE);
                        } else {
                            layoutEmptyContacts.setVisibility(View.GONE);
                            recyclerEmergencyContacts.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading emergency contacts", e);
                    Toast.makeText(this, "Failed to load emergency contacts", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSOSConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("🚨 EMERGENCY SOS")
                .setMessage("This will immediately:\n\n" +
                        "✓ Alert all your emergency contacts\n" +
                        "✓ Share your live location\n" +
                        "✓ Send ride details\n" +
                        "✓ Call emergency services (999)\n\n" +
                        "Are you in danger?")
                .setPositiveButton("YES - SEND SOS", (dialog, which) -> triggerSOS())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void triggerSOS() {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "⚠️ No emergency contacts added. Calling 999...", Toast.LENGTH_LONG).show();
            callEmergencyServices();
            return;
        }

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST);
            return;
        }

        sendSOSToAllContacts();
    }

    private void sendSOSToAllContacts() {
        String locationUrl = currentLocation != null ?
                "https://maps.google.com/?q=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() :
                "Location unavailable";

        String sosMessage = "🚨 EMERGENCY SOS 🚨\n\n" +
                "I need help! I'm in a ride.\n\n" +
                "Driver: " + (driverName != null ? driverName : "Unknown") + "\n" +
                "From: " + (pickupLocation != null ? pickupLocation : "Unknown") + "\n" +
                "To: " + (dropLocation != null ? dropLocation : "Unknown") + "\n" +
                "My Location: " + locationUrl + "\n\n" +
                "Ride ID: " + rideId + "\n" +
                "Time: " + new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date());

        SmsManager smsManager = SmsManager.getDefault();
        int successCount = 0;

        for (EmergencyContact contact : emergencyContacts) {
            try {
                ArrayList<String> parts = smsManager.divideMessage(sosMessage);
                smsManager.sendMultipartTextMessage(
                        contact.getPhoneNumber(),
                        null,
                        parts,
                        null,
                        null
                );
                successCount++;
                Log.d(TAG, "✅ SOS sent to: " + contact.getName());
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to send SOS to " + contact.getName(), e);
            }
        }

        if (successCount > 0) {
            Toast.makeText(this, "✅ SOS sent to " + successCount + " contact(s)", Toast.LENGTH_LONG).show();

            new AlertDialog.Builder(this)
                    .setTitle("Call Emergency Services?")
                    .setMessage("SOS alerts sent. Do you want to call 999 now?")
                    .setPositiveButton("Yes, Call 999", (dialog, which) -> callEmergencyServices())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            Toast.makeText(this, "❌ Failed to send SOS. Calling 999...", Toast.LENGTH_SHORT).show();
            callEmergencyServices();
        }
    }

    private void callEmergencyServices() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER_BD));

            // Check if we can use ACTION_CALL for direct calling
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER_BD));
            }

            startActivity(intent);
            Log.d(TAG, "📞 Calling emergency services: " + EMERGENCY_NUMBER_BD);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error calling emergency services", e);
            Toast.makeText(this, "Unable to call emergency services", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareLocationWithAll() {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts to share with", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLocation == null) {
            Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST);
            return;
        }

        String locationUrl = "https://maps.google.com/?q=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude();
        String message = "📍 My Current Location\n\n" +
                "I'm sharing my location with you.\n" +
                "Ride: " + (pickupLocation != null ? pickupLocation : "Unknown") + " → " +
                (dropLocation != null ? dropLocation : "Unknown") + "\n" +
                "Driver: " + (driverName != null ? driverName : "Unknown") + "\n\n" +
                "Location: " + locationUrl + "\n" +
                "Time: " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        SmsManager smsManager = SmsManager.getDefault();
        int successCount = 0;

        for (EmergencyContact contact : emergencyContacts) {
            try {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(
                        contact.getPhoneNumber(),
                        null,
                        parts,
                        null,
                        null
                );
                successCount++;
            } catch (Exception e) {
                Log.e(TAG, "Failed to share location with " + contact.getName(), e);
            }
        }

        Toast.makeText(this, "✅ Location shared with " + successCount + " contact(s)", Toast.LENGTH_SHORT).show();
    }

    private void callEmergencyContact(EmergencyContact contact) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to call " + contact.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareLocationWithContact(EmergencyContact contact) {
        if (currentLocation == null) {
            Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST);
            return;
        }

        String locationUrl = "https://maps.google.com/?q=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude();
        String message = "📍 Sharing my location with you\n" +
                "Location: " + locationUrl + "\n" +
                "Time: " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contact.getPhoneNumber(), null, message, null, null);
            Toast.makeText(this, "✅ Location shared with " + contact.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to share location", e);
            Toast.makeText(this, "Failed to share location", Toast.LENGTH_SHORT).show();
        }
    }

    private void openManageContacts() {
        Intent intent = new Intent(this, ManageEmergencyContactsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted. Please try again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission required for emergency alerts", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission required for safety features", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmergencyContacts();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
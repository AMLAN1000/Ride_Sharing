package com.example.ridesharing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Background service to listen for ride acceptance notifications.
 * Handles BOTH directions:
 * 1. When a DRIVER accepts a PASSENGER's request
 * 2. When a PASSENGER accepts a DRIVER's posted ride
 */
public class RideListenerService extends Service {

    private static final String TAG = "RideListenerService";
    private static final String FOREGROUND_CHANNEL_ID = "ride_listener_service";
    private static final int FOREGROUND_NOTIFICATION_ID = 9999;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration passengerRequestsListener;
    private ListenerRegistration driverRequestsListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 Service created");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);
        createForegroundNotificationChannel();

        // Start as foreground service to prevent Android from killing it
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        startListeningForAcceptedRides();
    }

    private void createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Ride Listener Service",
                    NotificationManager.IMPORTANCE_LOW // Low importance = silent
            );
            channel.setDescription("Keeps the app listening for ride notifications");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Ride Sharing Active")
                .setContentText("Listening for ride updates...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void startListeningForAcceptedRides() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "❌ No user logged in, stopping service");
            stopSelf();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "👤 Starting listeners for user: " + userId);

        // ========================================
        // LISTENER 1: For PASSENGER notifications
        // When I'm the passenger and a driver accepts my request
        // ========================================
        passengerRequestsListener = db.collection("ride_requests")
                .whereEqualTo("passengerId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Listen failed for passenger requests", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                QueryDocumentSnapshot document = dc.getDocument();
                                String status = document.getString("status");
                                Boolean notifShown = document.getBoolean("notificationShown");
                                Boolean isDriverPost = document.getBoolean("isDriverPost");

                                // Check: status changed to accepted, notification not shown yet, NOT a driver post
                                if ("accepted".equals(status) &&
                                        (notifShown == null || !notifShown) &&
                                        (isDriverPost == null || !isDriverPost)) {

                                    Log.d(TAG, "🔔 PASSENGER notification triggered for request: " + document.getId());
                                    showPassengerNotification(document);

                                    // Mark as shown
                                    document.getReference().update("notificationShown", true)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "✅ Marked passenger notification as shown");
                                            });
                                }
                            }
                        }
                    }
                });

        // ========================================
        // LISTENER 2: For DRIVER notifications
        // When I'm the driver and a passenger accepts my posted ride
        // ========================================
        driverRequestsListener = db.collection("ride_requests")
                .whereEqualTo("driverId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Listen failed for driver requests", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                QueryDocumentSnapshot document = dc.getDocument();
                                String status = document.getString("status");
                                Boolean notifShown = document.getBoolean("notificationShown");
                                Boolean isDriverPost = document.getBoolean("isDriverPost");

                                // Check: status changed to accepted, notification not shown yet, IS a driver post
                                if ("accepted".equals(status) &&
                                        (notifShown == null || !notifShown) &&
                                        (isDriverPost != null && isDriverPost)) {

                                    Log.d(TAG, "🔔 DRIVER notification triggered for request: " + document.getId());
                                    showDriverNotification(document);

                                    // Mark as shown
                                    document.getReference().update("notificationShown", true)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "✅ Marked driver notification as shown");
                                            });
                                }
                            }
                        }
                    }
                });

        Log.d(TAG, "✅ Started listening for accepted rides (both directions)");
    }

    /**
     * Show notification to PASSENGER when DRIVER accepts their request
     */
    private void showPassengerNotification(QueryDocumentSnapshot document) {
        String driverName = document.getString("driverName");
        String driverPhone = document.getString("driverPhone");
        String pickupLocation = document.getString("pickupLocation");
        String dropLocation = document.getString("dropLocation");
        Double fare = document.getDouble("fare");

        NotificationHelper.showRideAcceptedByDriverNotification(
                this,
                driverName != null ? driverName : "Driver",
                driverPhone,
                pickupLocation != null ? pickupLocation : "Pickup",
                dropLocation != null ? dropLocation : "Drop",
                fare != null ? fare : 0.0,
                document.getId()
        );

        Log.d(TAG, "✅ Showed PASSENGER notification for ride: " + document.getId());
    }

    /**
     * Show notification to DRIVER when PASSENGER accepts their posted ride
     */
    private void showDriverNotification(QueryDocumentSnapshot document) {
        String passengerName = document.getString("passengerName");
        String passengerPhone = document.getString("passengerPhone");
        String pickupLocation = document.getString("pickupLocation");
        String dropLocation = document.getString("dropLocation");
        Double fare = document.getDouble("fare");

        NotificationHelper.showRideAcceptedByPassengerNotification(
                this,
                passengerName != null ? passengerName : "Passenger",
                passengerPhone,
                pickupLocation != null ? pickupLocation : "Pickup",
                dropLocation != null ? dropLocation : "Drop",
                fare != null ? fare : 0.0,
                document.getId()
        );

        Log.d(TAG, "✅ Showed DRIVER notification for ride: " + document.getId());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🔄 Service started/restarted");
        return START_STICKY; // Restart service if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (passengerRequestsListener != null) {
            passengerRequestsListener.remove();
            Log.d(TAG, "🛑 Passenger listener removed");
        }
        if (driverRequestsListener != null) {
            driverRequestsListener.remove();
            Log.d(TAG, "🛑 Driver listener removed");
        }

        Log.d(TAG, "💀 Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}
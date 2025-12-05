package com.example.ridesharing;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class RideStatusMonitor {

    private static final String TAG = "RideStatusMonitor";
    private static RideStatusMonitor instance;
    private ListenerRegistration statusListener;
    private RideNotificationManager rideNotificationManager;
    private Context context;

    private RideStatusMonitor(Context context) {
        this.context = context;
        this.rideNotificationManager = RideNotificationManager.getInstance(context);
    }

    public static synchronized RideStatusMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new RideStatusMonitor(context);
        }
        return instance;
    }

    public void startMonitoring(String userId) {
        stopMonitoring();
        Log.d(TAG, "🚀 Starting ride status monitoring for user: " + userId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Monitor all rides where user is involved
        statusListener = db.collection("ride_requests")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Error listening to rides", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot document = dc.getDocument();

                            // Check if user is involved in this ride
                            String driverId = document.getString("driverId");
                            String passengerId = document.getString("passengerId");
                            List<String> passengerIds = (List<String>) document.get("passengerIds");

                            boolean isUserInvolved = userId.equals(driverId) ||
                                    userId.equals(passengerId) ||
                                    (passengerIds != null && passengerIds.contains(userId));

                            if (!isUserInvolved) {
                                continue;
                            }

                            // Check for status changes
                            String newStatus = document.getString("status");
                            String oldStatus = getOldStatus(document.getId());

                            if (oldStatus != null && !oldStatus.equals(newStatus)) {
                                handleStatusChange(document, userId, oldStatus, newStatus);
                            }

                            // ✅ NEW: Check for carpool passenger count changes
                            Long newPassengerCount = document.getLong("passengerCount");
                            Long oldPassengerCount = getOldPassengerCount(document.getId());

                            if (oldPassengerCount != null && newPassengerCount != null &&
                                    !oldPassengerCount.equals(newPassengerCount) &&
                                    "carpool".equals(document.getString("type")) &&
                                    "pending".equals(newStatus)) {
                                handleCarpoolPassengerChange(document, userId, oldPassengerCount, newPassengerCount);
                            }

                            // Update tracked values
                            updateOldStatus(document.getId(), newStatus);
                            updateOldPassengerCount(document.getId(), newPassengerCount);
                        }
                    }
                });
    }

    private void handleCarpoolPassengerChange(DocumentSnapshot document, String userId,
                                              Long oldCount, Long newCount) {
        try {
            String driverId = document.getString("driverId");
            String lastUpdatedBy = document.getString("lastUpdatedBy");

            Log.d(TAG, String.format("🚗 Carpool passenger change: %d -> %d", oldCount, newCount));
            Log.d(TAG, String.format("👤 Current user: %s, Driver: %s, LastUpdatedBy: %s",
                    userId, driverId, lastUpdatedBy));

            // Only notify if someone ELSE made the change
            if (lastUpdatedBy != null && lastUpdatedBy.equals(userId)) {
                Log.d(TAG, "⏭️ Skipping carpool notification - I made this change");
                return;
            }

            // If I'm the driver, show "seat filled" notification
            if (userId.equals(driverId)) {
                rideNotificationManager.sendCarpoolSeatFilled(document);
                Log.d(TAG, "✅ Carpool seat filled notification sent to driver");
            }
            // If I'm a passenger who already joined, I don't need notification about another passenger joining
            else {
                Log.d(TAG, "⏭️ I'm a passenger, no notification needed for another passenger joining");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling carpool passenger change", e);
        }
    }

    private void handleStatusChange(DocumentSnapshot document, String userId,
                                    String oldStatus, String newStatus) {
        try {
            String rideId = document.getId();
            String rideType = document.getString("type");
            String driverId = document.getString("driverId");
            String passengerId = document.getString("passengerId");
            String notificationSentBy = document.getString("notificationSentBy");

            Log.d(TAG, String.format("🔄 Status change: %s -> %s for ride %s",
                    oldStatus, newStatus, rideId));
            Log.d(TAG, String.format("👤 Current user: %s, Driver: %s, Passenger: %s, SentBy: %s",
                    userId, driverId, passengerId, notificationSentBy));

            // Determine if THIS user is driver or passenger
            boolean isCurrentUserDriver = userId.equals(driverId);
            List<String> passengerIds = (List<String>) document.get("passengerIds");
            boolean isCurrentUserPassenger = userId.equals(passengerId) ||
                    (passengerIds != null && passengerIds.contains(userId));

            String myRole = isCurrentUserDriver ? "driver" : "passenger";

            Log.d(TAG, String.format("📱 My role: %s", myRole));

            // Only send notification if I'm NOT the one who made the change
            if (notificationSentBy != null && notificationSentBy.equals(userId)) {
                Log.d(TAG, "⏭️ Skipping notification - I made this change");
                return;
            }

            // Handle different status changes
            switch (newStatus) {
                case "accepted":
                    handleAcceptedStatus(document, userId, myRole, rideType);
                    break;

                case "cancelled":
                    rideNotificationManager.sendRideCancelled(document, myRole);
                    Log.d(TAG, "✅ Cancelled notification sent to: " + myRole);
                    break;

                case "completed":
                    rideNotificationManager.sendRideCompleted(document, myRole);
                    Log.d(TAG, "✅ Completed notification sent to: " + myRole);
                    break;

                case "in_progress":
                    rideNotificationManager.sendRideStarted(document, myRole);
                    Log.d(TAG, "✅ Started notification sent to: " + myRole);
                    break;

                case "no_show":
                    rideNotificationManager.sendNoShowNotification(document, myRole);
                    Log.d(TAG, "✅ No-show notification sent to: " + myRole);
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling status change", e);
        }
    }

    private void handleAcceptedStatus(DocumentSnapshot document, String userId,
                                      String myRole, String rideType) {
        try {
            if ("carpool".equals(rideType)) {
                handleCarpoolAccepted(document, userId, myRole);
            } else {
                // Regular ride accepted
                if ("passenger".equals(myRole)) {
                    rideNotificationManager.sendRideAcceptedForPassenger(document);
                    Log.d(TAG, "✅ Passenger acceptance notification sent");
                } else if ("driver".equals(myRole)) {
                    rideNotificationManager.sendRideAcceptedForDriver(document);
                    Log.d(TAG, "✅ Driver acceptance notification sent");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling accepted status", e);
        }
    }

    private void handleCarpoolAccepted(DocumentSnapshot document, String userId, String myRole) {
        try {
            Long passengerCount = document.getLong("passengerCount");
            Long maxSeats = document.getLong("maxSeats");

            if (passengerCount != null && maxSeats != null && passengerCount >= maxSeats) {
                // Carpool is now full
                if ("driver".equals(myRole)) {
                    rideNotificationManager.sendCarpoolFull(document);
                    Log.d(TAG, "✅ Carpool full notification sent to driver");
                } else {
                    // Passengers get notified that carpool is confirmed
                    rideNotificationManager.sendPassengerJoinedCarpool(document);
                    Log.d(TAG, "✅ Carpool confirmed notification sent to passenger");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling carpool accepted", e);
        }
    }

    // Track old statuses in memory
    private Map<String, String> rideStatuses = new HashMap<>();
    private Map<String, Long> passengerCounts = new HashMap<>();

    private String getOldStatus(String rideId) {
        return rideStatuses.get(rideId);
    }

    private void updateOldStatus(String rideId, String newStatus) {
        rideStatuses.put(rideId, newStatus);
    }

    private Long getOldPassengerCount(String rideId) {
        return passengerCounts.get(rideId);
    }

    private void updateOldPassengerCount(String rideId, Long newCount) {
        if (newCount != null) {
            passengerCounts.put(rideId, newCount);
        }
    }

    public void stopMonitoring() {
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
        Log.d(TAG, "🛑 Stopped ride status monitoring");
    }

    public static void initialize(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            getInstance(context).startMonitoring(currentUser.getUid());
        }

        // Listen for auth state changes
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            RideStatusMonitor monitor = getInstance(context);

            if (user != null) {
                monitor.startMonitoring(user.getUid());
                Log.d(TAG, "👤 User logged in, starting monitoring for: " + user.getUid());
            } else {
                monitor.stopMonitoring();
                Log.d(TAG, "👤 User logged out, stopping monitoring");
            }
        });
    }
}
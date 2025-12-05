package com.example.ridesharing;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.Locale;

public class RideNotificationManager {

    private static final String TAG = "RideNotificationManager";
    private static final String CHANNEL_ID = "ride_notifications";
    private static final String CHANNEL_NAME = "Ride Notifications";
    private static final int NOTIFICATION_ID_BASE = 1000;

    private Context context;
    private android.app.NotificationManager systemNotificationManager;
    private static RideNotificationManager instance;

    private RideNotificationManager(Context context) {
        this.context = context;
        createNotificationChannel();
        systemNotificationManager = (android.app.NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static synchronized RideNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new RideNotificationManager(context);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for ride updates");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
            );

            if (systemNotificationManager != null) {
                systemNotificationManager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created");
            }
        }
    }

    // ============ RIDE NOTIFICATIONS ============

    public void sendRideAcceptedForPassenger(DocumentSnapshot rideDoc) {
        try {
            String driverName = rideDoc.getString("driverName");
            String driverPhone = rideDoc.getString("driverPhone");
            String pickupLocation = rideDoc.getString("pickupLocation");
            String dropLocation = rideDoc.getString("dropLocation");
            Double fare = rideDoc.getDouble("fare");
            String rideId = rideDoc.getId();

            String title = "🎉 Ride Accepted!";
            String message = String.format(Locale.getDefault(),
                    "%s accepted your ride request!\n📍 From: %s\n🎯 To: %s\n💰 Fare: ৳%.0f",
                    driverName, pickupLocation, dropLocation, fare
            );

            if (driverPhone != null && !driverPhone.isEmpty()) {
                message += "\n📞 " + driverPhone;
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAsPassenger", true);
            intent.putExtra("rideId", rideId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, rideId + "_passenger_accepted");
            Log.d(TAG, "✅ Ride accepted notification sent to passenger");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending ride accepted notification", e);
        }
    }

    public void sendRideAcceptedForDriver(DocumentSnapshot rideDoc) {
        try {
            String passengerName = rideDoc.getString("passengerName");
            String passengerPhone = rideDoc.getString("passengerPhone");
            String pickupLocation = rideDoc.getString("pickupLocation");
            String dropLocation = rideDoc.getString("dropLocation");
            Double fare = rideDoc.getDouble("fare");
            String rideId = rideDoc.getId();

            String title = "🎉 Passenger Accepted Your Ride!";
            String message = String.format(Locale.getDefault(),
                    "%s accepted your ride offer!\n📍 From: %s\n🎯 To: %s\n💰 Fare: ৳%.0f",
                    passengerName, pickupLocation, dropLocation, fare
            );

            if (passengerPhone != null && !passengerPhone.isEmpty()) {
                message += "\n📞 " + passengerPhone;
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAsDriver", true);
            intent.putExtra("rideId", rideId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, rideId + "_driver_accepted");
            Log.d(TAG, "✅ Ride accepted notification sent to driver");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending ride accepted notification to driver", e);
        }
    }

    // ============ CARPOOL NOTIFICATIONS ============

    public void sendCarpoolSeatFilled(DocumentSnapshot carpoolDoc) {
        try {
            List<String> passengerNames = (List<String>) carpoolDoc.get("passengerNames");
            String lastPassenger = passengerNames != null && !passengerNames.isEmpty() ?
                    passengerNames.get(passengerNames.size() - 1) : "A passenger";

            String pickupLocation = carpoolDoc.getString("pickupLocation");
            String dropLocation = carpoolDoc.getString("dropLocation");
            Long passengerCount = carpoolDoc.getLong("passengerCount");
            Long maxSeats = carpoolDoc.getLong("maxSeats");
            Double farePerPassenger = carpoolDoc.getDouble("farePerPassenger");
            String carpoolId = carpoolDoc.getId();

            String title = "👥 Carpool Seat Filled!";
            String message = String.format(Locale.getDefault(),
                    "%s joined your carpool!\n📍 From: %s\n🎯 To: %s\n💰 Fare per passenger: ৳%.0f\n👥 %d/%d seats filled",
                    lastPassenger, pickupLocation, dropLocation, farePerPassenger,
                    passengerCount, maxSeats
            );

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAsDriver", true);
            intent.putExtra("rideId", carpoolId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, carpoolId + "_seat_filled");
            Log.d(TAG, "✅ Carpool seat filled notification sent to driver");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending carpool seat filled notification", e);
        }
    }

    public void sendCarpoolFull(DocumentSnapshot carpoolDoc) {
        try {
            String pickupLocation = carpoolDoc.getString("pickupLocation");
            String dropLocation = carpoolDoc.getString("dropLocation");
            Long maxSeats = carpoolDoc.getLong("maxSeats");
            String carpoolId = carpoolDoc.getId();

            String title = "🚗 Carpool Full!";
            String message = String.format(Locale.getDefault(),
                    "Your carpool is now full with %d passengers!\n📍 From: %s\n🎯 To: %s\n✅ Ready to depart!",
                    maxSeats, pickupLocation, dropLocation
            );

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAsDriver", true);
            intent.putExtra("rideId", carpoolId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, carpoolId + "_full");
            Log.d(TAG, "✅ Carpool full notification sent to driver");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending carpool full notification", e);
        }
    }

    public void sendPassengerJoinedCarpool(DocumentSnapshot carpoolDoc) {
        try {
            String driverName = carpoolDoc.getString("driverName");
            String driverPhone = carpoolDoc.getString("driverPhone");
            String pickupLocation = carpoolDoc.getString("pickupLocation");
            String dropLocation = carpoolDoc.getString("dropLocation");
            Double farePerPassenger = carpoolDoc.getDouble("farePerPassenger");
            Long passengerCount = carpoolDoc.getLong("passengerCount");
            Long maxSeats = carpoolDoc.getLong("maxSeats");
            String carpoolId = carpoolDoc.getId();

            String title = "✅ Joined Carpool Successfully!";
            String message = String.format(Locale.getDefault(),
                    "You joined %s's carpool!\n📍 From: %s\n🎯 To: %s\n💰 Your fare: ৳%.0f\n👥 %d/%d passengers joined",
                    driverName, pickupLocation, dropLocation, farePerPassenger, passengerCount, maxSeats
            );

            if (driverPhone != null && !driverPhone.isEmpty()) {
                message += "\n📞 Driver: " + driverPhone;
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAsPassenger", true);
            intent.putExtra("rideId", carpoolId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, carpoolId + "_joined");
            Log.d(TAG, "✅ Passenger joined carpool notification sent");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending passenger joined carpool notification", e);
        }
    }

    // ============ STATUS CHANGE NOTIFICATIONS ============

    public void sendRideCancelled(DocumentSnapshot rideDoc, String userType) {
        try {
            String title = "❌ Ride Cancelled";
            String message = "";
            String rideId = rideDoc.getId();

            if ("driver".equals(userType)) {
                String passengerName = rideDoc.getString("passengerName");
                message = String.format("Your ride with %s has been cancelled.", passengerName);
            } else if ("passenger".equals(userType)) {
                String driverName = rideDoc.getString("driverName");
                message = String.format("Your ride with %s has been cancelled.", driverName);
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAs" + capitalizeFirstLetter(userType), true);
            intent.putExtra("rideId", rideId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, rideId + "_cancelled_" + userType);
            Log.d(TAG, "✅ Ride cancelled notification sent to " + userType);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending ride cancelled notification", e);
        }
    }

    public void sendRideCompleted(DocumentSnapshot rideDoc, String userType) {
        try {
            String title = "✅ Ride Completed Successfully!";
            String message = "";
            Double fare = rideDoc.getDouble("fare");
            String rideId = rideDoc.getId();

            if ("driver".equals(userType)) {
                String passengerName = rideDoc.getString("passengerName");
                message = String.format("You completed the ride with %s\n💰 Fare earned: ৳%.0f", passengerName, fare);
            } else if ("passenger".equals(userType)) {
                String driverName = rideDoc.getString("driverName");
                message = String.format("Your ride with %s is completed\n💰 Fare paid: ৳%.0f", driverName, fare);
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAs" + capitalizeFirstLetter(userType), true);
            intent.putExtra("rideId", rideId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, rideId + "_completed_" + userType);
            Log.d(TAG, "✅ Ride completed notification sent to " + userType);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending ride completed notification", e);
        }
    }

    // ============ NEW METHODS ADDED ============

    public void sendRideStarted(DocumentSnapshot rideDoc, String userType) {
        try {
            String title = "🚗 Ride Started!";
            String message = "";
            String rideId = rideDoc.getId();

            if ("driver".equals(userType)) {
                String passengerName = rideDoc.getString("passengerName");
                message = String.format("You've started the ride with %s", passengerName);
            } else if ("passenger".equals(userType)) {
                String driverName = rideDoc.getString("driverName");
                message = String.format("Your driver %s has started the ride", driverName);
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAs" + capitalizeFirstLetter(userType), true);
            intent.putExtra("rideId", rideId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, rideId + "_started_" + userType);
            Log.d(TAG, "✅ Ride started notification sent to " + userType);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending ride started notification", e);
        }
    }

    public void sendNoShowNotification(DocumentSnapshot rideDoc, String userType) {
        try {
            String title = "⏰ No Show";
            String message = "";
            String rideId = rideDoc.getId();

            if ("driver".equals(userType)) {
                message = "Passenger didn't show up for the scheduled ride";
            } else if ("passenger".equals(userType)) {
                message = "You missed your scheduled ride";
            }

            Intent intent = new Intent(context, MyRidesActivity.class);
            intent.putExtra("openAs" + capitalizeFirstLetter(userType), true);
            intent.putExtra("rideId", rideId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            sendNotification(title, message, intent, rideId + "_noshow_" + userType);
            Log.d(TAG, "✅ No-show notification sent to " + userType);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending no-show notification", e);
        }
    }

    // ============ HELPER METHODS ============

    private void sendNotification(String title, String message, Intent intent, String notificationId) {
        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            builder.setVibrate(new long[]{0, 500, 200, 500});
            builder.setLights(Color.GREEN, 1000, 1000);

            int notificationIdInt = NOTIFICATION_ID_BASE + Math.abs(notificationId.hashCode());
            systemNotificationManager.notify(notificationIdInt, builder.build());

            Log.d(TAG, "✅ Notification sent: " + title);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending notification", e);
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static void createNotificationChannelStatic(Context context) {
        getInstance(context);
    }
}
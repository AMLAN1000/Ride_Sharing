package com.example.ridesharing;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * Helper class for showing local notifications when ride requests are accepted.
 * Works for BOTH passengers and drivers.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "ride_notifications";
    private static final String CHANNEL_NAME = "Ride Notifications";
    private static final int NOTIFICATION_ID_BASE = 1000;

    /**
     * Initialize notification channel (required for Android 8.0+)
     * Call this once when app starts
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for ride request updates");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
            );

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created");
            }
        }
    }

    /**
     * Show notification when a PASSENGER's ride request is accepted by a DRIVER
     * This is for the passenger who posted the request
     */
    public static void showRideAcceptedByDriverNotification(
            Context context,
            String driverName,
            String driverPhone,
            String pickupLocation,
            String dropLocation,
            double fare,
            String requestId
    ) {
        Log.d(TAG, "🚗 Showing PASSENGER notification: Driver " + driverName + " accepted");

        // Create intent to open MyRidesActivity when notification is tapped
        Intent intent = new Intent(context, MyRidesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("openAsPassenger", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        String title = "🎉 Driver Accepted Your Ride!";
        String message = driverName + " accepted your ride request!\n" +
                "📍 From: " + pickupLocation + "\n" +
                "🎯 To: " + dropLocation + "\n" +
                "💰 Fare: ৳" + String.format("%.0f", fare);

        if (driverPhone != null && !driverPhone.isEmpty()) {
            message += "\n📞 " + driverPhone;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(driverName + " accepted your ride!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 500, 200, 500})
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int notificationId = NOTIFICATION_ID_BASE + Math.abs(requestId.hashCode());
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "✅ PASSENGER notification shown with ID: " + notificationId);
        }
    }

    /**
     * Show notification when a DRIVER's posted ride is accepted by a PASSENGER
     * This is for the driver who posted the ride offer
     */
    public static void showRideAcceptedByPassengerNotification(
            Context context,
            String passengerName,
            String passengerPhone,
            String pickupLocation,
            String dropLocation,
            double fare,
            String requestId
    ) {
        Log.d(TAG, "👤 Showing DRIVER notification: Passenger " + passengerName + " accepted");

        // Create intent to open MyRidesActivity when notification is tapped
        Intent intent = new Intent(context, MyRidesActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("openAsDriver", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        String title = "🎉 Passenger Accepted Your Ride!";
        String message = passengerName + " accepted your ride offer!\n" +
                "📍 From: " + pickupLocation + "\n" +
                "🎯 To: " + dropLocation + "\n" +
                "💰 Fare: ৳" + String.format("%.0f", fare);

        if (passengerPhone != null && !passengerPhone.isEmpty()) {
            message += "\n📞 " + passengerPhone;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(passengerName + " accepted your ride!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 500, 200, 500})
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        // Show notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int notificationId = NOTIFICATION_ID_BASE + Math.abs(requestId.hashCode());
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "✅ DRIVER notification shown with ID: " + notificationId);
        }
    }

    /**
     * Play notification sound
     */
    public static void playNotificationSound(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }
}
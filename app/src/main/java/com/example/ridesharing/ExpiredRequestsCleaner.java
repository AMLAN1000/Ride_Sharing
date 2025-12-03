package com.example.ridesharing;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ExpiredRequestsCleaner {
    private static final String TAG = "ExpiredCleaner";

    /**
     * Cleans up expired pending ride requests.
     * This should be called when the app starts or when viewing ride lists.
     */
    public static void cleanExpiredPendingRequests() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "Starting cleanup of expired pending requests...");

        // Query all pending requests
        db.collection("ride_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int deletedCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Long departureTime = document.getLong("departureTime");

                        // If departure time has passed, delete the request
                        if (departureTime != null && departureTime < currentTime) {
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Deleted expired request: " + document.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to delete expired request: " + document.getId(), e);
                                    });
                            deletedCount++;
                        }
                    }

                    Log.d(TAG, "Cleanup complete. Deleted " + deletedCount + " expired requests.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error during cleanup", e);
                });
    }

    /**
     * Cleans up a specific expired request by ID.
     * Call this when you detect an expired request in your UI.
     */
    public static void cleanSpecificExpiredRequest(String requestId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("ride_requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        Long departureTime = documentSnapshot.getLong("departureTime");
                        long currentTime = System.currentTimeMillis();

                        // Only delete if still pending and departure time has passed
                        if ("pending".equals(status) && departureTime != null && departureTime < currentTime) {
                            documentSnapshot.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Deleted expired request: " + requestId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to delete expired request: " + requestId, e);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking request: " + requestId, e);
                });
    }
}
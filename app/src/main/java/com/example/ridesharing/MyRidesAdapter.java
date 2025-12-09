package com.example.ridesharing;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyRidesAdapter extends RecyclerView.Adapter<MyRidesAdapter.ViewHolder> {

    private List<MyRideItem> rides;
    private OnRideItemClickListener listener;

    public interface OnRideItemClickListener {
        void onCallClick(MyRideItem ride);
        void onMessageClick(MyRideItem ride);
        void onViewDetailsClick(MyRideItem ride);
        void onCompleteRideClick(MyRideItem ride);
        void onCancelRideClick(MyRideItem ride);
        void onTrackClick(MyRideItem ride);
        void onSafetyClick(MyRideItem ride);
    }

    public MyRidesAdapter(List<MyRideItem> rides, OnRideItemClickListener listener) {
        this.rides = rides;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_ride, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyRideItem ride = rides.get(position);

        // Set status
        String status = ride.getStatus();
        if ("accepted".equals(status)) {
            holder.chipStatus.setText("✅ ONGOING");
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
        } else if ("completed".equals(status)) {
            holder.chipStatus.setText("✔ COMPLETED");
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        } else if ("pending".equals(status)) {
            holder.chipStatus.setText("⏳ PENDING");
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
        } else if ("cancelled".equals(status)) {
            holder.chipStatus.setText("❌ CANCELLED");
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
        } else {
            holder.chipStatus.setText(status.toUpperCase());
            holder.chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        }

        // Set time
        Long time = ride.getAcceptedAt() != null ? ride.getAcceptedAt() : ride.getDepartureTime();
        if (time != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
            holder.tvDepartureTime.setText(sdf.format(new Date(time)));
        } else {
            holder.tvDepartureTime.setText("");
        }

        // Show carpool info if applicable
        if (ride.isCarpool()) {
            holder.tvCarpoolInfo.setVisibility(View.VISIBLE);
            holder.tvCarpoolInfo.setText("🚗 Carpool • " + ride.getPassengerCount() + "/" + ride.getMaxSeats() + " seats");
        } else {
            holder.tvCarpoolInfo.setVisibility(View.GONE);
        }

        // Set role label
        String roleLabel = ride.isPassengerView() ? "Driver:" : "Passenger" + (ride.getPassengerCount() > 1 ? "s:" : ":");
        holder.tvRoleLabel.setText(roleLabel);
        holder.tvOtherPersonName.setText(ride.getOtherPersonName());

        // Handle phone display
        if (ride.getOtherPersonPhone() != null && !ride.getOtherPersonPhone().isEmpty() &&
                !ride.getOtherPersonPhone().equals(ride.getOtherPersonName())) {
            holder.tvOtherPersonPhone.setVisibility(View.VISIBLE);
            holder.tvOtherPersonPhone.setText("📞 " + ride.getOtherPersonPhone());
        } else {
            holder.tvOtherPersonPhone.setVisibility(View.GONE);
        }

        // For DRIVER view in CARPOOL with multiple passengers, show summary
        if (!ride.isPassengerView() && ride.isCarpool() && ride.getPassengerDetails() != null && ride.getPassengerDetails().size() > 1) {
            holder.tvOtherPersonName.setText(ride.getPassengerCount() + " Passengers");
            holder.tvOtherPersonPhone.setVisibility(View.GONE);
            holder.tvCarpoolInfo.setText("🚗 Carpool • Tap 'Details' to see each passenger's stops");
        }

        // Set route
        holder.tvPickupLocation.setText(ride.getPickupLocation());
        holder.tvDropLocation.setText(ride.getDropLocation());

        // Set fare
        holder.tvFare.setText("৳" + String.format(Locale.getDefault(), "%.0f", ride.getFare()));

        // Set vehicle info
        String vehicleInfo = (ride.getVehicleType() != null ? ride.getVehicleType().toUpperCase() : "CAR") +
                " • " + ride.getPassengers() + " seat" + (ride.getPassengers() > 1 ? "s" : "");
        holder.tvVehicleInfo.setText(vehicleInfo);

        // Handle unread messages badge
        if (ride.getUnreadCount() > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(ride.getUnreadCount()));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // Show/hide buttons based on status
        boolean isOngoing = "accepted".equals(status);
        boolean isPending = "pending".equals(status);

        // Message button - only for ongoing rides
        holder.btnMessage.setEnabled(isOngoing);
        holder.btnMessage.setAlpha(isOngoing ? 1.0f : 0.5f);

        // Track button - only for ongoing rides
        holder.btnTrack.setVisibility(isOngoing ? View.VISIBLE : View.GONE);

        // ===== SAFETY BUTTON - ONLY FOR PASSENGERS IN ONGOING RIDES =====
        if (ride.isPassengerView() && isOngoing) {
            // Show safety button only for passengers in ongoing rides
            holder.btnSafety.setVisibility(View.VISIBLE);
            holder.btnSafety.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSafetyClick(ride);
                }
            });
        } else {
            // Hide safety button for drivers and non-ongoing rides
            holder.btnSafety.setVisibility(View.GONE);
        }

        // ===== COMPLETE/START BUTTON LOGIC FOR DRIVER =====
        if (!ride.isPassengerView()) {
            if (isOngoing) {
                // Show "Complete Ride" for ongoing rides
                holder.btnComplete.setVisibility(View.VISIBLE);
                holder.btnComplete.setText("Complete");
                holder.btnComplete.setOnClickListener(v -> listener.onCompleteRideClick(ride));
            } else if (isPending && ride.isCarpool() && ride.getPassengerCount() > 0) {
                // Show "Start Ride" for pending carpools with passengers
                holder.btnComplete.setVisibility(View.VISIBLE);
                holder.btnComplete.setText("Start (" + ride.getPassengerCount() + "/" + ride.getMaxSeats() + ")");
                holder.btnComplete.setOnClickListener(v -> showStartRideDialog(v.getContext(), ride));
            } else {
                holder.btnComplete.setVisibility(View.GONE);
            }
        } else {
            holder.btnComplete.setVisibility(View.GONE);
        }

        // Cancel button - only for ongoing or pending rides (driver only)
        holder.btnCancel.setVisibility((isOngoing || isPending) && !ride.isPassengerView() ? View.VISIBLE : View.GONE);

        // Set other click listeners
        holder.btnCall.setOnClickListener(v -> listener.onCallClick(ride));
        holder.btnMessage.setOnClickListener(v -> listener.onMessageClick(ride));
        holder.btnViewDetails.setOnClickListener(v -> listener.onViewDetailsClick(ride));
        holder.btnCancel.setOnClickListener(v -> listener.onCancelRideClick(ride));
        holder.btnTrack.setOnClickListener(v -> listener.onTrackClick(ride));
        // Note: Safety button click listener is already set above conditionally
    }

    // Add this helper method inside MyRidesAdapter class
    private void showStartRideDialog(android.content.Context context, MyRideItem ride) {
        int seatsLeft = ride.getMaxSeats() - ride.getPassengerCount();

        new AlertDialog.Builder(context)
                .setTitle("Start Ride Now?")
                .setMessage("You have " + ride.getPassengerCount() + " passenger(s) and " +
                        seatsLeft + " empty seat(s).\n\n" +
                        "Starting the ride will:\n" +
                        "• Close this carpool to new passengers\n" +
                        "• Begin the journey\n\n" +
                        "Continue?")
                .setPositiveButton("Yes, Start", (dialog, which) -> {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("ride_requests")
                            .document(ride.getId())
                            .update(
                                    "status", "accepted",
                                    "startedEarly", true,
                                    "startedAt", System.currentTimeMillis(),
                                    "acceptedAt", System.currentTimeMillis()
                            )
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context,
                                        "✅ Ride started with " + ride.getPassengerCount() + " passenger(s)!",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context,
                                        "Failed to start ride: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return rides != null ? rides.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        Chip chipStatus;
        TextView tvRoleLabel, tvOtherPersonName, tvCarpoolInfo;
        TextView tvPickupLocation, tvDropLocation;
        TextView tvFare, tvVehicleInfo, tvDepartureTime;
        TextView tvOtherPersonPhone;
        MaterialButton btnCall, btnMessage, btnViewDetails, btnComplete, btnCancel, btnTrack, btnSafety;
        TextView tvUnreadBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_my_ride);
            chipStatus = itemView.findViewById(R.id.chip_status);
            tvRoleLabel = itemView.findViewById(R.id.tv_role_label);
            tvOtherPersonName = itemView.findViewById(R.id.tv_other_person_name);
            tvPickupLocation = itemView.findViewById(R.id.tv_pickup_location);
            tvDropLocation = itemView.findViewById(R.id.tv_drop_location);
            tvFare = itemView.findViewById(R.id.tv_fare);
            tvVehicleInfo = itemView.findViewById(R.id.tv_vehicle_info);
            tvDepartureTime = itemView.findViewById(R.id.tv_departure_time);
            tvOtherPersonPhone = itemView.findViewById(R.id.tv_other_person_phone);
            tvCarpoolInfo = itemView.findViewById(R.id.tv_carpool_info);
            btnCall = itemView.findViewById(R.id.btn_call);
            btnMessage = itemView.findViewById(R.id.btn_message);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnTrack = itemView.findViewById(R.id.btn_track);
            btnSafety = itemView.findViewById(R.id.btn_safety);
            tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
        }
    }
}
package com.example.ridesharing;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        holder.bind(ride, listener);
    }

    @Override
    public int getItemCount() {
        return rides != null ? rides.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private Chip chipStatus;
        private TextView tvRoleLabel, tvOtherPersonName, tvCarpoolInfo;
        private TextView tvPickupLocation, tvDropLocation;
        private TextView tvFare, tvVehicleInfo, tvDepartureTime;
        private TextView tvOtherPersonPhone;
        private MaterialButton btnCall, btnMessage, btnViewDetails, btnComplete, btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize all views with null checks
            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void bind(MyRideItem ride, OnRideItemClickListener listener) {
            if (ride == null) return;

            // Status chip
            String status = ride.getStatus();
            if ("accepted".equals(status)) {
                chipStatus.setText("✅ ONGOING");
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                if (cardView != null) cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            } else if ("completed".equals(status)) {
                chipStatus.setText("✓ COMPLETED");
                chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
                if (cardView != null) cardView.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            } else if ("pending".equals(status)) {
                chipStatus.setText("⏳ PENDING");
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                if (cardView != null) cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            } else if ("cancelled".equals(status)) {
                chipStatus.setText("❌ CANCELLED");
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light);
                if (cardView != null) cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            }

            // Role label
            if (tvRoleLabel != null) {
                String roleLabel = ride.getRoleLabel();
                tvRoleLabel.setText(roleLabel + ":");
            }

            // Person name
            if (tvOtherPersonName != null) {
                if (ride.isCarpool() && !ride.isPassengerView()) {
                    // For drivers viewing carpools, show passenger count
                    tvOtherPersonName.setText(ride.getPassengerCount() + " passenger" +
                            (ride.getPassengerCount() != 1 ? "s" : ""));
                } else {
                    tvOtherPersonName.setText(ride.getOtherPersonName() != null ?
                            ride.getOtherPersonName() : "Unknown");
                }
            }

            // Trip locations
            if (tvPickupLocation != null) {
                tvPickupLocation.setText(ride.getPickupLocation() != null ?
                        ride.getPickupLocation() : "Pickup location");
            }
            if (tvDropLocation != null) {
                tvDropLocation.setText(ride.getDropLocation() != null ?
                        ride.getDropLocation() : "Drop location");
            }

            // Fare display
            if (tvFare != null) {
                if (ride.isCarpool() && ride.getFarePerPassenger() > 0) {
                    tvFare.setText(String.format(Locale.getDefault(), "৳%.0f each", ride.getFarePerPassenger()));
                } else {
                    tvFare.setText(String.format(Locale.getDefault(), "৳%.0f", ride.getFare()));
                }
            }

            // Vehicle info
            if (tvVehicleInfo != null) {
                String vehicleInfo = (ride.getVehicleType() != null ?
                        ride.getVehicleType().toUpperCase() : "CAR");

                if (ride.isCarpool()) {
                    vehicleInfo += " • " + ride.getPassengerCount() + "/" + ride.getMaxSeats() + " seats";
                } else {
                    vehicleInfo += " • " + ride.getPassengers() + " passenger" +
                            (ride.getPassengers() > 1 ? "s" : "");
                }
                tvVehicleInfo.setText(vehicleInfo);
            }

            // Departure time
            if (tvDepartureTime != null && ride.getDepartureTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                tvDepartureTime.setText(sdf.format(new Date(ride.getDepartureTime())));
            }

            // Phone number
            if (tvOtherPersonPhone != null) {
                String phone = ride.getOtherPersonPhone();
                boolean isPhoneNumber = phone != null && !phone.isEmpty() &&
                        !phone.equals(ride.getOtherPersonName()) &&
                        !phone.matches(".*[a-zA-Z].*");

                if (isPhoneNumber) {
                    tvOtherPersonPhone.setText("📞 " + phone);
                    tvOtherPersonPhone.setVisibility(View.VISIBLE);
                } else {
                    tvOtherPersonPhone.setVisibility(View.GONE);
                }
            }

            // Carpool info
            if (tvCarpoolInfo != null) {
                if (ride.isCarpool()) {
                    tvCarpoolInfo.setText("🚗 Carpool • " + ride.getPassengerCount() + "/" + ride.getMaxSeats() + " seats");
                    tvCarpoolInfo.setVisibility(View.VISIBLE);
                } else {
                    tvCarpoolInfo.setVisibility(View.GONE);
                }
            }

            // Button click listeners
            if (btnCall != null) {
                btnCall.setOnClickListener(v -> {
                    if (listener != null) listener.onCallClick(ride);
                });
            }

            if (btnMessage != null) {
                btnMessage.setOnClickListener(v -> {
                    if (listener != null) listener.onMessageClick(ride);
                });
            }

            if (btnViewDetails != null) {
                btnViewDetails.setOnClickListener(v -> {
                    if (listener != null) listener.onViewDetailsClick(ride);
                });
            }

            if (btnComplete != null) {
                btnComplete.setOnClickListener(v -> {
                    if (listener != null) listener.onCompleteRideClick(ride);
                });
            }

            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> {
                    if (listener != null) listener.onCancelRideClick(ride);
                });
            }

            // Button visibility logic
            if (btnComplete != null && btnCancel != null && btnCall != null && btnMessage != null) {
                boolean isPassenger = ride.isPassengerView();
                String statusLower = status.toLowerCase();

                // Complete button - only for drivers on accepted rides
                btnComplete.setVisibility(!isPassenger && "accepted".equals(statusLower) ? View.VISIBLE : View.GONE);

                // Cancel button - only for drivers on pending/accepted rides
                boolean showCancelButton = !isPassenger &&
                        ("pending".equals(statusLower) || "accepted".equals(statusLower));
                btnCancel.setVisibility(showCancelButton ? View.VISIBLE : View.GONE);
                btnCancel.setText("pending".equals(statusLower) ? "Cancel Offer" : "Cancel Ride");

                // Call button - only when there's a valid phone number
                String phone = ride.getOtherPersonPhone();
                boolean showCallButton = phone != null && !phone.isEmpty() &&
                        !phone.equals(ride.getOtherPersonName()) &&
                        !phone.matches(".*[a-zA-Z].*");
                btnCall.setVisibility(showCallButton ? View.VISIBLE : View.GONE);

                // Message button - show for accepted (ongoing) rides
                btnMessage.setVisibility("accepted".equals(statusLower) ? View.VISIBLE : View.GONE);
            }
        }
    }
}
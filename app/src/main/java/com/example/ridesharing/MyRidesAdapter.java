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
        holder.bind(rides.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private Chip chipStatus;
        private TextView tvRoleLabel;
        private TextView tvOtherPersonName;
        private TextView tvPickupLocation, tvDropLocation;
        private TextView tvFare, tvVehicleInfo, tvDepartureTime;
        private TextView tvOtherPersonPhone;
        private MaterialButton btnCall, btnMessage, btnViewDetails, btnComplete;

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
            btnCall = itemView.findViewById(R.id.btn_call);
            btnMessage = itemView.findViewById(R.id.btn_message);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
            btnComplete = itemView.findViewById(R.id.btn_complete);
        }

        public void bind(MyRideItem ride, OnRideItemClickListener listener) {
            // Status chip
            String status = ride.getStatus();
            if ("accepted".equals(status)) {
                chipStatus.setText("✅ ONGOING");
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            } else if ("completed".equals(status)) {
                chipStatus.setText("✓ COMPLETED");
                chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
            }

            // Role label (Driver or Passenger)
            String roleLabel = ride.getRoleLabel();
            tvRoleLabel.setText(roleLabel + ":");
            tvOtherPersonName.setText(ride.getOtherPersonName());

            // Trip details
            tvPickupLocation.setText(ride.getPickupLocation());
            tvDropLocation.setText(ride.getDropLocation());
            tvFare.setText(String.format(Locale.getDefault(), "৳%.0f", ride.getFare()));

            // Vehicle info
            String vehicleInfo = (ride.getVehicleType() != null ?
                    ride.getVehicleType().toUpperCase() : "CAR") +
                    " • " + ride.getPassengers() + " passenger" +
                    (ride.getPassengers() > 1 ? "s" : "");
            tvVehicleInfo.setText(vehicleInfo);

            // Departure time
            if (ride.getDepartureTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                tvDepartureTime.setText(sdf.format(new Date(ride.getDepartureTime())));
            }

            // Phone
            if (ride.getOtherPersonPhone() != null && !ride.getOtherPersonPhone().isEmpty()) {
                tvOtherPersonPhone.setText("📞 " + ride.getOtherPersonPhone());
                tvOtherPersonPhone.setVisibility(View.VISIBLE);
            } else {
                tvOtherPersonPhone.setVisibility(View.GONE);
            }

            // Click listeners
            btnCall.setOnClickListener(v -> listener.onCallClick(ride));
            btnMessage.setOnClickListener(v -> listener.onMessageClick(ride));
            btnViewDetails.setOnClickListener(v -> listener.onViewDetailsClick(ride));
            btnComplete.setOnClickListener(v -> listener.onCompleteRideClick(ride));

            // Only show complete button for ongoing rides
            btnComplete.setVisibility("accepted".equals(status) ? View.VISIBLE : View.GONE);
        }
    }
}
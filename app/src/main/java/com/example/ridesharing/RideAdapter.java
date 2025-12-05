package com.example.ridesharing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private OnRideClickListener listener;

    public interface OnRideClickListener {
        void onRideRequestClick(Ride ride);
        void onProfileViewClick(Ride ride);
        void onRideCallClick(Ride ride);
        void onRideMessageClick(Ride ride);
    }

    public RideAdapter(List<Ride> rideList, OnRideClickListener listener) {
        this.rideList = rideList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_card, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rideList.get(position);

        // Set data to views
        holder.driverName.setText(ride.getDriverName());
        holder.driverRating.setText(String.format("%.1f ⭐", ride.getRating()));
        holder.tvRideType.setText("🚗 Ride");

        // Fare information
        holder.rideFare.setText("৳" + String.format("%.0f", ride.getFare()));

        // Trip information
        holder.sourceText.setText(ride.getSource());
        holder.destinationText.setText(ride.getDestination());
        holder.departureTime.setText(ride.getDepartureTime());

        // Seats information
        holder.tvSeatsInfo.setText(ride.getAvailableSeats() + " seats");

        // Vehicle info
        holder.chipVehicleType.setText(ride.getVehicleModel());
        holder.chipPassengers.setText(ride.getAvailableSeats() + " available");

        // Distance
        if (ride.getDistance() > 0) {
            holder.tvDistance.setText(String.format("%.1f km", ride.getDistance()));
        } else {
            holder.tvDistance.setText("-- km");
        }

        // Fairness chip
        if (ride.isFareFair()) {
            holder.chipFairness.setText("Fair");
            holder.chipFairness.setChipBackgroundColorResource(android.R.color.holo_green_light);
        } else {
            holder.chipFairness.setText("High");
            holder.chipFairness.setChipBackgroundColorResource(android.R.color.holo_red_light);
        }

        // Set click listeners
        holder.requestRideButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRideRequestClick(ride);
            }
        });

        holder.viewProfileButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileViewClick(ride);
            }
        });

        holder.ivCall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRideCallClick(ride);
            }
        });

        holder.ivMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRideMessageClick(ride);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    public void updateRides(List<Ride> newRideList) {
        this.rideList = newRideList;
        notifyDataSetChanged();
    }

    // ViewHolder class
    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverRating, tvRideType;
        TextView rideFare, tvSeatsInfo;
        TextView sourceText, destinationText, departureTime, tvDistance;
        Chip chipVehicleType, chipPassengers, chipFairness;
        MaterialButton requestRideButton, viewProfileButton;
        ImageView ivCall, ivMessage;
        TextView driverIcon;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);

            // Driver info
            driverName = itemView.findViewById(R.id.driver_name);
            driverRating = itemView.findViewById(R.id.driver_rating);
            tvRideType = itemView.findViewById(R.id.tv_ride_type);
            driverIcon = itemView.findViewById(R.id.driver_icon);

            // Contact buttons
            ivCall = itemView.findViewById(R.id.iv_call);
            ivMessage = itemView.findViewById(R.id.iv_message);

            // Fare info
            rideFare = itemView.findViewById(R.id.ride_fare);

            // Trip info
            sourceText = itemView.findViewById(R.id.source_text);
            destinationText = itemView.findViewById(R.id.destination_text);
            departureTime = itemView.findViewById(R.id.departure_time);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvSeatsInfo = itemView.findViewById(R.id.tv_seats_info);

            // Chips
            chipVehicleType = itemView.findViewById(R.id.chip_vehicle_type);
            chipPassengers = itemView.findViewById(R.id.chip_passengers);
            chipFairness = itemView.findViewById(R.id.chip_fairness);

            // Action buttons
            requestRideButton = itemView.findViewById(R.id.request_ride_button);
            viewProfileButton = itemView.findViewById(R.id.view_profile_button);
        }
    }
}
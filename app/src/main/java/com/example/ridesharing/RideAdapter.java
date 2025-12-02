package com.example.ridesharing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private OnRideClickListener listener;

    public interface OnRideClickListener {
        void onRideRequestClick(Ride ride);
        void onProfileViewClick(Ride ride);
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
        holder.vehicleInfo.setText(ride.getVehicleModel() + " • " + ride.getAvailableSeats() + " seats available");
        holder.driverRating.setText(String.valueOf(ride.getRating()));
        holder.rideFare.setText("৳" + ride.getFare());
        holder.sourceText.setText(ride.getSource());
        holder.destinationText.setText(ride.getDestination());
        holder.departureTime.setText(ride.getDepartureTime());
        holder.arrivalTime.setText(ride.getArrivalTime());

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
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    // Method to update the ride list
    public void updateRides(List<Ride> newRideList) {
        this.rideList = newRideList;
        notifyDataSetChanged(); // Refresh the RecyclerView
    }

    // ViewHolder class
    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView driverName, vehicleInfo, driverRating, rideFare;
        TextView sourceText, destinationText, departureTime, arrivalTime;
        Button requestRideButton, viewProfileButton;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            driverName = itemView.findViewById(R.id.driver_name);
            vehicleInfo = itemView.findViewById(R.id.vehicle_info);
            driverRating = itemView.findViewById(R.id.driver_rating);
            rideFare = itemView.findViewById(R.id.ride_fare);
            sourceText = itemView.findViewById(R.id.source_text);
            destinationText = itemView.findViewById(R.id.destination_text);
            departureTime = itemView.findViewById(R.id.departure_time);
            arrivalTime = itemView.findViewById(R.id.arrival_time);
            requestRideButton = itemView.findViewById(R.id.request_ride_button);
            viewProfileButton = itemView.findViewById(R.id.view_profile_button);
        }
    }
}
package com.example.ridesharing;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.util.List;
import java.util.Locale;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.RideRequestViewHolder> {

    private List<RideRequest> requests;
    private OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onAcceptRequestClick(RideRequest request);
        void onPassengerProfileClick(RideRequest request);
        void onMessagePassengerClick(RideRequest request);
        void onCallPassengerClick(RideRequest request);
        void onViewMapClick(RideRequest request);
    }

    public RideRequestAdapter(List<RideRequest> requests, OnRequestClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_request_professional, parent, false);
        return new RideRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideRequestViewHolder holder, int position) {
        RideRequest request = requests.get(position);
        holder.bind(request, listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<RideRequest> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    static class RideRequestViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView ivPassengerPhoto;
        private TextView tvPassengerName;
        private TextView tvRating;
        private TextView tvUserType;
        private ImageView ivCall;
        private ImageView ivMessage;

        private TextView tvPickupLocation;
        private TextView tvDropLocation;
        private ImageView ivRouteArrow;
        private TextView tvDistance;

        private TextView tvFareAmount;
        private Chip chipFairness;
        private TextView tvFareLabel;

        private TextView tvDepartureTime;
        private TextView tvTimeRemaining;
        private Chip chipVehicleType;
        private Chip chipPassengers;

        private TextView tvSpecialRequest;
        private View layoutSpecialRequest;

        private MaterialButton btnAccept;
        private MaterialButton btnViewMap;

        public RideRequestViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.card_ride_request);

            // Passenger info
            ivPassengerPhoto = itemView.findViewById(R.id.iv_passenger_photo);
            tvPassengerName = itemView.findViewById(R.id.tv_passenger_name);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvUserType = itemView.findViewById(R.id.tv_user_type);
            ivCall = itemView.findViewById(R.id.iv_call);
            ivMessage = itemView.findViewById(R.id.iv_message);

            // Trip details
            tvPickupLocation = itemView.findViewById(R.id.tv_pickup_location);
            tvDropLocation = itemView.findViewById(R.id.tv_drop_location);
            ivRouteArrow = itemView.findViewById(R.id.iv_route_arrow);
            tvDistance = itemView.findViewById(R.id.tv_distance);

            // Fare info
            tvFareAmount = itemView.findViewById(R.id.tv_fare_amount);
            chipFairness = itemView.findViewById(R.id.chip_fairness);
            tvFareLabel = itemView.findViewById(R.id.tv_fare_label);

            // Time and vehicle
            tvDepartureTime = itemView.findViewById(R.id.tv_departure_time);
            tvTimeRemaining = itemView.findViewById(R.id.tv_time_remaining);
            chipVehicleType = itemView.findViewById(R.id.chip_vehicle_type);
            chipPassengers = itemView.findViewById(R.id.chip_passengers);

            // Special request
            tvSpecialRequest = itemView.findViewById(R.id.tv_special_request);
            layoutSpecialRequest = itemView.findViewById(R.id.layout_special_request);

            // Action buttons
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnViewMap = itemView.findViewById(R.id.btn_view_map);
        }

        public void bind(RideRequest request, OnRequestClickListener listener) {
            // Load passenger photo
            if (request.getPassengerPhoto() != null && !request.getPassengerPhoto().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(request.getPassengerPhoto())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .into(ivPassengerPhoto);
            } else {
                ivPassengerPhoto.setImageResource(R.drawable.ic_person_placeholder);
            }

            // Passenger details
            tvPassengerName.setText(request.getPassengerName());
            tvRating.setText(String.format(Locale.getDefault(), "%.1f ⭐", request.getRating()));
            tvUserType.setText(request.getUserType());

            // Trip details
            tvPickupLocation.setText(request.getSource());
            tvDropLocation.setText(request.getDestination());

            if (request.getDistance() != null) {
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", request.getDistance()));
                tvDistance.setVisibility(View.VISIBLE);
            } else {
                tvDistance.setVisibility(View.GONE);
            }

            // Fare information
            tvFareAmount.setText(String.format(Locale.getDefault(), "৳%.0f", request.getOfferedFare()));

            // Fairness indicator (you can add logic based on calculated fair fare)
            chipFairness.setText("Fair Price");
            chipFairness.setChipBackgroundColorResource(android.R.color.holo_green_light);
            chipFairness.setTextColor(Color.WHITE);

            // Time information
            tvDepartureTime.setText(request.getDepartureTime());
            tvTimeRemaining.setText(request.getTimeRemaining());

            // Vehicle type
            if (request.getVehicleType() != null) {
                chipVehicleType.setText(request.getVehicleType().toUpperCase());
                chipVehicleType.setVisibility(View.VISIBLE);

                // Different colors for different vehicles
                if (request.getVehicleType().equals("car")) {
                    chipVehicleType.setChipBackgroundColorResource(android.R.color.holo_blue_light);
                } else {
                    chipVehicleType.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                }
            } else {
                chipVehicleType.setVisibility(View.GONE);
            }

            // Passengers count
            chipPassengers.setText(request.getPassengers() + " passenger" +
                    (request.getPassengers() > 1 ? "s" : ""));

            // Special request
            if (request.getSpecialRequest() != null && !request.getSpecialRequest().trim().isEmpty()) {
                tvSpecialRequest.setText(request.getSpecialRequest());
                layoutSpecialRequest.setVisibility(View.VISIBLE);
            } else {
                layoutSpecialRequest.setVisibility(View.GONE);
            }

            // Click listeners
            ivPassengerPhoto.setOnClickListener(v -> listener.onPassengerProfileClick(request));
            tvPassengerName.setOnClickListener(v -> listener.onPassengerProfileClick(request));

            ivCall.setOnClickListener(v -> listener.onCallPassengerClick(request));
            ivMessage.setOnClickListener(v -> listener.onMessagePassengerClick(request));

            btnAccept.setOnClickListener(v -> listener.onAcceptRequestClick(request));
            btnViewMap.setOnClickListener(v -> listener.onViewMapClick(request));

            // Card click for details
            cardView.setOnClickListener(v -> listener.onPassengerProfileClick(request));
        }
    }
}
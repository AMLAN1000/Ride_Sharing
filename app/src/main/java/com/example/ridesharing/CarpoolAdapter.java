package com.example.ridesharing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import java.util.List;
import java.util.Map;

public class CarpoolAdapter extends RecyclerView.Adapter<CarpoolAdapter.CarpoolViewHolder> {

    private List<Carpool> carpoolList;
    private OnCarpoolClickListener listener;

    public interface OnCarpoolClickListener {
        void onCarpoolRequestClick(Carpool carpool);
        void onCarpoolProfileViewClick(Carpool carpool);
        void onCarpoolCallClick(Carpool carpool);
        void onCarpoolMessageClick(Carpool carpool);
    }

    public CarpoolAdapter(List<Carpool> carpoolList, OnCarpoolClickListener listener) {
        this.carpoolList = carpoolList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarpoolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carpool_card, parent, false); // Use new layout
        return new CarpoolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarpoolViewHolder holder, int position) {
        Carpool carpool = carpoolList.get(position);

        // Set data to views
        holder.driverName.setText(carpool.getDriverName());
        holder.driverRating.setText(String.format("%.1f ⭐", carpool.getRating()));
        holder.tvCarpoolType.setText("🚗 Carpool");

        // Fare information
        holder.farePerPassenger.setText("৳" + String.format("%.0f", carpool.getFarePerPassenger()));
        holder.tvTotalFare.setText("Total: ৳" + String.format("%.0f", carpool.getTotalFare()));

        // Trip information
        holder.sourceText.setText(carpool.getSource());
        holder.destinationText.setText(carpool.getDestination());
        holder.departureTime.setText(carpool.getDepartureTime());

        // Distance information
        if (holder.tvDistance != null) {
            holder.tvDistance.setText(String.format("%.1f km", carpool.getDistance()));
        }

        // Show route stops if available
        LinearLayout layoutRouteStops = holder.itemView.findViewById(R.id.layout_route_stops);
        TextView tvRouteStops = holder.itemView.findViewById(R.id.tv_route_stops);

        if (layoutRouteStops != null && tvRouteStops != null) {
            // Fetch route stops from Firestore
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("ride_requests")
                    .document(carpool.getId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            List<Map<String, Object>> routeStops =
                                    (List<Map<String, Object>>) document.get("routeStops");

                            if (routeStops != null && routeStops.size() > 2) {
                                // Build stops text
                                StringBuilder stopsText = new StringBuilder();
                                for (int i = 0; i < routeStops.size(); i++) {
                                    Map<String, Object> stop = routeStops.get(i);
                                    String address = (String) stop.get("address");

                                    if (i == 0) {
                                        stopsText.append("🟢 Start: ").append(address);
                                    } else if (i == routeStops.size() - 1) {
                                        stopsText.append("\n🔴 End: ").append(address);
                                    } else {
                                        stopsText.append("\n🟡 Stop ").append(i).append(": ").append(address);
                                    }
                                }

                                tvRouteStops.setText(stopsText.toString());
                                layoutRouteStops.setVisibility(View.VISIBLE);
                            } else {
                                layoutRouteStops.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> layoutRouteStops.setVisibility(View.GONE));
        }

        // Seats information
        int seatsLeft = carpool.getMaxSeats() - carpool.getPassengerCount();
        holder.tvSeatsInfo.setText(carpool.getPassengerCount() + "/" + carpool.getMaxSeats() + " filled");

        // Vehicle info
        holder.chipVehicleType.setText(carpool.getVehicleModel());
        holder.chipPassengers.setText(seatsLeft + " seat" + (seatsLeft != 1 ? "s" : "") + " left");

        // Set click listeners
        holder.joinCarpoolButton.setOnClickListener(v -> {
            if (listener != null && holder.joinCarpoolButton.isEnabled()) {
                listener.onCarpoolRequestClick(carpool);
            }
        });

        holder.viewProfileButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCarpoolProfileViewClick(carpool);
            }
        });

        holder.ivCall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCarpoolCallClick(carpool);
            }
        });

        holder.ivMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCarpoolMessageClick(carpool);
            }
        });

        // Handle button state based on seat availability
        if (seatsLeft <= 0) {
            holder.joinCarpoolButton.setText("Full");
            holder.joinCarpoolButton.setEnabled(false);
            holder.joinCarpoolButton.setBackgroundColor(0xFFCCCCCC); // Grey
            holder.joinCarpoolButton.setTextColor(0xFF666666);
        } else {
            holder.joinCarpoolButton.setText("Join Carpool");
            holder.joinCarpoolButton.setEnabled(true);
            holder.joinCarpoolButton.setBackgroundColor(0xFF4CAF50); // Green
            holder.joinCarpoolButton.setTextColor(0xFFFFFFFF);
        }
    }

    @Override
    public int getItemCount() {
        return carpoolList.size();
    }

    public void updateCarpools(List<Carpool> newCarpoolList) {
        this.carpoolList = newCarpoolList;
        notifyDataSetChanged();
    }

    static class CarpoolViewHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverRating, tvCarpoolType;
        TextView farePerPassenger, tvTotalFare;
        TextView sourceText, destinationText, departureTime, tvSeatsInfo, tvDistance;
        Chip chipVehicleType, chipPassengers;
        MaterialButton joinCarpoolButton, viewProfileButton;
        ImageView ivCall, ivMessage;
        TextView driverIcon;

        public CarpoolViewHolder(@NonNull View itemView) {
            super(itemView);

            // Driver info
            driverName = itemView.findViewById(R.id.driver_name);
            driverRating = itemView.findViewById(R.id.driver_rating);
            tvCarpoolType = itemView.findViewById(R.id.tv_carpool_type);
            driverIcon = itemView.findViewById(R.id.driver_icon);

            // Contact buttons
            ivCall = itemView.findViewById(R.id.iv_call);
            ivMessage = itemView.findViewById(R.id.iv_message);

            // Fare info
            farePerPassenger = itemView.findViewById(R.id.fare_per_passenger);
            tvTotalFare = itemView.findViewById(R.id.tv_total_fare);

            // Trip info
            sourceText = itemView.findViewById(R.id.source_text);
            destinationText = itemView.findViewById(R.id.destination_text);
            departureTime = itemView.findViewById(R.id.departure_time);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvSeatsInfo = itemView.findViewById(R.id.tv_seats_info);

            // Chips
            chipVehicleType = itemView.findViewById(R.id.chip_vehicle_type);
            chipPassengers = itemView.findViewById(R.id.chip_passengers);

            // Action buttons
            joinCarpoolButton = itemView.findViewById(R.id.join_carpool_button);
            viewProfileButton = itemView.findViewById(R.id.view_profile_button);
        }
    }
}
package com.example.ridesharing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.RequestViewHolder> {

    private List<RideRequest> requestList;
    private OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onAcceptRequestClick(RideRequest request);
        void onPassengerProfileClick(RideRequest request);
        void onMessagePassengerClick(RideRequest request);
    }

    public RideRequestAdapter(List<RideRequest> requestList, OnRequestClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    public void updateRequests(List<RideRequest> newRequests) {
        this.requestList = newRequests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RideRequest request = requestList.get(position);
        holder.bind(request, listener);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        private TextView passengerNameText, passengerTypeText, ratingText;
        private TextView sourceText, destinationText, timeText, fareText;
        private TextView passengersCountText, specialRequestText;
        private Button acceptButton, profileButton, messageButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views based on item_ride_request.xml
            passengerNameText = itemView.findViewById(R.id.passenger_name_text);
            passengerTypeText = itemView.findViewById(R.id.passenger_type_text);
            ratingText = itemView.findViewById(R.id.rating_text);
            sourceText = itemView.findViewById(R.id.source_text);
            destinationText = itemView.findViewById(R.id.destination_text);
            timeText = itemView.findViewById(R.id.time_text);
            fareText = itemView.findViewById(R.id.fare_text);
            passengersCountText = itemView.findViewById(R.id.passengers_count_text);
            specialRequestText = itemView.findViewById(R.id.special_request_text);
            acceptButton = itemView.findViewById(R.id.accept_button);
            profileButton = itemView.findViewById(R.id.profile_button);
            messageButton = itemView.findViewById(R.id.message_button);
        }

        public void bind(RideRequest request, OnRequestClickListener listener) {
            passengerNameText.setText(request.getPassengerName());
            passengerTypeText.setText(request.getPassengerType());
            ratingText.setText(String.valueOf(request.getRating()));
            sourceText.setText(request.getSource());
            destinationText.setText(request.getDestination());
            timeText.setText(request.getPreferredTime() + " - " + request.getEstimatedArrival());
            fareText.setText("৳" + request.getOfferedFare());
            passengersCountText.setText(request.getPassengersCount() + " passenger(s)");
            specialRequestText.setText(request.getSpecialRequest());

            acceptButton.setOnClickListener(v -> listener.onAcceptRequestClick(request));
            profileButton.setOnClickListener(v -> listener.onPassengerProfileClick(request));
            messageButton.setOnClickListener(v -> listener.onMessagePassengerClick(request));
        }
    }
}
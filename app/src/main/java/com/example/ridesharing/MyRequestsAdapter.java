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

public class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.ViewHolder> {

    private List<MyRideRequest> requests;
    private OnMyRequestClickListener listener;

    public interface OnMyRequestClickListener {
        void onCallDriverClick(MyRideRequest request);
        void onCancelRequestClick(MyRideRequest request);
        void onViewDetailsClick(MyRideRequest request);
    }

    public MyRequestsAdapter(List<MyRideRequest> requests, OnMyRequestClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private Chip chipStatus;
        private TextView tvPickupLocation, tvDropLocation, tvFare;
        private TextView tvVehicleInfo, tvDepartureTime, tvDriverInfo;
        private View layoutDriverInfo;
        private MaterialButton btnCallDriver, btnCancel, btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_my_request);
            chipStatus = itemView.findViewById(R.id.chip_status);
            tvPickupLocation = itemView.findViewById(R.id.tv_pickup_location);
            tvDropLocation = itemView.findViewById(R.id.tv_drop_location);
            tvFare = itemView.findViewById(R.id.tv_fare);
            tvVehicleInfo = itemView.findViewById(R.id.tv_vehicle_info);
            tvDepartureTime = itemView.findViewById(R.id.tv_departure_time);
            tvDriverInfo = itemView.findViewById(R.id.tv_driver_info);
            layoutDriverInfo = itemView.findViewById(R.id.layout_driver_info);
            btnCallDriver = itemView.findViewById(R.id.btn_call_driver);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        public void bind(MyRideRequest request, OnMyRequestClickListener listener) {
            String status = request.getStatus();
            chipStatus.setText(status.toUpperCase());

            if ("pending".equals(status)) {
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                chipStatus.setText("⏳ PENDING");
            } else if ("accepted".equals(status)) {
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light);
                chipStatus.setText("✅ ACCEPTED");
                cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            }

            tvPickupLocation.setText(request.getPickupLocation());
            tvDropLocation.setText(request.getDropLocation());
            tvFare.setText(String.format(Locale.getDefault(), "৳%.0f", request.getFare()));

            String vehicleInfo = (request.getVehicleType() != null ?
                    request.getVehicleType().toUpperCase() : "CAR") +
                    " • " + request.getPassengers() + " passenger" +
                    (request.getPassengers() > 1 ? "s" : "");
            tvVehicleInfo.setText(vehicleInfo);

            if (request.getDepartureTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                tvDepartureTime.setText(sdf.format(new Date(request.getDepartureTime())));
            }

            if ("accepted".equals(status) && request.getDriverName() != null) {
                layoutDriverInfo.setVisibility(View.VISIBLE);
                String driverInfo = "Driver: " + request.getDriverName();
                if (request.getDriverPhone() != null) {
                    driverInfo += "\n📞 " + request.getDriverPhone();
                }
                tvDriverInfo.setText(driverInfo);
                btnCallDriver.setVisibility(View.VISIBLE);
            } else {
                layoutDriverInfo.setVisibility(View.GONE);
                btnCallDriver.setVisibility(View.GONE);
            }

            btnCallDriver.setOnClickListener(v -> listener.onCallDriverClick(request));
            btnCancel.setOnClickListener(v -> listener.onCancelRequestClick(request));
            btnViewDetails.setOnClickListener(v -> listener.onViewDetailsClick(request));

            btnCancel.setVisibility("pending".equals(status) ? View.VISIBLE : View.GONE);
        }
    }
}
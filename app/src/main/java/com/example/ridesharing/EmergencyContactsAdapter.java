package com.example.ridesharing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder> {

    private List<EmergencyContact> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onCallClick(EmergencyContact contact);
        void onShareLocationClick(EmergencyContact contact);
    }

    public EmergencyContactsAdapter(List<EmergencyContact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.bind(contact, listener);
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvPhone, tvRelationship, tvPrimaryBadge;
        private MaterialButton btnCall, btnShareLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvRelationship = itemView.findViewById(R.id.tv_contact_relationship);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
            btnCall = itemView.findViewById(R.id.btn_call_contact);
            btnShareLocation = itemView.findViewById(R.id.btn_share_location_contact);
        }

        public void bind(EmergencyContact contact, OnContactClickListener listener) {
            tvName.setText(contact.getName());
            tvPhone.setText(contact.getPhoneNumber());
            tvRelationship.setText(contact.getRelationship());

            if (contact.isPrimary()) {
                tvPrimaryBadge.setVisibility(View.VISIBLE);
            } else {
                tvPrimaryBadge.setVisibility(View.GONE);
            }

            btnCall.setOnClickListener(v -> {
                if (listener != null) listener.onCallClick(contact);
            });

            btnShareLocation.setOnClickListener(v -> {
                if (listener != null) listener.onShareLocationClick(contact);
            });
        }
    }
}
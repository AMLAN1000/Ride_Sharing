package com.example.ridesharing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ManageContactsAdapter extends RecyclerView.Adapter<ManageContactsAdapter.ViewHolder> {

    private List<EmergencyContact> contacts;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onEditClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact);
        void onSetPrimaryClick(EmergencyContact contact);
    }

    public ManageContactsAdapter(List<EmergencyContact> contacts, OnContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_contact, parent, false);
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
        private ImageButton btnEdit, btnDelete;
        private MaterialButton btnSetPrimary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvRelationship = itemView.findViewById(R.id.tv_contact_relationship);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnSetPrimary = itemView.findViewById(R.id.btn_set_primary);
        }

        public void bind(EmergencyContact contact, OnContactActionListener listener) {
            tvName.setText(contact.getName());
            tvPhone.setText(contact.getPhoneNumber());
            tvRelationship.setText(contact.getRelationship());

            if (contact.isPrimary()) {
                tvPrimaryBadge.setVisibility(View.VISIBLE);
                btnSetPrimary.setVisibility(View.GONE);
            } else {
                tvPrimaryBadge.setVisibility(View.GONE);
                btnSetPrimary.setVisibility(View.VISIBLE);
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(contact);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(contact);
            });

            btnSetPrimary.setOnClickListener(v -> {
                if (listener != null) listener.onSetPrimaryClick(contact);
            });
        }
    }
}
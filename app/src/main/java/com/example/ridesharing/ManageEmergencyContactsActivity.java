package com.example.ridesharing;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageEmergencyContactsActivity extends AppCompatActivity {

    private static final String TAG = "ManageContacts";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyState;
    private MaterialButton btnAddContact;

    private ManageContactsAdapter adapter;
    private List<EmergencyContact> contactsList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_emergency_contacts);

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        loadContacts();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_contacts);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        btnAddContact = findViewById(R.id.btn_add_contact);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnAddContact.setOnClickListener(v -> showAddContactDialog());
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageContactsAdapter(contactsList, new ManageContactsAdapter.OnContactActionListener() {
            @Override
            public void onEditClick(EmergencyContact contact) {
                showEditContactDialog(contact);
            }

            @Override
            public void onDeleteClick(EmergencyContact contact) {
                showDeleteConfirmation(contact);
            }

            @Override
            public void onSetPrimaryClick(EmergencyContact contact) {
                setPrimaryContact(contact);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadContacts() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    contactsList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        EmergencyContact contact = document.toObject(EmergencyContact.class);
                        contact.setId(document.getId());
                        contactsList.add(contact);
                    }

                    adapter.notifyDataSetChanged();

                    if (contactsList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading contacts", e);
                    Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddContactDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_emergency_contact, null);

        EditText etName = dialogView.findViewById(R.id.et_contact_name);
        EditText etPhone = dialogView.findViewById(R.id.et_contact_phone);
        Spinner spinnerRelationship = dialogView.findViewById(R.id.spinner_relationship);
        SwitchMaterial switchPrimary = dialogView.findViewById(R.id.switch_primary);

        new AlertDialog.Builder(this)
                .setTitle("Add Emergency Contact")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String relationship = spinnerRelationship.getSelectedItem().toString();
                    boolean isPrimary = switchPrimary.isChecked();

                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addContact(name, phone, relationship, isPrimary);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditContactDialog(EmergencyContact contact) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_emergency_contact, null);

        EditText etName = dialogView.findViewById(R.id.et_contact_name);
        EditText etPhone = dialogView.findViewById(R.id.et_contact_phone);
        Spinner spinnerRelationship = dialogView.findViewById(R.id.spinner_relationship);
        SwitchMaterial switchPrimary = dialogView.findViewById(R.id.switch_primary);

        // Pre-fill existing data
        etName.setText(contact.getName());
        etPhone.setText(contact.getPhoneNumber());
        switchPrimary.setChecked(contact.isPrimary());

        new AlertDialog.Builder(this)
                .setTitle("Edit Emergency Contact")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String relationship = spinnerRelationship.getSelectedItem().toString();
                    boolean isPrimary = switchPrimary.isChecked();

                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateContact(contact.getId(), name, phone, relationship, isPrimary);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addContact(String name, String phone, String relationship, boolean isPrimary) {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> contactData = new HashMap<>();
        contactData.put("name", name);
        contactData.put("phoneNumber", phone);
        contactData.put("relationship", relationship);
        contactData.put("isPrimary", isPrimary);

        db.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .add(contactData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "✅ Contact added", Toast.LENGTH_SHORT).show();
                    loadContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding contact", e);
                    Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateContact(String contactId, String name, String phone, String relationship, boolean isPrimary) {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> contactData = new HashMap<>();
        contactData.put("name", name);
        contactData.put("phoneNumber", phone);
        contactData.put("relationship", relationship);
        contactData.put("isPrimary", isPrimary);

        db.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contactId)
                .update(contactData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Contact updated", Toast.LENGTH_SHORT).show();
                    loadContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating contact", e);
                    Toast.makeText(this, "Failed to update contact", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact?")
                .setMessage("Are you sure you want to delete " + contact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteContact(contact))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteContact(EmergencyContact contact) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contact.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Contact deleted", Toast.LENGTH_SHORT).show();
                    loadContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting contact", e);
                    Toast.makeText(this, "Failed to delete contact", Toast.LENGTH_SHORT).show();
                });
    }

    private void setPrimaryContact(EmergencyContact contact) {
        String userId = mAuth.getCurrentUser().getUid();

        // First, remove primary from all contacts
        for (EmergencyContact c : contactsList) {
            db.collection("users")
                    .document(userId)
                    .collection("emergency_contacts")
                    .document(c.getId())
                    .update("isPrimary", false);
        }

        // Then set this one as primary
        db.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contact.getId())
                .update("isPrimary", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Primary contact set", Toast.LENGTH_SHORT).show();
                    loadContacts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error setting primary", e);
                    Toast.makeText(this, "Failed to set primary contact", Toast.LENGTH_SHORT).show();
                });
    }
}
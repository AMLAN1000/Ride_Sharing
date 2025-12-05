package com.example.ridesharing;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private TextView tvOtherPersonName;
    private TextView tvRideInfo;
    private TextView tvEmptyState;

    private ChatAdapter adapter;
    private List<ChatMessage> messagesList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration messagesListener;

    private String rideId;
    private String currentUserId;
    private String currentUserName;
    private String otherPersonName;
    private String otherPersonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        rideId = getIntent().getStringExtra("rideId");
        otherPersonName = getIntent().getStringExtra("otherPersonName");
        otherPersonId = getIntent().getStringExtra("otherPersonId");
        String pickupLocation = getIntent().getStringExtra("pickupLocation");
        String dropLocation = getIntent().getStringExtra("dropLocation");

        if (rideId == null) {
            Toast.makeText(this, "Error: Ride ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        loadCurrentUserInfo();
        setupClickListeners();

        // Load other person's name if we have their ID
        if (otherPersonId != null && !otherPersonId.isEmpty()) {
            loadOtherPersonName();
        }

        loadMessages();

        // Set ride info
        if (tvOtherPersonName != null) {
            tvOtherPersonName.setText(otherPersonName != null ? otherPersonName : "Chat");
        }
        if (tvRideInfo != null && pickupLocation != null && dropLocation != null) {
            tvRideInfo.setText(pickupLocation + " → " + dropLocation);
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        progressBar = findViewById(R.id.progress_bar);
        tvOtherPersonName = findViewById(R.id.tv_other_person_name);
        tvRideInfo = findViewById(R.id.tv_ride_info);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
    }

    private void loadCurrentUserInfo() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Try fullName first, then fall back to name
                        currentUserName = document.getString("fullName");
                        if (currentUserName == null || currentUserName.isEmpty()) {
                            currentUserName = document.getString("name");
                        }
                        if (currentUserName == null || currentUserName.isEmpty()) {
                            currentUserName = "User";
                        }
                        Log.d(TAG, "✅ Current user name loaded: " + currentUserName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user info", e);
                    currentUserName = "User";
                });
    }

    private void loadOtherPersonName() {
        if (otherPersonId == null || otherPersonId.isEmpty()) return;

        db.collection("users").document(otherPersonId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Try fullName first, then fall back to name
                        String name = document.getString("fullName");
                        if (name == null || name.isEmpty()) {
                            name = document.getString("name");
                        }
                        if (name != null && !name.isEmpty()) {
                            otherPersonName = name;
                            if (tvOtherPersonName != null) {
                                tvOtherPersonName.setText(otherPersonName);
                            }
                            Log.d(TAG, "✅ Other person name loaded: " + otherPersonName);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading other person info", e));
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerView.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(messagesList, currentUserId);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        // Also send on enter key
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadMessages() {
        if (rideId == null || currentUserId == null) return;

        showLoading();

        // Listen to messages in real-time
        messagesListener = db.collection("ride_requests")
                .document(rideId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    hideLoading();

                    if (error != null) {
                        Log.e(TAG, "Error loading messages", error);
                        Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    messagesList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            try {
                                ChatMessage message = doc.toObject(ChatMessage.class);
                                if (message != null) {
                                    message.setMessageId(doc.getId());

                                    // Ensure sender name is not null or empty
                                    if (message.getSenderName() == null || message.getSenderName().trim().isEmpty()) {
                                        message.setSenderName("User");
                                    }

                                    messagesList.add(message);

                                    // Mark message as read if it's not from me
                                    if (!message.getSenderId().equals(currentUserId) && !message.isRead()) {
                                        markMessageAsRead(doc.getId());
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing message", e);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // Scroll to bottom
                    if (!messagesList.isEmpty()) {
                        recyclerView.smoothScrollToPosition(messagesList.size() - 1);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Please wait, loading user info...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure we have a valid sender name
        if (currentUserName == null || currentUserName.trim().isEmpty()) {
            currentUserName = "User";
        }

        // Disable send button
        btnSend.setEnabled(false);

        // Create message object
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("senderName", currentUserName);
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());
        message.put("isRead", false);

        Log.d(TAG, "📤 Sending message with senderName: " + currentUserName);

        // Save to Firestore
        db.collection("ride_requests")
                .document(rideId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Message sent: " + documentReference.getId());
                    etMessage.setText("");
                    btnSend.setEnabled(true);

                    // Update last message in ride document
                    updateLastMessage(messageText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                });
    }

    private void updateLastMessage(String messageText) {
        // Update the ride document with last message info for notification purposes
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", messageText);
        updates.put("lastMessageTime", System.currentTimeMillis());
        updates.put("lastMessageSenderId", currentUserId);

        db.collection("ride_requests")
                .document(rideId)
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update last message", e));
    }

    private void markMessageAsRead(String messageId) {
        db.collection("ride_requests")
                .document(rideId)
                .collection("messages")
                .document(messageId)
                .update("isRead", true)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark message as read", e));
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}
package com.example.ridesharing;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final String TAG = "ChatAdapter";
    private List<ChatMessage> messages;
    private String currentUserId;
    private FirebaseFirestore db;
    private Map<String, String> userNameCache = new HashMap<>();

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message, currentUserId, db, userNameCache);
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout messageContainer;
        private TextView tvMessage;
        private TextView tvTimestamp;
        private TextView tvSenderName;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.message_container);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
        }

        public void bind(ChatMessage message, String currentUserId, FirebaseFirestore db, Map<String, String> userNameCache) {
            boolean isSentByMe = message.getSenderId().equals(currentUserId);

            // Set message text
            tvMessage.setText(message.getMessage());

            // Set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(message.getTimestamp())));

            // Set sender name (only show for received messages)
            if (isSentByMe) {
                tvSenderName.setVisibility(View.GONE);
            } else {
                tvSenderName.setVisibility(View.VISIBLE);

                // Get sender name from message
                String senderName = message.getSenderName();
                String senderId = message.getSenderId();

                // If sender name exists and is not "User", use it
                if (senderName != null && !senderName.trim().isEmpty() && !senderName.equals("User")) {
                    tvSenderName.setText(senderName);
                    // Cache it for future use
                    if (!userNameCache.containsKey(senderId)) {
                        userNameCache.put(senderId, senderName);
                        Log.d(TAG, "✅ Cached name from message: " + senderName);
                    }
                } else {
                    // Check cache first
                    if (userNameCache.containsKey(senderId)) {
                        String cachedName = userNameCache.get(senderId);
                        tvSenderName.setText(cachedName);
                        Log.d(TAG, "📦 Using cached name for " + senderId + ": " + cachedName);
                    } else {
                        // Set temporary name
                        tvSenderName.setText("Loading...");

                        // Fetch from Firestore
                        db.collection("users").document(senderId)
                                .get()
                                .addOnSuccessListener(document -> {
                                    if (document.exists()) {
                                        // Try fullName first, then fall back to name
                                        String fetchedName = document.getString("fullName");
                                        if (fetchedName == null || fetchedName.trim().isEmpty()) {
                                            fetchedName = document.getString("name");
                                        }

                                        String displayName;
                                        if (fetchedName != null && !fetchedName.trim().isEmpty()) {
                                            displayName = fetchedName.trim();
                                        } else {
                                            displayName = "User " + senderId.substring(0, Math.min(4, senderId.length()));
                                        }

                                        // Cache it
                                        userNameCache.put(senderId, displayName);

                                        // Update UI
                                        tvSenderName.setText(displayName);
                                        Log.d(TAG, "✅ Fetched and cached name for " + senderId + ": " + displayName);
                                    } else {
                                        String fallbackName = "User " + senderId.substring(0, Math.min(4, senderId.length()));
                                        userNameCache.put(senderId, fallbackName);
                                        tvSenderName.setText(fallbackName);
                                        Log.w(TAG, "⚠️ User document not found for " + senderId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Error fetching user name for " + senderId, e);
                                    String fallbackName = "User " + senderId.substring(0, Math.min(4, senderId.length()));
                                    userNameCache.put(senderId, fallbackName);
                                    tvSenderName.setText(fallbackName);
                                });
                    }
                }
            }

            // Align message based on sender
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();
            if (isSentByMe) {
                // My messages - align right, blue background
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.bg_message_sent);
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                tvTimestamp.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
            } else {
                // Their messages - align left, gray background
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.bg_message_received);
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                tvTimestamp.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
                tvSenderName.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark));
            }
            messageContainer.setLayoutParams(params);
        }
    }
}
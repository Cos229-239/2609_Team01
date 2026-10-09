package com.example.gamingteamfinder;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConversationActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String friendUid;
    private String friendName;
    private String conversationId;

    private TextView textFriendAvatar;
    private TextView textFriendName;
    private TextView textFriendStatus;
    private TextView textStartConversation;

    private EditText editMessage;
    private LinearLayout conversationContainer;
    private ScrollView messagesScrollView;

    private boolean messageListenerStarted = false;
    private ListenerRegistration messageListenerRegistration;
    private ListenerRegistration friendStatusListener;
    private ListenerRegistration readReceiptListener;
    private Timestamp friendLastReadAt;
    private boolean hasRenderedMessage = false;
    private int messageLoadVersion = 0;
    private boolean isConversationVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Button buttonBack =
                findViewById(R.id.buttonBack);

        Button buttonSend =
                findViewById(R.id.buttonSend);

        editMessage =
                findViewById(R.id.editMessage);

        conversationContainer =
                findViewById(R.id.conversationContainer);

        messagesScrollView =
                findViewById(R.id.messagesScrollView);

        textStartConversation =
                findViewById(R.id.textStartConversation);

        textFriendAvatar =
                findViewById(R.id.textFriendAvatar);

        textFriendName =
                findViewById(R.id.textFriendName);

        textFriendStatus =
                findViewById(R.id.textFriendStatus);


        friendUid =
                getIntent().getStringExtra("FRIEND_UID");

        friendName =
                getIntent().getStringExtra("FRIEND_NAME");


        buttonBack.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ConversationActivity.this,
                    MessagesActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });


        if (friendName != null
                && !friendName.trim().isEmpty()) {

            textFriendName.setText(friendName);

            textFriendAvatar.setText(
                    getInitials(friendName)
            );
        }


        if (friendUid != null
                && !friendUid.isEmpty()) {

            prepareConversation();
        }


        buttonSend.setOnClickListener(v ->
                sendMessage()
        );
    }


    private void prepareConversation() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String currentUid =
                currentUser.getUid();

        if (currentUid.compareTo(friendUid) < 0) {

            conversationId =
                    currentUid + "_" + friendUid;

        } else {

            conversationId =
                    friendUid + "_" + currentUid;
        }


        db.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!isConversationVisible) {
                        return;
                    }

                    if (document.exists()) {

                        markConversationAsRead();
                        startMessageListener();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        isConversationVisible = true;

        startFriendStatusListener();
        startReadReceiptListener();

        if (conversationId != null
                && !messageListenerStarted) {

            startMessageListener();
            markConversationAsRead();
        }
    }

    @Override
    protected void onStop() {

        isConversationVisible = false;

        if (messageListenerRegistration != null) {

            messageListenerRegistration.remove();
            messageListenerRegistration = null;
        }

        if (friendStatusListener != null) {

            friendStatusListener.remove();
            friendStatusListener = null;
        }
        if (readReceiptListener != null) {

            readReceiptListener.remove();
            readReceiptListener = null;
        }

        messageListenerStarted = false;

        // Stop any old async message load
        messageLoadVersion++;

        super.onStop();
    }
    private void sendMessage() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (friendUid == null
                || friendUid.isEmpty()) {

            return;
        }


        String messageText =
                editMessage
                        .getText()
                        .toString()
                        .trim();


        if (messageText.isEmpty()) {
            return;
        }


        String currentUid =
                currentUser.getUid();


        // Create conversation ID
        if (conversationId == null) {

            if (currentUid.compareTo(friendUid) < 0) {

                conversationId =
                        currentUid + "_" + friendUid;

            } else {

                conversationId =
                        friendUid + "_" + currentUid;
            }
        }


        // Current day ID
        String dayId =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(
                        Calendar.getInstance().getTime()
                );


        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(currentUserDocument -> {

                    String currentName =
                            currentUserDocument.getString(
                                    "displayName"
                            );


                    if (currentName == null
                            || currentName.trim().isEmpty()) {

                        currentName = "Player";
                    }


                    String safeFriendName =
                            friendName;


                    if (safeFriendName == null
                            || safeFriendName.trim().isEmpty()) {

                        safeFriendName = "Player";
                    }


                    String user1Uid;
                    String user1Name;

                    String user2Uid;
                    String user2Name;


                    if (currentUid.compareTo(friendUid) < 0) {

                        user1Uid = currentUid;
                        user1Name = currentName;

                        user2Uid = friendUid;
                        user2Name = safeFriendName;

                    } else {

                        user1Uid = friendUid;
                        user1Name = safeFriendName;

                        user2Uid = currentUid;
                        user2Name = currentName;
                    }


                    // Determine who is sending
                    String sender;

                    if (currentUid.equals(user1Uid)) {

                        sender = "user1";

                    } else {

                        sender = "user2";
                    }

                    String unreadField;

                    if (currentUid.equals(user1Uid)) {

                        unreadField = "user2UnreadCount";

                    } else {

                        unreadField = "user1UnreadCount";
                    }


                    // =========================
                    // Conversation summary
                    // =========================

                    Map<String, Object> conversation =
                            new HashMap<>();


                    conversation.put(
                            "members",
                            Arrays.asList(
                                    user1Uid,
                                    user2Uid
                            )
                    );

                    conversation.put(
                            "user1Uid",
                            user1Uid
                    );

                    conversation.put(
                            "user1Name",
                            user1Name
                    );

                    conversation.put(
                            "user2Uid",
                            user2Uid
                    );

                    conversation.put(
                            "user2Name",
                            user2Name
                    );
                    conversation.put(
                            "lastMessageSenderUid",
                            currentUid
                    );

                    conversation.put(
                            "updatedAt",
                            FieldValue.serverTimestamp()
                    );

                    conversation.put(
                            unreadField,
                            FieldValue.increment(1)
                    );


                    String finalSender =
                            sender;


                    // Save conversation
                    db.collection("conversations")
                            .document(conversationId)
                            .set(
                                    conversation,
                                    SetOptions.merge()
                            )
                            .addOnSuccessListener(unused -> {

                                // =========================
                                // Message
                                // =========================

                                Map<String, Object> message =
                                        new HashMap<>();

                                message.put(
                                        "sender",
                                        finalSender
                                );

                                message.put(
                                        "text",
                                        messageText
                                );

                                message.put(
                                        "createdAt",
                                        FieldValue.serverTimestamp()
                                );


                                db.collection("conversations")
                                        .document(conversationId)
                                        .collection("days")
                                        .document(dayId)
                                        .collection("messages")
                                        .add(message)
                                        .addOnSuccessListener(messageDocument -> {

                                            // =========================
                                            // Day document
                                            // =========================

                                            Map<String, Object> dayData =
                                                    new HashMap<>();

                                            dayData.put(
                                                    "date",
                                                    dayId
                                            );

                                            dayData.put(
                                                    "lastMessage",
                                                    messageText
                                            );

                                            dayData.put(
                                                    "updatedAt",
                                                    FieldValue.serverTimestamp()
                                            );


                                            db.collection("conversations")
                                                    .document(conversationId)
                                                    .collection("days")
                                                    .document(dayId)
                                                    .set(
                                                            dayData,
                                                            SetOptions.merge()
                                                    )
                                                    .addOnSuccessListener(dayUnused -> {

                                                        editMessage.setText("");

                                                        if (!messageListenerStarted) {
                                                            startMessageListener();
                                                        }

                                                    })
                                                    .addOnFailureListener(e -> {

                                                        Toast.makeText(
                                                                ConversationActivity.this,
                                                                "Failed to update day: " + e.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show();

                                                    });

                                        })
                                        .addOnFailureListener(e -> {

                                            Toast.makeText(
                                                    ConversationActivity.this,
                                                    "Failed to send message: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();

                                        });

                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        ConversationActivity.this,
                                        "Failed to create conversation.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            });

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ConversationActivity.this,
                            "Failed to load your profile.",
                            Toast.LENGTH_SHORT
                    ).show();

                });
    }

    private void startMessageListener() {

        if (!isConversationVisible
                || messageListenerStarted
                || conversationId == null) {

            return;
        }

        messageListenerStarted = true;


        messageListenerRegistration =
                db.collection("conversations")
                        .document(conversationId)
                        .collection("days")
                        .orderBy(
                                "date",
                                Query.Direction.ASCENDING
                        )
                        .addSnapshotListener((daySnapshots, error) -> {

                    if (error != null
                            || daySnapshots == null) {

                        return;
                    }


                    // Every refresh gets a new version
                    int currentLoadVersion =
                            ++messageLoadVersion;


                    conversationContainer.removeAllViews();

                    hasRenderedMessage = false;


                    if (daySnapshots.isEmpty()) {

                        conversationContainer.addView(
                                textStartConversation
                        );

                        return;
                    }


                    loadDayMessages(
                            daySnapshots.getDocuments(),
                            0,
                            currentLoadVersion
                    );

                });
    }

    private void loadDayMessages(
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> dayDocuments,
            int index,
            int loadVersion
    ) {

        // Stop an old load if a newer refresh already started
        if (loadVersion != messageLoadVersion) {
            return;
        }


        if (index >= dayDocuments.size()) {

            if (!hasRenderedMessage) {

                conversationContainer.addView(
                        textStartConversation
                );
            }

            updateSeenReceipt();

            messagesScrollView.post(() ->
                    messagesScrollView.fullScroll(
                            ScrollView.FOCUS_DOWN
                    )
            );

            markConversationAsRead();

            return;
        }


        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }


        String currentUid =
                currentUser.getUid();


        String currentSender;

        if (currentUid.compareTo(friendUid) < 0) {

            currentSender = "user1";

        } else {

            currentSender = "user2";
        }


        com.google.firebase.firestore.DocumentSnapshot dayDocument =
                dayDocuments.get(index);


        dayDocument
                .getReference()
                .collection("messages")
                .orderBy(
                        "createdAt",
                        Query.Direction.ASCENDING
                )
                .get()
                .addOnSuccessListener(messageSnapshots -> {

                    // A newer refresh started while this request was loading
                    if (loadVersion != messageLoadVersion) {
                        return;
                    }


                    if (!messageSnapshots.isEmpty()) {

                        Timestamp firstTimestamp = null;


                        for (com.google.firebase.firestore.DocumentSnapshot document
                                : messageSnapshots.getDocuments()) {

                            Timestamp createdAt =
                                    document.getTimestamp("createdAt");

                            if (createdAt != null) {

                                firstTimestamp = createdAt;
                                break;
                            }
                        }


                        if (firstTimestamp != null) {

                            addDateSeparator(
                                    firstTimestamp
                            );
                        }


                        for (com.google.firebase.firestore.DocumentSnapshot document
                                : messageSnapshots.getDocuments()) {

                            String sender =
                                    document.getString("sender");

                            String text =
                                    document.getString("text");

                            Timestamp createdAt =
                                    document.getTimestamp("createdAt");


                            if (text == null) {
                                continue;
                            }


                            boolean isMine =
                                    currentSender.equals(sender);


                            addMessageBubble(
                                    text,
                                    isMine,
                                    createdAt
                            );


                            hasRenderedMessage = true;
                        }
                    }


                    loadDayMessages(
                            dayDocuments,
                            index + 1,
                            loadVersion
                    );

                })
                .addOnFailureListener(e -> {

                    if (loadVersion != messageLoadVersion) {
                        return;
                    }


                    loadDayMessages(
                            dayDocuments,
                            index + 1,
                            loadVersion
                    );

                });
    }

    private void addMessageBubble(
            String message,
            boolean isMine,
            Timestamp createdAt
    ) {

        LinearLayout messageGroup =
                new LinearLayout(this);

        messageGroup.setOrientation(
                LinearLayout.VERTICAL
        );


        LinearLayout.LayoutParams groupParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        if (isMine) {

            groupParams.gravity =
                    Gravity.END;

            groupParams.setMargins(
                    dp(60),
                    dp(5),
                    0,
                    dp(7)
            );

        } else {

            groupParams.gravity =
                    Gravity.START;

            groupParams.setMargins(
                    0,
                    dp(5),
                    dp(60),
                    dp(7)
            );
        }

        messageGroup.setLayoutParams(
                groupParams
        );

        if (isMine && createdAt != null) {

            messageGroup.setTag(
                    createdAt
            );
        }


        // Message bubble
        TextView bubble =
                new TextView(this);

        bubble.setText(message);
        bubble.setTextSize(15);

        bubble.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
        );


        GradientDrawable background =
                new GradientDrawable();

        background.setCornerRadius(
                dp(18)
        );


        if (isMine) {

            background.setColor(
                    Color.parseColor("#6C4FB3")
            );

            bubble.setTextColor(
                    Color.WHITE
            );

        } else {

            background.setColor(
                    Color.WHITE
            );

            bubble.setTextColor(
                    Color.parseColor("#222222")
            );
        }

        bubble.setBackground(background);


        // Time under message
        TextView timeText =
                new TextView(this);

        timeText.setText(
                formatMessageTime(createdAt)
        );

        timeText.setTextSize(11);

        timeText.setTextColor(
                Color.parseColor("#888888")
        );

        timeText.setPadding(
                dp(6),
                dp(2),
                dp(6),
                0
        );


        if (isMine) {

            timeText.setGravity(
                    Gravity.END
            );

        } else {

            timeText.setGravity(
                    Gravity.START
            );
        }


        messageGroup.addView(bubble);
        messageGroup.addView(timeText);

        conversationContainer.addView(
                messageGroup
        );
    }
    private String formatMessageTime(
            Timestamp timestamp
    ) {

        if (timestamp == null) {
            return "";
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                );

        return formatter.format(
                timestamp.toDate()
        );
    }


    private void addDateSeparator(
            Timestamp timestamp
    ) {

        if (timestamp == null) {
            return;
        }

        Calendar now =
                Calendar.getInstance();

        Calendar messageDate =
                Calendar.getInstance();

        messageDate.setTime(
                timestamp.toDate()
        );

        String label;


        // Today
        if (now.get(Calendar.YEAR)
                == messageDate.get(Calendar.YEAR)

                && now.get(Calendar.DAY_OF_YEAR)
                == messageDate.get(Calendar.DAY_OF_YEAR)) {

            label = "Today";

        } else {

            Calendar yesterday =
                    Calendar.getInstance();

            yesterday.add(
                    Calendar.DAY_OF_YEAR,
                    -1
            );


            // Yesterday
            if (yesterday.get(Calendar.YEAR)
                    == messageDate.get(Calendar.YEAR)

                    && yesterday.get(Calendar.DAY_OF_YEAR)
                    == messageDate.get(Calendar.DAY_OF_YEAR)) {

                label = "Yesterday";

            } else {

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(
                                "M/d/yyyy",
                                Locale.getDefault()
                        );

                label = dateFormat.format(
                        timestamp.toDate()
                );
            }
        }


        TextView dateText =
                new TextView(this);

        dateText.setText(label);

        dateText.setTextSize(12);

        dateText.setTextColor(
                Color.parseColor("#777777")
        );

        dateText.setGravity(
                Gravity.CENTER
        );

        dateText.setPadding(
                0,
                dp(16),
                0,
                dp(10)
        );

        conversationContainer.addView(
                dateText
        );
    }

    private void markConversationAsRead() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (!isConversationVisible
                || currentUser == null
                || conversationId == null) {

            return;
        }


        String currentUid =
                currentUser.getUid();


        db.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!isConversationVisible) {
                        return;
                    }

                    if (!document.exists()) {
                        return;
                    }

                    String user1Uid =
                            document.getString("user1Uid");

                    String unreadField;
                    String lastReadField;


                    if (currentUid.equals(user1Uid)) {

                        unreadField =
                                "user1UnreadCount";

                        lastReadField =
                                "user1LastReadAt";

                    } else {

                        unreadField =
                                "user2UnreadCount";

                        lastReadField =
                                "user2LastReadAt";
                    }


                    Map<String, Object> readUpdates =
                            new HashMap<>();

                    readUpdates.put(
                            unreadField,
                            0L
                    );

                    readUpdates.put(
                            lastReadField,
                            FieldValue.serverTimestamp()
                    );


                    db.collection("conversations")
                            .document(conversationId)
                            .update(readUpdates);
                });
    }

    private void startReadReceiptListener() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (!isConversationVisible
                || currentUser == null
                || conversationId == null) {

            return;
        }


        if (readReceiptListener != null) {
            return;
        }


        String currentUid =
                currentUser.getUid();


        readReceiptListener =
                db.collection("conversations")
                        .document(conversationId)
                        .addSnapshotListener(
                                (document, error) -> {

                                    if (error != null
                                            || document == null
                                            || !document.exists()) {

                                        return;
                                    }


                                    String user1Uid =
                                            document.getString(
                                                    "user1Uid"
                                            );


                                    String friendLastReadField;


                                    if (currentUid.equals(user1Uid)) {

                                        friendLastReadField =
                                                "user2LastReadAt";

                                    } else {

                                        friendLastReadField =
                                                "user1LastReadAt";
                                    }


                                    friendLastReadAt =
                                            document.getTimestamp(
                                                    friendLastReadField
                                            );


                                    updateSeenReceipt();
                                }
                        );
    }

    private void updateSeenReceipt() {

        LinearLayout seenMessageGroup = null;

        Timestamp latestSeenTime = null;


        for (int i = 0;
             i < conversationContainer.getChildCount();
             i++) {

            View child =
                    conversationContainer.getChildAt(i);

            Object tag =
                    child.getTag();


            if (!(child instanceof LinearLayout)
                    || !(tag instanceof Timestamp)) {

                continue;
            }


            LinearLayout messageGroup =
                    (LinearLayout) child;


            // Remove old Seen text if there is one
            while (messageGroup.getChildCount() > 2) {

                messageGroup.removeViewAt(
                        messageGroup.getChildCount() - 1
                );
            }


            Timestamp messageTime =
                    (Timestamp) tag;


            if (friendLastReadAt != null
                    && messageTime.compareTo(
                    friendLastReadAt
            ) <= 0) {

                if (latestSeenTime == null
                        || messageTime.compareTo(
                        latestSeenTime
                ) > 0) {

                    latestSeenTime =
                            messageTime;

                    seenMessageGroup =
                            messageGroup;
                }
            }
        }


        if (seenMessageGroup == null) {
            return;
        }


        TextView seenText =
                new TextView(this);

        seenText.setText("Seen");

        seenText.setTextSize(11);

        seenText.setTextColor(
                Color.parseColor("#6C4FB3")
        );

        seenText.setGravity(
                Gravity.END
        );

        seenText.setPadding(
                dp(6),
                dp(1),
                dp(6),
                0
        );


        seenMessageGroup.addView(
                seenText
        );
    }

    private void startFriendStatusListener() {

        if (friendUid == null
                || friendUid.isEmpty()) {

            return;
        }


        if (friendStatusListener != null) {

            friendStatusListener.remove();
            friendStatusListener = null;
        }


        friendStatusListener =
                db.collection("users")
                        .document(friendUid)
                        .addSnapshotListener(
                                (document, error) -> {

                                    if (error != null
                                            || document == null
                                            || !document.exists()) {

                                        return;
                                    }


                                    Boolean onlineValue =
                                            document.getBoolean(
                                                    "isOnline"
                                            );

                                    boolean isOnline =
                                            Boolean.TRUE.equals(
                                                    onlineValue
                                            );


                                    if (isOnline) {

                                        textFriendStatus.setText(
                                                "● Online"
                                        );

                                        textFriendStatus.setTextColor(
                                                Color.parseColor(
                                                        "#3FAE64"
                                                )
                                        );

                                    } else {

                                        textFriendStatus.setText(
                                                "● Offline"
                                        );

                                        textFriendStatus.setTextColor(
                                                Color.parseColor(
                                                        "#9E9E9E"
                                                )
                                        );
                                    }
                                }
                        );
    }


    private String getInitials(
            String displayName
    ) {

        if (displayName == null
                || displayName.trim().isEmpty()) {

            return "?";
        }


        String[] parts =
                displayName
                        .trim()
                        .split("\\s+");


        if (parts.length >= 2) {

            return parts[0]
                    .substring(0, 1)
                    .toUpperCase()

                    + parts[1]
                    .substring(0, 1)
                    .toUpperCase();
        }


        return displayName
                .substring(0, 1)
                .toUpperCase();
    }


    private int dp(int value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
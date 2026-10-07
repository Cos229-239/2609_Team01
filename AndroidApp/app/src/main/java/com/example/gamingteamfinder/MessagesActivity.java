package com.example.gamingteamfinder;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MessagesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout messagesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        messagesContainer =
                findViewById(R.id.messagesContainer);

        ImageButton buttonHome =
                findViewById(R.id.buttonHome);

        ImageButton buttonSearch =
                findViewById(R.id.buttonSearch);

        ImageButton buttonMessages =
                findViewById(R.id.buttonMessages);

        ImageButton buttonProfile =
                findViewById(R.id.buttonProfile);

        Button buttonBack =
                findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MessagesActivity.this,
                    HomeActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });


        buttonHome.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MessagesActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);
        });


        buttonSearch.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MessagesActivity.this,
                    SearchPlayerActivity.class
            );

            startActivity(intent);
        });


        buttonMessages.setOnClickListener(v -> {
            // Already on Messages screen
        });


        buttonProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MessagesActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        loadConversations();
    }


    private void loadConversations() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }


        String currentUid =
                currentUser.getUid();


        messagesContainer.removeAllViews();


        Map<String, Timestamp> conversationTimes =
                new HashMap<>();


        db.collection("conversations")
                .whereArrayContains(
                        "members",
                        currentUid
                )
                .get()
                .addOnSuccessListener(conversationSnapshots -> {

                    for (QueryDocumentSnapshot document
                            : conversationSnapshots) {

                        List<String> members =
                                (List<String>)
                                        document.get("members");


                        if (members == null) {
                            continue;
                        }


                        String otherUid = null;


                        for (String uid : members) {

                            if (!uid.equals(currentUid)) {

                                otherUid = uid;
                                break;
                            }
                        }


                        if (otherUid == null) {
                            continue;
                        }


                        Timestamp updatedAt =
                                document.getTimestamp(
                                        "updatedAt"
                                );


                        if (updatedAt != null) {

                            conversationTimes.put(
                                    otherUid,
                                    updatedAt
                            );
                        }
                    }


                    loadFriendsForMessages(
                            currentUid,
                            conversationTimes
                    );

                })
                .addOnFailureListener(e -> {

                    loadFriendsForMessages(
                            currentUid,
                            conversationTimes
                    );

                });
    }

    private void loadFriendsForMessages(
            String currentUid,
            Map<String, Timestamp> conversationTimes
    ) {

        db.collection("friends")
                .whereArrayContains(
                        "members",
                        currentUid
                )
                .get()
                .addOnSuccessListener(friendSnapshots -> {

                    if (friendSnapshots.isEmpty()) {

                        showNoMessages();
                        return;
                    }


                    for (QueryDocumentSnapshot friendDocument
                            : friendSnapshots) {

                        List<String> members =
                                (List<String>)
                                        friendDocument.get("members");


                        if (members == null) {
                            continue;
                        }


                        String friendUid = null;


                        for (String uid : members) {

                            if (!uid.equals(currentUid)) {

                                friendUid = uid;
                                break;
                            }
                        }


                        if (friendUid == null) {
                            continue;
                        }


                        String finalFriendUid =
                                friendUid;


                        db.collection("users")
                                .document(friendUid)
                                .get()
                                .addOnSuccessListener(userDocument -> {

                                    if (!userDocument.exists()) {
                                        return;
                                    }


                                    String displayName =
                                            userDocument.getString(
                                                    "displayName"
                                            );


                                    if (displayName == null
                                            || displayName.trim().isEmpty()) {

                                        displayName = "Player";
                                    }


                                    String conversationId;


                                    if (currentUid.compareTo(finalFriendUid) < 0) {

                                        conversationId =
                                                currentUid
                                                        + "_"
                                                        + finalFriendUid;

                                    } else {

                                        conversationId =
                                                finalFriendUid
                                                        + "_"
                                                        + currentUid;
                                    }


                                    Timestamp conversationUpdatedAt =
                                            conversationTimes.get(
                                                    finalFriendUid
                                            );


                                    if (conversationUpdatedAt != null) {

                                        loadLatestDayMessage(
                                                conversationId,
                                                finalFriendUid,
                                                displayName,
                                                conversationUpdatedAt
                                        );

                                    } else {

                                        addConversationRow(
                                                finalFriendUid,
                                                displayName,
                                                "Start a conversation",
                                                null
                                        );
                                    }

                                });
                    }

                })
                .addOnFailureListener(e -> {

                    showNoMessages();

                });
    }

    private void addConversationRow(
            String friendUid,
            String displayName,
            String lastMessage,
            Timestamp lastMessageTime
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
        );


        // Avatar
        TextView avatar =
                new TextView(this);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(58)
                );

        avatarParams.setMargins(
                0,
                0,
                dp(14),
                0
        );

        avatar.setLayoutParams(
                avatarParams
        );

        avatar.setGravity(
                Gravity.CENTER
        );

        avatar.setText(
                getInitials(displayName)
        );

        avatar.setTextSize(18);

        avatar.setTypeface(
                null,
                Typeface.BOLD
        );

        avatar.setTextColor(
                Color.parseColor("#30364F")
        );

        avatar.setBackgroundResource(
                R.drawable.friend_avatar_background
        );


        // Name + last message
        LinearLayout infoSection =
                new LinearLayout(this);

        infoSection.setOrientation(
                LinearLayout.VERTICAL
        );

        infoSection.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );


        TextView nameText =
                new TextView(this);

        nameText.setText(displayName);
        nameText.setTextSize(17);

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        nameText.setTextColor(
                Color.parseColor("#222222")
        );


        TextView messageText =
                new TextView(this);

        messageText.setText(lastMessage);

        messageText.setTextSize(14);

        messageText.setTextColor(
                Color.parseColor("#777777")
        );

        messageText.setPadding(
                0,
                dp(4),
                0,
                0
        );

        TextView timeText =
                new TextView(this);

        timeText.setText(
                formatMessageTime(
                        lastMessageTime
                )
        );

        timeText.setTextSize(12);

        timeText.setTextColor(
                Color.parseColor("#777777")
        );

        timeText.setGravity(
                Gravity.TOP | Gravity.END
        );

        timeText.setPadding(
                dp(8),
                dp(4),
                0,
                0
        );


        infoSection.addView(nameText);
        infoSection.addView(messageText);

        row.addView(avatar);
        row.addView(infoSection);
        row.addView(timeText);

        row.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MessagesActivity.this,
                            ConversationActivity.class
                    );

            intent.putExtra(
                    "FRIEND_UID",
                    friendUid
            );

            intent.putExtra(
                    "FRIEND_NAME",
                    displayName
            );

            startActivity(intent);
        });


        messagesContainer.addView(row);
    }


    private void showNoMessages() {

        TextView emptyText =
                new TextView(this);

        emptyText.setText(
                "No messages yet."
        );

        emptyText.setTextSize(16);

        emptyText.setTextColor(
                Color.parseColor("#777777")
        );

        emptyText.setGravity(
                Gravity.CENTER
        );

        emptyText.setPadding(
                0,
                dp(40),
                0,
                0
        );

        messagesContainer.addView(
                emptyText
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

    private String formatMessageTime(
            Timestamp timestamp
    ) {

        if (timestamp == null) {
            return "";
        }

        Calendar now =
                Calendar.getInstance();

        Calendar messageTime =
                Calendar.getInstance();

        messageTime.setTime(
                timestamp.toDate()
        );


        // Today -> 3:03 PM
        if (now.get(Calendar.YEAR)
                == messageTime.get(Calendar.YEAR)

                && now.get(Calendar.DAY_OF_YEAR)
                == messageTime.get(Calendar.DAY_OF_YEAR)) {

            SimpleDateFormat timeFormat =
                    new SimpleDateFormat(
                            "h:mm a",
                            Locale.getDefault()
                    );

            return timeFormat.format(
                    timestamp.toDate()
            );
        }


        // Yesterday -> Yesterday
        Calendar yesterday =
                Calendar.getInstance();

        yesterday.add(
                Calendar.DAY_OF_YEAR,
                -1
        );

        if (yesterday.get(Calendar.YEAR)
                == messageTime.get(Calendar.YEAR)

                && yesterday.get(Calendar.DAY_OF_YEAR)
                == messageTime.get(Calendar.DAY_OF_YEAR)) {

            return "Yesterday";
        }


        // Within the last 7 days -> Mon, Tue, Wed...
        Calendar sevenDaysAgo =
                Calendar.getInstance();

        sevenDaysAgo.add(
                Calendar.DAY_OF_YEAR,
                -7
        );

        if (messageTime.after(sevenDaysAgo)) {

            SimpleDateFormat dayFormat =
                    new SimpleDateFormat(
                            "EEE",
                            Locale.getDefault()
                    );

            return dayFormat.format(
                    timestamp.toDate()
            );
        }


        // Older -> 10/5
        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "M/d",
                        Locale.getDefault()
                );

        return dateFormat.format(
                timestamp.toDate()
        );
    }

    private void loadLatestDayMessage(
            String conversationId,
            String friendUid,
            String displayName,
            Timestamp conversationUpdatedAt
    ) {

        db.collection("conversations")
                .document(conversationId)
                .collection("days")
                .orderBy(
                        "updatedAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING
                )
                .limit(1)
                .get()
                .addOnSuccessListener(daySnapshots -> {

                    String lastMessage =
                            "Start a conversation";

                    if (!daySnapshots.isEmpty()) {

                        String savedMessage =
                                daySnapshots
                                        .getDocuments()
                                        .get(0)
                                        .getString("lastMessage");

                        if (savedMessage != null
                                && !savedMessage.trim().isEmpty()) {

                            lastMessage =
                                    savedMessage;
                        }
                    }

                    addConversationRow(
                            friendUid,
                            displayName,
                            lastMessage,
                            conversationUpdatedAt
                    );

                })
                .addOnFailureListener(e -> {

                    addConversationRow(
                            friendUid,
                            displayName,
                            "Start a conversation",
                            conversationUpdatedAt
                    );

                });
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
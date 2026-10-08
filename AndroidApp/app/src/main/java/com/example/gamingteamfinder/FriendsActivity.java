package com.example.gamingteamfinder;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout onlineFriendsContainer;
    private LinearLayout offlineFriendsContainer;

    private TextView textOnlineHeader;
    private final List<ListenerRegistration> friendStatusListeners =
            new ArrayList<>();

    private final Map<String, Boolean> friendOnlineStates =
            new HashMap<>();

    private int friendsLoadVersion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Button buttonBack =
                findViewById(R.id.buttonBack);

        textOnlineHeader =
                findViewById(R.id.textOnlineHeader);

        onlineFriendsContainer =
                findViewById(R.id.onlineFriendsContainer);

        offlineFriendsContainer =
                findViewById(R.id.offlineFriendsContainer);

        // Bottom navigation
        ImageButton buttonHome =
                findViewById(R.id.buttonHome);

        ImageButton buttonSearch =
                findViewById(R.id.buttonSearch);

        ImageButton buttonMessages =
                findViewById(R.id.buttonMessages);

        ImageButton buttonProfile =
                findViewById(R.id.buttonProfile);

        buttonBack.setOnClickListener(v -> finish());

        buttonHome.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FriendsActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);
        });

        buttonSearch.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FriendsActivity.this,
                    SearchPlayerActivity.class
            );

            startActivity(intent);
        });

        buttonMessages.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FriendsActivity.this,
                    MessagesActivity.class
            );

            startActivity(intent);
        });

        buttonProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FriendsActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadFriendsList();
    }

    @Override
    protected void onPause() {
        super.onPause();

        clearFriendStatusListeners();

        friendsLoadVersion++;
    }

    private void loadFriendsList() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String currentUid =
                currentUser.getUid();

        int loadVersion =
                ++friendsLoadVersion;

        clearFriendStatusListeners();

        friendOnlineStates.clear();

        onlineFriendsContainer.removeAllViews();
        offlineFriendsContainer.removeAllViews();

        textOnlineHeader.setText("Online (0)");

        db.collection("friends")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener(friendSnapshots -> {


                    if (loadVersion != friendsLoadVersion) {
                        return;
                    }

                    if (friendSnapshots.isEmpty()) {

                        showNoFriends();
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

                        ListenerRegistration statusListener =
                                db.collection("users")
                                        .document(finalFriendUid)
                                        .addSnapshotListener(
                                                (userDocument, error) -> {

                                                    if (loadVersion != friendsLoadVersion) {
                                                        return;
                                                    }

                                                    if (error != null
                                                            || userDocument == null
                                                            || !userDocument.exists()) {

                                                        return;
                                                    }


                                                    String displayName =
                                                            getValue(
                                                                    userDocument.getString(
                                                                            "displayName"
                                                                    )
                                                            );

                                                    String game =
                                                            getValue(
                                                                    userDocument.getString(
                                                                            "game"
                                                                    )
                                                            );

                                                    String rank =
                                                            getValue(
                                                                    userDocument.getString(
                                                                            "rank"
                                                                    )
                                                            );

                                                    String role =
                                                            getValue(
                                                                    userDocument.getString(
                                                                            "role"
                                                                    )
                                                            );


                                                    Boolean onlineValue =
                                                            userDocument.getBoolean(
                                                                    "isOnline"
                                                            );

                                                    boolean isOnline =
                                                            Boolean.TRUE.equals(
                                                                    onlineValue
                                                            );


                                                    // Remove the old version of this friend row
                                                    removeFriendRow(
                                                            finalFriendUid
                                                    );


                                                    // Save newest online state
                                                    friendOnlineStates.put(
                                                            finalFriendUid,
                                                            isOnline
                                                    );


                                                    if (isOnline) {

                                                        addFriendRow(
                                                                onlineFriendsContainer,
                                                                finalFriendUid,
                                                                displayName,
                                                                game,
                                                                rank,
                                                                role,
                                                                true
                                                        );

                                                    } else {

                                                        addFriendRow(
                                                                offlineFriendsContainer,
                                                                finalFriendUid,
                                                                displayName,
                                                                game,
                                                                rank,
                                                                role,
                                                                false
                                                        );
                                                    }


                                                    updateOnlineHeader();
                                                }
                                        );


                        friendStatusListeners.add(
                                statusListener
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    if (loadVersion != friendsLoadVersion) {
                        return;
                    }

                    Toast.makeText(
                            FriendsActivity.this,
                            "Failed to load friends.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void addFriendRow(
            LinearLayout container,
            String friendUid,
            String displayName,
            String game,
            String rank,
            String role,
            boolean isOnline
    ) {

        LinearLayout friendRow =
                new LinearLayout(this);

        friendRow.setTag(
                friendUid
        );

        friendRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        friendRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        friendRow.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
        );

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.setMargins(
                0,
                0,
                0,
                dp(4)
        );

        friendRow.setLayoutParams(rowParams);


        // Avatar
        TextView avatar =
                new TextView(this);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(
                        dp(64),
                        dp(64)
                );

        avatarParams.setMargins(
                0,
                0,
                dp(16),
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

        avatar.setTextSize(20);

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


        // Info
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

        nameText.setText(
                displayName
        );

        nameText.setTextSize(18);

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        nameText.setTextColor(
                Color.parseColor("#222222")
        );


        TextView gameText =
                new TextView(this);

        gameText.setText(
                game + " · " + rank + " · " + role
        );

        gameText.setTextSize(14);

        gameText.setTextColor(
                Color.parseColor("#666666")
        );

        gameText.setPadding(
                0,
                dp(3),
                0,
                0
        );


        TextView statusText =
                new TextView(this);

        if (isOnline) {

            statusText.setText("● Online");

            statusText.setTextColor(
                    Color.parseColor("#3FAE64")
            );

        } else {

            statusText.setText("● Offline");

            statusText.setTextColor(
                    Color.parseColor("#9E9E9E")
            );
        }

        statusText.setTextSize(12);

        statusText.setPadding(
                0,
                dp(4),
                0,
                0
        );


        infoSection.addView(nameText);
        infoSection.addView(gameText);
        infoSection.addView(statusText);
        LinearLayout actionRow =
                new LinearLayout(this);

        actionRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        LinearLayout.LayoutParams actionRowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        actionRowParams.setMargins(
                0,
                dp(8),
                0,
                0
        );

        actionRow.setLayoutParams(actionRowParams);


        // View Profile button
        Button viewProfileButton =
                new Button(this);

        viewProfileButton.setText("Profile");
        viewProfileButton.setTextSize(12);
        viewProfileButton.setAllCaps(false);

        LinearLayout.LayoutParams profileParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                );

        profileParams.setMargins(
                0,
                0,
                dp(6),
                0
        );

        viewProfileButton.setLayoutParams(
                profileParams
        );

        viewProfileButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FriendsActivity.this,
                    PlayerDetailActivity.class
            );

            intent.putExtra(
                    "PLAYER_UID",
                    friendUid
            );

            intent.putExtra(
                    "FROM_FRIENDS",
                    true
            );

            startActivity(intent);
        });


        // Chat button
        Button chatButton =
                new Button(this);

        chatButton.setText("Message");
        chatButton.setTextSize(12);
        chatButton.setAllCaps(false);

        LinearLayout.LayoutParams chatParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1
                );

        chatParams.setMargins(
                dp(6),
                0,
                0,
                0
        );

        chatButton.setLayoutParams(
                chatParams
        );

        chatButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    FriendsActivity.this,
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


        actionRow.addView(viewProfileButton);
        actionRow.addView(chatButton);

        infoSection.addView(actionRow);
        friendRow.addView(avatar);
        friendRow.addView(infoSection);




        container.addView(friendRow);
    }

    private void showNoFriends() {

        TextView noFriends =
                new TextView(this);

        noFriends.setText(
                "No friends yet."
        );

        noFriends.setTextSize(16);

        noFriends.setTextColor(
                Color.parseColor("#777777")
        );

        noFriends.setPadding(
                dp(8),
                dp(20),
                0,
                0
        );

        offlineFriendsContainer.addView(
                noFriends
        );
    }

    private void updateOnlineHeader() {

        int onlineCount = 0;

        for (Boolean isOnline
                : friendOnlineStates.values()) {

            if (Boolean.TRUE.equals(isOnline)) {
                onlineCount++;
            }
        }

        textOnlineHeader.setText(
                "Online (" + onlineCount + ")"
        );
    }


    private void removeFriendRow(
            String friendUid
    ) {

        removeFriendRowFromContainer(
                onlineFriendsContainer,
                friendUid
        );

        removeFriendRowFromContainer(
                offlineFriendsContainer,
                friendUid
        );
    }


    private void removeFriendRowFromContainer(
            LinearLayout container,
            String friendUid
    ) {

        for (int i = container.getChildCount() - 1;
             i >= 0;
             i--) {

            View child =
                    container.getChildAt(i);

            Object tag =
                    child.getTag();

            if (friendUid.equals(tag)) {

                container.removeViewAt(i);
            }
        }
    }


    private void clearFriendStatusListeners() {

        for (ListenerRegistration listener
                : friendStatusListeners) {

            listener.remove();
        }

        friendStatusListeners.clear();
    }
    private String getValue(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Not set";
        }

        return value;
    }

    private String getInitials(
            String displayName
    ) {

        if (displayName == null
                || displayName.trim().isEmpty()
                || displayName.equals("Not set")) {

            return "?";
        }

        String[] parts =
                displayName.trim().split("\\s+");

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
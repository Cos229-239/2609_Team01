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

import androidx.appcompat.app.AlertDialog;
import android.content.res.ColorStateList;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

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
    private ListenerRegistration friendsListListener;
    private int friendsLoadVersion = 0;
    private int friendsRenderVersion = 0;
    private final Set<String> activeFriendUids = new HashSet<>();
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

        if (friendsListListener != null) {
            friendsListListener.remove();
            friendsListListener = null;
        }

        clearFriendStatusListeners();

        friendsLoadVersion++;
        friendsRenderVersion++;
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

        if (friendsListListener != null) {
            friendsListListener.remove();
            friendsListListener = null;
        }

        clearFriendStatusListeners();

        friendOnlineStates.clear();

        onlineFriendsContainer.removeAllViews();
        offlineFriendsContainer.removeAllViews();

        textOnlineHeader.setText("Online (0)");

        friendsListListener = db.collection("friends")
                .whereArrayContains("members", currentUid)
                .addSnapshotListener((friendSnapshots, error) -> {

                    if (loadVersion != friendsLoadVersion) {
                        return;
                    }

                    final int renderVersion = ++friendsRenderVersion;

                    clearFriendStatusListeners();
                    friendOnlineStates.clear();
                    activeFriendUids.clear();

                    onlineFriendsContainer.removeAllViews();
                    offlineFriendsContainer.removeAllViews();

                    textOnlineHeader.setText("Online (0)");

                    if (error != null || friendSnapshots == null) {
                        TextView errorText = new TextView(this);

                        errorText.setText(
                                "Failed to load friends. Please try again."
                        );

                        errorText.setTextColor(Color.RED);
                        errorText.setTextSize(14);

                        offlineFriendsContainer.addView(errorText);
                        return;
                    }

                    if (friendSnapshots.isEmpty()) {

                        showNoFriends();
                        return;
                    }

                    int invalidFriendRecords = 0;

                    for (QueryDocumentSnapshot friendDocument
                            : friendSnapshots) {

                        Object rawMembers =
                                friendDocument.get("members");

                        if (!(rawMembers instanceof List)) {
                            invalidFriendRecords++;
                            continue;
                        }

                        List<?> members = (List<?>) rawMembers;

                        if (members.size() != 2
                                || !(members.get(0) instanceof String)
                                || !(members.get(1) instanceof String)) {

                            invalidFriendRecords++;
                            continue;
                        }

                        String firstUid = (String) members.get(0);
                        String secondUid = (String) members.get(1);

                        if (firstUid.trim().isEmpty()
                                || secondUid.trim().isEmpty()
                                || firstUid.equals(secondUid)
                                || (!firstUid.equals(currentUid)
                                && !secondUid.equals(currentUid))) {

                            invalidFriendRecords++;
                            continue;
                        }

                        String friendUid =
                                currentUid.equals(firstUid)
                                        ? secondUid
                                        : firstUid;


                        String finalFriendUid =
                                friendUid;

                        activeFriendUids.add(finalFriendUid);

                        ListenerRegistration statusListener =
                                db.collection("users")
                                        .document(finalFriendUid)
                                        .addSnapshotListener(
                                                (userDocument, userError) -> {
                                                    if (loadVersion != friendsLoadVersion
                                                            || renderVersion != friendsRenderVersion
                                                            || !activeFriendUids.contains(finalFriendUid)) {
                                                        return;
                                                    }
                                                    if (userError != null
                                                            || userDocument == null
                                                            || !userDocument.exists()) {

                                                        // Remove any previous row for this friend
                                                        removeFriendRow(finalFriendUid);

                                                        // Update online state
                                                        friendOnlineStates.remove(finalFriendUid);
                                                        updateOnlineHeader();

                                                        if (userError != null) {

                                                            showFriendWarning(
                                                                    finalFriendUid,
                                                                    "Could not load this friend's profile."
                                                            );

                                                        } else {

                                                            showFriendWarning(
                                                                    finalFriendUid,
                                                                    "A friend's profile is unavailable."
                                                            );
                                                        }

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

                    // Show warning if some friendship records are invalid
                    if (invalidFriendRecords > 0) {

                        showFriendWarning(
                                null,
                                "Some friendship records could not be displayed."
                        );
                    }
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

// Remove Friend button
        Button removeFriendButton = new Button(this);

        removeFriendButton.setText("Remove Friend");
        removeFriendButton.setTextSize(12);
        removeFriendButton.setAllCaps(false);

        removeFriendButton.setTextColor(
                Color.parseColor("#C84545")
        );

        removeFriendButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#FBE9E9")
                )
        );

        LinearLayout.LayoutParams removeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(42)
                );

        removeParams.topMargin = dp(8);
        removeFriendButton.setLayoutParams(removeParams);

        removeFriendButton.setOnClickListener(v -> {
            confirmRemoveFriend(friendUid, displayName);
        });

        infoSection.addView(removeFriendButton);

        friendRow.addView(avatar);
        friendRow.addView(infoSection);




        container.addView(friendRow);
    }

    private void showFriendWarning(
            String friendUid,
            String message
    ) {

        TextView warningText = new TextView(this);

        warningText.setTag(friendUid);

        warningText.setText(message);
        warningText.setTextSize(14);

        warningText.setTextColor(
                Color.parseColor("#C84545")
        );

        warningText.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
        );

        offlineFriendsContainer.addView(warningText);
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

    private void confirmRemoveFriend(
            String friendUid,
            String friendName
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Remove Friend")
                .setMessage(
                        "Are you sure you want to remove "
                                + friendName
                                + " from your friends list?"
                )
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeFriend(friendUid);
                })
                .show();
    }

    private void removeFriend(String friendUid) {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = currentUser.getUid();

        if (currentUid.equals(friendUid)) {
            return;
        }

        // Match the friend document ID used when accepting requests
        String friendId;

        if (currentUid.compareTo(friendUid) < 0) {
            friendId = currentUid + "_" + friendUid;
        } else {
            friendId = friendUid + "_" + currentUid;
        }

        DocumentReference friendRef =
                db.collection("friends").document(friendId);

        db.runTransaction(transaction -> {

            DocumentSnapshot friendDoc =
                    transaction.get(friendRef);

            if (!friendDoc.exists()) {
                throw new IllegalStateException(
                        "Friendship no longer exists."
                );
            }

            List<String> members =
                    (List<String>) friendDoc.get("members");

            if (members == null
                    || members.size() != 2
                    || !members.contains(currentUid)
                    || !members.contains(friendUid)) {

                throw new IllegalStateException(
                        "Invalid friendship."
                );
            }

            String fromUid =
                    friendDoc.getString("user1Uid");

            String toUid =
                    friendDoc.getString("user2Uid");

            if (fromUid == null || toUid == null
                    || !members.contains(fromUid)
                    || !members.contains(toUid)) {

                throw new IllegalStateException(
                        "Invalid friend request information."
                );
            }

            // Find the original accepted request
            DocumentReference requestRef =
                    db.collection("playerRequests")
                            .document(fromUid + "_" + toUid);

            DocumentSnapshot requestDoc =
                    transaction.get(requestRef);

            // Remove the accepted request so users can add each other again
            if (requestDoc.exists()) {

                if (!"accepted".equals(
                        requestDoc.getString("status"))) {

                    throw new IllegalStateException(
                            "Friend request status is inconsistent."
                    );
                }

                transaction.delete(requestRef);
            }

            // Remove the friendship
            transaction.delete(friendRef);

            return null;

        }).addOnSuccessListener(unused -> {

            // Prevent old listeners from recreating this friend row
            activeFriendUids.remove(friendUid);

            // Remove the friend immediately from the UI
            removeFriendRow(friendUid);

            // Remove the friend's online state
            friendOnlineStates.remove(friendUid);

            // Update online friend count
            updateOnlineHeader();

            // Show empty message if no friends remain
            if (onlineFriendsContainer.getChildCount() == 0
                    && offlineFriendsContainer.getChildCount() == 0) {

                showNoFriends();
            }

            Toast.makeText(
                    FriendsActivity.this,
                    "Friend removed successfully.",
                    Toast.LENGTH_SHORT
            ).show();

        }).addOnFailureListener(e -> {

            Toast.makeText(
                    FriendsActivity.this,
                    "Failed to remove friend: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        });
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
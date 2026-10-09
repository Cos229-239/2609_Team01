package com.example.gamingteamfinder;

import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.gamingteamfinder.createTeam.CreateTeamActivity;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textNotificationBadge;
    private TextView textMessageBadge;

    private LinearLayout friendsContainer;

    private ListenerRegistration messageUnreadListener;

    private final List<ListenerRegistration> homeFriendStatusListeners =
            new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

    // Notification badge
        textNotificationBadge = findViewById(R.id.textNotificationBadge);
        textMessageBadge =
                findViewById(R.id.textMessageBadge);
        FrameLayout notificationContainer =
                findViewById(R.id.notificationContainer);

    // Hide badge until Firestore finishes loading
        textNotificationBadge.setVisibility(View.GONE);
        textMessageBadge.setVisibility(View.GONE);

        friendsContainer = findViewById(R.id.friendsContainer);

        // Quick Actions
        Button buttonFindTeam = findViewById(R.id.buttonFindTeam);
        Button buttonFindDuo = findViewById(R.id.buttonFindDuo);
        Button buttonFindPlayers = findViewById(R.id.buttonFindPlayers);
        Button buttonTeam = findViewById(R.id.buttonTeam);
        ImageView buttonFriends = findViewById(R.id.buttonFriends);

        buttonFriends.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    FriendsActivity.class
            );

            startActivity(intent);
        });

        // Bottom Navigation
        ImageButton buttonHome = findViewById(R.id.buttonHome);
        ImageButton buttonSearch = findViewById(R.id.buttonSearch);
        ImageButton buttonMessages = findViewById(R.id.buttonMessages);
        ImageButton buttonProfile = findViewById(R.id.buttonProfile);


        // Notifications
        notificationContainer.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    NotificationsActivity.class
            );

            startActivity(intent);
        });

        // Find Team
        buttonFindTeam.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LfgActivity.class);
            startActivity(intent);
        });


        // Find Duo - temporary
        buttonFindDuo.setOnClickListener(v -> {
            Toast.makeText(
                    HomeActivity.this,
                    "Find Duo coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });


        // Browse Players
        buttonFindPlayers.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchPlayerActivity.class);
            startActivity(intent);
        });


        // Create Team
        buttonTeam.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CreateTeamActivity.class);
            startActivity(intent);
        });


        // Home
        buttonHome.setOnClickListener(v -> {
            // Already on Home screen
        });


        // Search
        buttonSearch.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchPlayerActivity.class);
            startActivity(intent);
        });


        // Messages
        buttonMessages.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    MessagesActivity.class
            );

            startActivity(intent);
        });


        // Profile
        buttonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadNotificationBadge();
        loadFriends();
        startMessageUnreadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (messageUnreadListener != null) {

            messageUnreadListener.remove();
            messageUnreadListener = null;
        }

        clearHomeFriendStatusListeners();
    }

    private void loadNotificationBadge() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            textNotificationBadge.setVisibility(View.GONE);
            return;
        }

        String currentUid = currentUser.getUid();

        db.collection("playerRequests")
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int requestCount = queryDocumentSnapshots.size();

                    if (requestCount > 0) {

                        textNotificationBadge.setText(
                                String.valueOf(requestCount)
                        );

                        textNotificationBadge.setVisibility(View.VISIBLE);

                    } else {

                        textNotificationBadge.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {

                    textNotificationBadge.setVisibility(View.GONE);
                });
    }

    private void startMessageUnreadListener() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {

            textMessageBadge.setVisibility(
                    View.GONE
            );

            return;
        }


        String currentUid =
                currentUser.getUid();


        if (messageUnreadListener != null) {

            messageUnreadListener.remove();
            messageUnreadListener = null;
        }


        messageUnreadListener =
                db.collection("conversations")
                        .whereArrayContains(
                                "members",
                                currentUid
                        )
                        .addSnapshotListener(
                                (conversationSnapshots, error) -> {

                                    if (error != null
                                            || conversationSnapshots == null) {

                                        textMessageBadge.setVisibility(
                                                View.GONE
                                        );

                                        return;
                                    }


                                    long totalUnread = 0;


                                    for (QueryDocumentSnapshot document
                                            : conversationSnapshots) {

                                        String user1Uid =
                                                document.getString(
                                                        "user1Uid"
                                                );

                                        String user2Uid =
                                                document.getString(
                                                        "user2Uid"
                                                );


                                        Long unreadCount = 0L;


                                        if (currentUid.equals(user1Uid)) {

                                            unreadCount =
                                                    document.getLong(
                                                            "user1UnreadCount"
                                                    );

                                        } else if (currentUid.equals(user2Uid)) {

                                            unreadCount =
                                                    document.getLong(
                                                            "user2UnreadCount"
                                                    );
                                        }


                                        if (unreadCount != null) {

                                            totalUnread +=
                                                    unreadCount;
                                        }
                                    }


                                    if (totalUnread > 0) {

                                        String badgeText;

                                        if (totalUnread > 99) {

                                            badgeText = "99+";

                                        } else {

                                            badgeText =
                                                    String.valueOf(
                                                            totalUnread
                                                    );
                                        }


                                        textMessageBadge.setText(
                                                badgeText
                                        );

                                        textMessageBadge.setVisibility(
                                                View.VISIBLE
                                        );

                                    } else {

                                        textMessageBadge.setVisibility(
                                                View.GONE
                                        );
                                    }
                                }
                        );
    }

    private void addFriend(
            LinearLayout container,
            String friendUid,
            String initials,
            String name,
            boolean isOnline
    ) {
        int size = (int) (72 * getResources().getDisplayMetrics().density);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout friendItem = new LinearLayout(this);
        friendItem.setOrientation(LinearLayout.VERTICAL);
        friendItem.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        itemParams.setMargins(0, 0, margin, 0);
        friendItem.setLayoutParams(itemParams);

        TextView avatar = new TextView(this);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(size, size);

        avatar.setLayoutParams(avatarParams);
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(initials);
        avatar.setTextSize(20);
        avatar.setTextColor(Color.parseColor("#30364F"));
        avatar.setBackgroundResource(R.drawable.friend_avatar_background);

        TextView friendName = new TextView(this);
        friendName.setText(name);
        friendName.setTextSize(14);
        friendName.setGravity(Gravity.CENTER);
        friendName.setPadding(0, 8, 0, 0);

        TextView onlineStatus = new TextView(this);

        if (isOnline) {

            onlineStatus.setText("● Online");
            onlineStatus.setTextColor(
                    Color.parseColor("#3FAE64")
            );

        } else {

            onlineStatus.setText("● Offline");
            onlineStatus.setTextColor(
                    Color.parseColor("#9E9E9E")
            );
        }

        onlineStatus.setTextSize(11);
        onlineStatus.setGravity(Gravity.CENTER);
        friendItem.addView(avatar);
        friendItem.addView(friendName);
        friendItem.addView(onlineStatus);

        container.addView(friendItem);
        ListenerRegistration statusListener =
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

                                    boolean online =
                                            Boolean.TRUE.equals(
                                                    onlineValue
                                            );


                                    if (online) {

                                        onlineStatus.setText(
                                                "● Online"
                                        );

                                        onlineStatus.setTextColor(
                                                Color.parseColor(
                                                        "#3FAE64"
                                                )
                                        );

                                    } else {

                                        onlineStatus.setText(
                                                "● Offline"
                                        );

                                        onlineStatus.setTextColor(
                                                Color.parseColor(
                                                        "#9E9E9E"
                                                )
                                        );
                                    }
                                }
                        );


        homeFriendStatusListeners.add(
                statusListener
        );
    }

    private void loadFriends() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String currentUid = currentUser.getUid();
        clearHomeFriendStatusListeners();

        // Remove old friend cards
        friendsContainer.removeAllViews();

        db.collection("friends")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener(friendSnapshots -> {

                    if (friendSnapshots.isEmpty()) {
                        showNoFriends();
                        return;
                    }

                    for (QueryDocumentSnapshot friendDocument : friendSnapshots) {

                        List<String> members =
                                (List<String>) friendDocument.get("members");

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
                                .document(finalFriendUid)
                                .get()
                                .addOnSuccessListener(userDocument -> {

                                    if (!userDocument.exists()) {
                                        return;
                                    }

                                    String displayName =
                                            userDocument.getString("displayName");

                                    if (displayName == null
                                            || displayName.trim().isEmpty()) {

                                        displayName = "Player";
                                    }

                                    String initials =
                                            getInitials(displayName);

                                    Boolean onlineValue =
                                            userDocument.getBoolean("isOnline");

                                    boolean isOnline =
                                            Boolean.TRUE.equals(onlineValue);

                                    addFriend(
                                            friendsContainer,
                                            finalFriendUid,
                                            initials,
                                            displayName,
                                            isOnline
                                    );
                                });
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            HomeActivity.this,
                            "Failed to load friends.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private String getInitials(String displayName) {

        if (displayName == null || displayName.trim().isEmpty()) {
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

    private void clearHomeFriendStatusListeners() {

        for (ListenerRegistration listener
                : homeFriendStatusListeners) {

            listener.remove();
        }

        homeFriendStatusListeners.clear();
    }

    private void showNoFriends() {

        TextView noFriends = new TextView(this);

        noFriends.setText("No friends yet");
        noFriends.setTextSize(14);
        noFriends.setTextColor(
                Color.parseColor("#777777")
        );

        friendsContainer.addView(noFriends);
    }
}
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

import android.graphics.Typeface;
import com.example.gamingteamfinder.teamDetails.TeamDetailsActivity;
import com.example.gamingteamfinder.utils.TeamLogoHelper;


public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textNotificationBadge;
    private TextView textMessageBadge;
    private ListenerRegistration notificationBadgeListener;

    private LinearLayout friendsContainer;

    private ListenerRegistration messageUnreadListener;

    // Listener for My Teams
    private ListenerRegistration teamsListener;

    private final List<ListenerRegistration> homeFriendStatusListeners =
            new ArrayList<>();

    private ListenerRegistration homeFriendsListListener;
    private int homeFriendsLoadVersion = 0;
    private int homeFriendsRenderVersion = 0;


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
        ImageButton buttonSettings = findViewById(R.id.buttonSettings);

        // Open Settings
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HomeActivity.this,
                    SettingsActivity.class
            );

            startActivity(intent);
        });


        // Open Friends
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
        loadMyTeams();
    }

    private void loadMyTeams() {

        LinearLayout teamsContainer =
                findViewById(R.id.layoutMyTeams);

        String userId = FirebaseAuth.getInstance()
                .getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            teamsContainer.removeAllViews();
            return;
        }

        if (teamsListener != null) {
            teamsListener.remove();
            teamsListener = null;
        }

        teamsListener = FirebaseFirestore.getInstance()
                .collection("teams")
                .whereEqualTo("ownerId", userId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        android.util.Log.e(
                                "HomeActivity",
                                "Failed to load teams",
                                error
                        );

                        Toast.makeText(
                                HomeActivity.this,
                                "Failed to load teams",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    teamsContainer.removeAllViews();

                    if (snapshot == null || snapshot.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText(
                                "You haven't created any teams yet."
                        );
                        emptyText.setTextColor(Color.DKGRAY);
                        emptyText.setTextSize(14);
                        teamsContainer.addView(emptyText);
                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snapshot.getDocuments()) {

                        String teamId = doc.getId();
                        String teamName = doc.getString("teamName");
                        String logoId = doc.getString("logoId");

                        addTeamWidget(
                                teamsContainer,
                                teamId,
                                teamName != null ? teamName : "Team",
                                logoId
                        );
                    }
                });
    }



    private void addTeamWidget(
            LinearLayout container,
            String teamId,
            String teamName,
            String logoId
    ) {

        // Create team container
        LinearLayout teamItem = new LinearLayout(this);
        teamItem.setOrientation(LinearLayout.VERTICAL);
        teamItem.setGravity(Gravity.CENTER_HORIZONTAL);
        teamItem.setClickable(true);
        teamItem.setFocusable(true);

        int width = dpToPx(96);

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        width,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        itemParams.setMargins(0, 0, dpToPx(12), 0);
        teamItem.setLayoutParams(itemParams);

        // Create team logo
        ImageView logo = new ImageView(this);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(
                        dpToPx(76),
                        dpToPx(76)
                );

        logo.setLayoutParams(imageParams);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setContentDescription(teamName + " logo");

        // Circular logo background
        android.graphics.drawable.GradientDrawable circle =
                new android.graphics.drawable.GradientDrawable();

        circle.setShape(
                android.graphics.drawable.GradientDrawable.OVAL
        );

        circle.setColor(Color.parseColor("#CED8F0"));

        logo.setBackground(circle);
        logo.setClipToOutline(true);

        // Display selected preloaded logo
        logo.setImageResource(
                TeamLogoHelper.getDrawableId(this, logoId)
        );

        teamItem.addView(logo);

        // Create team name
        TextView nameText = new TextView(this);

        nameText.setText(teamName);
        nameText.setTextColor(Color.parseColor("#303443"));
        nameText.setTextSize(13);
        nameText.setTypeface(null, Typeface.NORMAL);
        nameText.setGravity(Gravity.CENTER);
        nameText.setMaxLines(2);
        nameText.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        nameParams.topMargin = dpToPx(8);
        nameText.setLayoutParams(nameParams);

        // Display team name under logo
        teamItem.addView(nameText);

        // Open Team Details when tapped
        teamItem.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    TeamDetailsActivity.class
            );

            intent.putExtra("TEAM_ID", teamId);

            startActivity(intent);
        });

        // Add team widget to My Teams section
        container.addView(teamItem);
    }


    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }



    @Override
    protected void onPause() {
        super.onPause();

        // Stop notification listener when leaving Home
        if (notificationBadgeListener != null) {
            notificationBadgeListener.remove();
            notificationBadgeListener = null;
        }

        // Stop listening for friendship changes
        if (homeFriendsListListener != null) {
            homeFriendsListListener.remove();
            homeFriendsListListener = null;
        }

        // Invalidate old asynchronous callbacks
        homeFriendsLoadVersion++;
        homeFriendsRenderVersion++;

        if (messageUnreadListener != null) {

            messageUnreadListener.remove();
            messageUnreadListener = null;
        }

        clearHomeFriendStatusListeners();
        if (teamsListener != null) {
            teamsListener.remove();
            teamsListener = null;
        }
    }

    private void loadNotificationBadge() {

        // Remove previous listener
        if (notificationBadgeListener != null) {
            notificationBadgeListener.remove();
            notificationBadgeListener = null;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Hide badge until data is loaded
        textNotificationBadge.setVisibility(View.GONE);

        if (currentUser == null) {
            return;
        }

        String currentUid = currentUser.getUid();

        // Listen for pending friend requests in real time
        notificationBadgeListener = db.collection("playerRequests")
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) {

                        android.util.Log.e(
                                "HomeActivity",
                                "Failed to load notification badge",
                                error
                        );

                        textNotificationBadge.setVisibility(View.GONE);
                        return;
                    }

                    int requestCount = snapshots.size();

                    if (requestCount > 0) {

                        String badgeText;

                        if (requestCount > 99) {
                            badgeText = "99+";
                        } else {
                            badgeText = String.valueOf(requestCount);
                        }

                        textNotificationBadge.setText(badgeText);
                        textNotificationBadge.setVisibility(View.VISIBLE);

                    } else {

                        textNotificationBadge.setVisibility(View.GONE);
                    }
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

        // Stop previous friends list listener
        if (homeFriendsListListener != null) {
            homeFriendsListListener.remove();
            homeFriendsListListener = null;
        }

        clearHomeFriendStatusListeners();
        friendsContainer.removeAllViews();

        final int loadVersion = ++homeFriendsLoadVersion;

        if (currentUser == null) {
            showNoFriends();
            return;
        }

        String currentUid = currentUser.getUid();

        // Listen for friendship changes in real time
        homeFriendsListListener = db.collection("friends")
                .whereArrayContains("members", currentUid)
                .addSnapshotListener((friendSnapshots, error) -> {

                    if (loadVersion != homeFriendsLoadVersion) {
                        return;
                    }

                    final int renderVersion =
                            ++homeFriendsRenderVersion;

                    clearHomeFriendStatusListeners();
                    friendsContainer.removeAllViews();

                    if (error != null || friendSnapshots == null) {

                        TextView errorText = new TextView(this);
                        errorText.setText("Failed to load friends.");
                        errorText.setTextColor(Color.RED);
                        errorText.setTextSize(14);

                        friendsContainer.addView(errorText);
                        return;
                    }

                    if (friendSnapshots.isEmpty()) {
                        showNoFriends();
                        return;
                    }

                    int validFriends = 0;
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

                        String finalFriendUid =
                                currentUid.equals(firstUid)
                                        ? secondUid
                                        : firstUid;

                        validFriends++;

                        db.collection("users")
                                .document(finalFriendUid)
                                .get()
                                .addOnSuccessListener(userDocument -> {

                                    // Prevent old callbacks from adding removed friends
                                    if (loadVersion != homeFriendsLoadVersion
                                            || renderVersion != homeFriendsRenderVersion) {
                                        return;
                                    }

                                    if (!userDocument.exists()) {

                                        showHomeFriendWarning(
                                                "A friend's profile is unavailable."
                                        );

                                        return;
                                    }

                                    String displayName =
                                            userDocument.getString("displayName");

                                    if (displayName == null
                                            || displayName.trim().isEmpty()) {
                                        displayName = "Player";
                                    }

                                    String initials = getInitials(displayName);

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
                                })
                                .addOnFailureListener(e -> {

                                    if (loadVersion != homeFriendsLoadVersion
                                            || renderVersion != homeFriendsRenderVersion) {
                                        return;
                                    }

                                    showHomeFriendWarning(
                                            "Could not load a friend's profile."
                                    );
                                });
                    }

                    if (invalidFriendRecords > 0) {

                        showHomeFriendWarning(
                                "Some friendship records could not be displayed."
                        );
                    }

                    if (validFriends == 0 && invalidFriendRecords == 0) {
                        showNoFriends();
                    }
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

    private void showHomeFriendWarning(String message) {

        TextView warningText = new TextView(this);

        warningText.setText(message);
        warningText.setTextSize(13);

        warningText.setTextColor(
                Color.parseColor("#C84545")
        );

        warningText.setMaxWidth(dpToPx(180));

        warningText.setPadding(
                dpToPx(8),
                dpToPx(8),
                dpToPx(8),
                dpToPx(8)
        );

        friendsContainer.addView(warningText);
    }
}
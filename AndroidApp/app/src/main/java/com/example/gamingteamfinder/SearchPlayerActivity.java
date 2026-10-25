package com.example.gamingteamfinder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.View;
import com.example.gamingteamfinder.teamDetails.TeamDetailsActivity;
import com.example.gamingteamfinder.utils.TeamLogoHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class SearchPlayerActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout playersContainer;
    private boolean isLoadingPlayers = false;
    private boolean showingTeams = false;
    private LinearLayout teamsContainer;
    private View playersScroll;
    private View teamsScroll;
    private TextView tabPlayersView;
    private TextView tabTeamsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_player);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Top controls
        Button buttonBack = findViewById(R.id.buttonBack);
        TextView tabPlayers = findViewById(R.id.tabPlayers);
        TextView tabTeams = findViewById(R.id.tabTeams);
        Button buttonFilter = findViewById(R.id.buttonFilter);

        playersContainer = findViewById(R.id.playersContainer);
        teamsContainer = findViewById(R.id.teamsContainer);
        playersScroll = findViewById(R.id.playersScroll);
        teamsScroll = findViewById(R.id.teamsScroll);
        tabPlayersView = tabPlayers;
        tabTeamsView = tabTeams;

        // Bottom navigation
        ImageButton buttonNavHome = findViewById(R.id.buttonNavHome);
        ImageButton buttonNavSearch = findViewById(R.id.buttonNavSearch);
        ImageButton buttonNavMessages = findViewById(R.id.buttonNavMessages);
        ImageButton buttonNavProfile = findViewById(R.id.buttonNavProfile);

        // Back
        buttonBack.setOnClickListener(v -> {

            Intent intent = new Intent(
                    SearchPlayerActivity.this,
                    HomeActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });

        // Switch tabs and swap their selected colors
        tabPlayers.setOnClickListener(v -> showTab(false));
        tabTeams.setOnClickListener(v -> showTab(true));
        showTab(false);

        // Filter
        buttonFilter.setOnClickListener(v -> {
            Toast.makeText(
                    SearchPlayerActivity.this,
                    "Filter coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Bottom Navigation
        buttonNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SearchPlayerActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);
        });

        buttonNavSearch.setOnClickListener(v -> {
            // Already on Search screen
        });

        buttonNavMessages.setOnClickListener(v -> {

            Intent intent = new Intent(
                    SearchPlayerActivity.this,
                    MessagesActivity.class
            );

            startActivity(intent);
        });

        buttonNavProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    SearchPlayerActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (showingTeams) {
            loadTeams();
        } else if (playersContainer != null) {
            loadPlayers();
        }
    }

    private void showTab(boolean teams) {
        showingTeams = teams;
        int selected = Color.parseColor("#6C4FB3");
        int inactive = Color.parseColor("#D5D5D5");
        tabPlayersView.setBackgroundColor(teams ? inactive : selected);
        tabTeamsView.setBackgroundColor(teams ? selected : inactive);
        tabPlayersView.setTextColor(teams ? Color.parseColor("#333333") : Color.WHITE);
        tabTeamsView.setTextColor(teams ? Color.WHITE : Color.parseColor("#333333"));
        playersScroll.setVisibility(teams ? View.GONE : View.VISIBLE);
        teamsScroll.setVisibility(teams ? View.VISIBLE : View.GONE);
        if (teams) loadTeams();
        else if (playersContainer.getChildCount() == 0) loadPlayers();
    }

    private void loadTeams() {
        teamsContainer.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("Loading teams...");
        teamsContainer.addView(loading);

        long teamStartTime =
                android.os.SystemClock.elapsedRealtime();
        db.collection("teams")
                .whereEqualTo("status", "open")
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {

                    long elapsed =
                            android.os.SystemClock.elapsedRealtime()
                                    - teamStartTime;

                    android.util.Log.d(
                            "SearchPerformance",
                            "Teams: " + snapshot.size()
                                    + " documents in " + elapsed + " ms"
                    );

                    if (!showingTeams) return;
                    teamsContainer.removeAllViews();
                    int count = 0;
                    for (DocumentSnapshot team : snapshot.getDocuments()) {
                        addTeamCard(team);
                        count++;
                    }
                    if (count == 0) showTeamMessage("No teams found.");
                })
                .addOnFailureListener(e -> {
                    if (showingTeams) {
                        teamsContainer.removeAllViews();
                        showTeamMessage("Failed to load teams: " + e.getMessage());
                    }
                });
    }

    private void showTeamMessage(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setPadding(0, dp(24), 0, 0);
        teamsContainer.addView(text);
    }

    private void addTeamCard(DocumentSnapshot team) {
        String teamId = team.getId();
        String name = team.getString("teamName");
        String game = team.getString("game");
        String mode = team.getString("gameMode");
        String minRank = team.getString("minRank");
        String maxRank = team.getString("maxRank");
        String ownerId = team.getString("ownerId");
        String start = team.getString("startTime");
        String end = team.getString("endTime");
        List<?> days = (List<?>) team.get("days");
        Long members = team.getLong("currentPlayers");
        Long maxMembers = team.getLong("maxPlayers");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(12), dp(10), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#D9D9D9"));
        background.setCornerRadius(dp(8));
        card.setBackground(background);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(16);
        card.setLayoutParams(cardParams);

        LinearLayout images = new LinearLayout(this);
        images.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams imagesParams = new LinearLayout.LayoutParams(dp(60), -2);
        imagesParams.rightMargin = dp(10);
        images.setLayoutParams(imagesParams);

        ImageView teamLogo = new ImageView(this);
        teamLogo.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        teamLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        teamLogo.setImageResource(TeamLogoHelper.getDrawableId(this, team.getString("logoId")));
        teamLogo.setContentDescription("Team logo");
        images.addView(teamLogo);

        ImageView gameLogo = new ImageView(this);
        LinearLayout.LayoutParams gameLogoParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        gameLogoParams.topMargin = dp(6);
        gameLogo.setLayoutParams(gameLogoParams);
        gameLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int gameDrawable = getGameLogo(game);
        if (gameDrawable != 0) {
            gameLogo.setImageResource(gameDrawable);
        } else {
            // Fallback when game logo drawables have not been added yet
            gameLogo.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        gameLogo.setContentDescription(game == null ? "Game logo" : game + " logo");
        images.addView(gameLogo);
        card.addView(images);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        addTeamText(details, name == null ? "Unnamed Team" : name, true);
        addTeamText(details, (mode == null ? "Game mode not set" : mode)
                + " - " + (minRank == null ? "?" : minRank)
                + " to " + (maxRank == null ? "?" : maxRank), false);
        addTeamText(details, "Mic - " + (Boolean.TRUE.equals(team.getBoolean("voiceChat")) ? "Yes" : "No"), false);
        String dayText = "Days not set";

        if (days != null && !days.isEmpty()) {
            List<String> shortDays = new ArrayList<>();

            for (Object day : days) {
                if (day instanceof String) {
                    String dayName = (String) day;

                    shortDays.add(
                            dayName.length() >= 3
                                    ? dayName.substring(0, 3)
                                    : dayName
                    );
                }
            }

            if (!shortDays.isEmpty()) {
                dayText = android.text.TextUtils.join(", ", shortDays);
            }
        }
        addTeamText(details, dayText + " · " + (start == null ? "?" : start)
                + " - " + (end == null ? "?" : end), false);
        addTeamText(details, (members == null ? 0 : members) + "/"
                + (maxMembers == null ? 5 : maxMembers) + " Players", false);
        card.addView(details);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(dp(74), -2);
        actionsParams.leftMargin = dp(6);
        actions.setLayoutParams(actionsParams);

        Button join = new Button(this);
        join.setText("Join");
        join.setAllCaps(false);
        join.setTextSize(12);
        join.setPadding(0, 0, 0, 0);
        join.setTextColor(Color.WHITE);
        join.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6C4FB3")));
        actions.addView(join);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getUid().equals(ownerId)) {
            join.setText("Owner");
            join.setEnabled(false);
        } else if (members != null && maxMembers != null && members >= maxMembers) {
            join.setText("Full");
            join.setEnabled(false);
        } else if (user != null) {
            // Each user can have one request document per team.
            db.collection("teams").document(teamId).collection("joinRequests")
                    .document(user.getUid()).get().addOnSuccessListener(request -> {
                        if (request.exists() && "pending".equals(request.getString("status"))) {
                            join.setText("Pending");
                            join.setEnabled(false);
                        }
                    });
        }
        join.setOnClickListener(v -> sendJoinRequest(teamId, ownerId, join));

        Button view = new Button(this);
        view.setText("View");
        view.setAllCaps(false);
        view.setTextSize(12);
        view.setPadding(0, 0, 0, 0);
        view.setTextColor(Color.WHITE);
        view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6C4FB3")));
        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeamDetailsActivity.class);
            intent.putExtra("TEAM_ID", teamId);
            startActivity(intent);
        });
        actions.addView(view);
        card.addView(actions);
        teamsContainer.addView(card);
    }

    private void addTeamText(LinearLayout parent, String value, boolean heading) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.parseColor("#222222"));
        text.setTextSize(heading ? 16 : 13);
        if (heading) text.setTypeface(null, Typeface.BOLD);
        parent.addView(text);
    }

    private int getGameLogo(String game) {
        if (game == null) return 0;
        // Put game_logo_lol.png and game_logo_valorant.png in res/drawable.
        String name = game.equalsIgnoreCase("League of Legends")
                ? "game_logo_lol" : game.equalsIgnoreCase("Valorant")
                                    ? "game_logo_valorant" : "";
        return name.isEmpty() ? 0 : getResources().getIdentifier(name, "drawable", getPackageName());
    }

    private void sendJoinRequest(String teamId, String ownerId, Button join) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ownerId == null || ownerId.equals(user.getUid())) return;
        join.setEnabled(false);
        com.google.firebase.firestore.DocumentReference requestRef = db.collection("teams")
                .document(teamId).collection("joinRequests").document(user.getUid());
        // A transaction prevents duplicate pending requests from rapid taps.
        db.runTransaction(transaction -> {
            DocumentSnapshot previous = transaction.get(requestRef);
            if (previous.exists() && "pending".equals(previous.getString("status"))) {
                return false;
            }
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("teamId", teamId);
            data.put("ownerId", ownerId);
            data.put("requesterId", user.getUid());
            data.put("status", "pending");
            data.put("createdAt", FieldValue.serverTimestamp());
            transaction.set(requestRef, data);
            return true;
        }).addOnSuccessListener(created -> {
            join.setText("Pending");
            Toast.makeText(this, created ? "Join request sent" : "Request already pending", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            join.setEnabled(true);
            Toast.makeText(this, "Couldn't send request: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadPlayers() {

        if (isLoadingPlayers) {
            return;
        }

        isLoadingPlayers = true;

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            isLoadingPlayers = false;

            Toast.makeText(
                    SearchPlayerActivity.this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currentUid = currentUser.getUid();

        // Clear old player cards
        playersContainer.removeAllViews();

        Set<String> friendUids = new HashSet<>();

        // First, find all friends of the current user
        db.collection("friends")
                .whereArrayContains("members", currentUid)
                .get()
                .addOnSuccessListener(friendSnapshots -> {

                    for (QueryDocumentSnapshot friendDocument : friendSnapshots) {

                        List<String> members =
                                (List<String>) friendDocument.get("members");

                        if (members != null) {

                            for (String uid : members) {

                                // Save only the OTHER user's UID
                                if (!uid.equals(currentUid)) {
                                    friendUids.add(uid);
                                }
                            }
                        }
                    }

                    // Load a limited batch of users
                    long playerStartTime =
                            android.os.SystemClock.elapsedRealtime();

                    db.collection("users")
                            .limit(30)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {

                                long elapsed =
                                        android.os.SystemClock.elapsedRealtime()
                                                - playerStartTime;

                                android.util.Log.d(
                                        "SearchPerformance",
                                        "Players: " + queryDocumentSnapshots.size()
                                                + " documents in " + elapsed + " ms"
                                );

                                int playerCount = 0;

                                for (QueryDocumentSnapshot document
                                        : queryDocumentSnapshots) {

                                    String playerUid = document.getId();

                                    // Do not show yourself
                                    if (playerUid.equals(currentUid)) {
                                        continue;
                                    }

                                    // Do not show existing friends
                                    if (friendUids.contains(playerUid)) {
                                        continue;
                                    }

                                    String displayName =
                                            getValue(document.getString("displayName"));

                                    String game =
                                            getValue(document.getString("game"));

                                    String rank =
                                            getValue(document.getString("rank"));

                                    String role =
                                            getValue(document.getString("role"));

                                    String availability =
                                            getValue(document.getString("availability"));

                                    addPlayerCard(
                                            playerUid,
                                            displayName,
                                            game,
                                            rank,
                                            role,
                                            availability
                                    );

                                    playerCount++;
                                }

                                if (playerCount == 0) {
                                    showNoPlayers();
                                }

                                isLoadingPlayers = false;
                            })
                            .addOnFailureListener(e -> {

                                isLoadingPlayers = false;

                                Toast.makeText(
                                        SearchPlayerActivity.this,
                                        "Failed to load players.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    isLoadingPlayers = false;

                    Toast.makeText(
                            SearchPlayerActivity.this,
                            "Failed to load friends.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void addPlayerCard(
            String playerUid,
            String displayName,
            String game,
            String rank,
            String role,
            String availability
    ) {

        // Main player card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        int padding = dp(16);

        card.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.parseColor("#D9D9D9"));
        cardBackground.setCornerRadius(dp(16));

        card.setBackground(cardBackground);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dp(16)
        );

        card.setLayoutParams(cardParams);


        // -------------------------
        // TOP: Avatar + Gamer Tag
        // -------------------------
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(
                        dp(56),
                        dp(56)
                );

        avatarParams.setMargins(
                0,
                0,
                dp(14),
                0
        );

        avatar.setLayoutParams(avatarParams);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextSize(18);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setText(getInitials(displayName));

        GradientDrawable avatarBackground =
                new GradientDrawable();

        avatarBackground.setShape(GradientDrawable.OVAL);
        avatarBackground.setColor(Color.WHITE);

        avatar.setBackground(avatarBackground);

        topRow.addView(avatar);


        TextView nameText = new TextView(this);
        nameText.setText(displayName);
        nameText.setTextSize(20);
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setTextColor(Color.parseColor("#222222"));

        topRow.addView(nameText);

        card.addView(topRow);


        // Space
        TextView spacer1 = new TextView(this);
        spacer1.setHeight(dp(18));
        card.addView(spacer1);


        // -------------------------
        // GAME + RANK / ROLE
        // -------------------------
        LinearLayout gameRankRow =
                new LinearLayout(this);

        gameRankRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        // LEFT SIDE
        LinearLayout gameSection =
                new LinearLayout(this);

        gameSection.setOrientation(
                LinearLayout.VERTICAL
        );

        gameSection.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView gameLabel =
                new TextView(this);

        gameLabel.setText("Currently playing");
        gameLabel.setTextSize(13);
        gameLabel.setTextColor(
                Color.parseColor("#555555")
        );

        TextView gameText =
                new TextView(this);

        gameText.setText(game);
        gameText.setTextSize(16);
        gameText.setTypeface(
                null,
                Typeface.BOLD
        );

        gameText.setTextColor(
                Color.parseColor("#222222")
        );

        gameSection.addView(gameLabel);
        gameSection.addView(gameText);


        // RIGHT SIDE
        LinearLayout rankSection =
                new LinearLayout(this);

        rankSection.setOrientation(
                LinearLayout.VERTICAL
        );

        rankSection.setGravity(
                Gravity.END
        );

        rankSection.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView rankLabel =
                new TextView(this);

        rankLabel.setText("Rank / Role");
        rankLabel.setTextSize(13);
        rankLabel.setTextColor(
                Color.parseColor("#555555")
        );

        TextView rankText =
                new TextView(this);

        rankText.setText(
                rank + " • " + role
        );

        rankText.setTextSize(16);
        rankText.setTypeface(
                null,
                Typeface.BOLD
        );

        rankText.setTextColor(
                Color.parseColor("#222222")
        );

        rankSection.addView(rankLabel);
        rankSection.addView(rankText);

        gameRankRow.addView(gameSection);
        gameRankRow.addView(rankSection);

        card.addView(gameRankRow);


        // Space
        TextView spacer2 = new TextView(this);
        spacer2.setHeight(dp(18));
        card.addView(spacer2);


        // -------------------------
        // PLAY STYLE
        // -------------------------
        TextView playStyleLabel =
                new TextView(this);

        playStyleLabel.setText(
                "Play Style / Availability"
        );

        playStyleLabel.setTextSize(13);
        playStyleLabel.setTextColor(
                Color.parseColor("#555555")
        );

        card.addView(playStyleLabel);


        TextView playStyleText =
                new TextView(this);

        playStyleText.setText(availability);
        playStyleText.setTextSize(15);
        playStyleText.setTextColor(
                Color.parseColor("#222222")
        );

        playStyleText.setPadding(
                0,
                dp(4),
                0,
                dp(16)
        );

        card.addView(playStyleText);


        // -------------------------
        // VIEW PLAYER BUTTON
        // -------------------------
        Button viewPlayerButton =
                new Button(this);

        viewPlayerButton.setText(
                "View Player"
        );

        viewPlayerButton.setTextSize(15);
        viewPlayerButton.setTextColor(
                Color.WHITE
        );

        viewPlayerButton.setAllCaps(false);

        viewPlayerButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#6C4FB3")
                )
        );

        viewPlayerButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    SearchPlayerActivity.this,
                    PlayerDetailActivity.class
            );

            intent.putExtra(
                    "PLAYER_UID",
                    playerUid
            );

            startActivity(intent);
        });

        card.addView(viewPlayerButton);


        // Add player card to screen
        playersContainer.addView(card);
    }
    private void showNoPlayers() {

        TextView noPlayers =
                new TextView(this);

        noPlayers.setText(
                "No other players found."
        );

        noPlayers.setTextSize(16);
        noPlayers.setGravity(Gravity.CENTER);

        noPlayers.setPadding(
                0,
                dp(30),
                0,
                0
        );

        playersContainer.addView(noPlayers);
    }

    private String getValue(String value) {

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
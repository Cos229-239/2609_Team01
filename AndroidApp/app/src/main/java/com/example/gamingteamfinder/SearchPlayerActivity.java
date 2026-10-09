package com.example.gamingteamfinder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
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

public class SearchPlayerActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout playersContainer;
    private boolean isLoadingPlayers = false;

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

        // Players tab
        tabPlayers.setOnClickListener(v -> {
            // Already on Players tab
        });

        // Teams tab
        tabTeams.setOnClickListener(v -> {
            Toast.makeText(
                    SearchPlayerActivity.this,
                    "Teams coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });

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

        if (playersContainer != null) {
            loadPlayers();
        }
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

                    // Now load all users
                    db.collection("users")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {

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
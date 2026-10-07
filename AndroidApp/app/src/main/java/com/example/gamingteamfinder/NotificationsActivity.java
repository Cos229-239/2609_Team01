package com.example.gamingteamfinder;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class NotificationsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout requestsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        requestsContainer = findViewById(R.id.requestsContainer);

        ImageButton buttonHome = findViewById(R.id.buttonHome);
        ImageButton buttonSearch = findViewById(R.id.buttonSearch);
        ImageButton buttonMessages = findViewById(R.id.buttonMessages);
        ImageButton buttonProfile = findViewById(R.id.buttonProfile);

        buttonHome.setOnClickListener(v -> {

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);
        });

        buttonSearch.setOnClickListener(v -> {

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    SearchPlayerActivity.class
            );

            startActivity(intent);
        });

        buttonMessages.setOnClickListener(v -> {

            Toast.makeText(
                    NotificationsActivity.this,
                    "Messages coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });

        buttonProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });

        Button buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadRequests();
    }

    private void loadRequests() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currentUid = currentUser.getUid();

        requestsContainer.removeAllViews();

        db.collection("playerRequests")
                .whereEqualTo("toUid", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int pendingCount = 0;

                    for (QueryDocumentSnapshot document
                            : queryDocumentSnapshots) {

                        String status = document.getString("status");

                        if (!"pending".equals(status)) {
                            continue;
                        }

                        String requestId = document.getId();
                        String fromUid = document.getString("fromUid");

                        if (fromUid == null || fromUid.isEmpty()) {
                            continue;
                        }

                        pendingCount++;

                        loadSenderProfile(
                                requestId,
                                fromUid
                        );
                    }

                    if (pendingCount == 0) {
                        showNoRequests();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            NotificationsActivity.this,
                            "Failed to load notifications.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void loadSenderProfile(
            String requestId,
            String fromUid
    ) {

        db.collection("users")
                .document(fromUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    String displayName =
                            getValue(documentSnapshot.getString("displayName"));

                    String game =
                            getValue(documentSnapshot.getString("game"));

                    String rank =
                            getValue(documentSnapshot.getString("rank"));

                    String role =
                            getValue(documentSnapshot.getString("role"));

                    addRequestCard(
                            requestId,
                            fromUid,
                            displayName,
                            game,
                            rank,
                            role
                    );
                });
    }

    private void addRequestCard(
            String requestId,
            String fromUid,
            String displayName,
            String game,
            String rank,
            String role
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.parseColor("#D9D9D9")
        );

        cardBackground.setCornerRadius(
                dp(16)
        );

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


        // Player name
        TextView nameText = new TextView(this);

        nameText.setText(displayName);
        nameText.setTextSize(20);
        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        nameText.setTextColor(
                Color.parseColor("#222222")
        );

        card.addView(nameText);


        // Request message
        TextView requestText = new TextView(this);

        requestText.setText(
                "sent you a friend request"
        );

        requestText.setTextSize(14);
        requestText.setTextColor(
                Color.parseColor("#555555")
        );

        requestText.setPadding(
                0,
                dp(4),
                0,
                dp(12)
        );

        card.addView(requestText);


        // Game
        TextView gameText = new TextView(this);

        gameText.setText(game);
        gameText.setTextSize(16);
        gameText.setTypeface(
                null,
                Typeface.BOLD
        );

        card.addView(gameText);


        // Rank / Role
        TextView rankRoleText =
                new TextView(this);

        rankRoleText.setText(
                rank + " · " + role
        );

        rankRoleText.setTextSize(14);

        rankRoleText.setPadding(
                0,
                dp(4),
                0,
                dp(16)
        );

        card.addView(rankRoleText);


        // Buttons row
        LinearLayout buttonRow =
                new LinearLayout(this);

        buttonRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        // Decline
        Button declineButton =
                new Button(this);

        declineButton.setText("Decline");
        declineButton.setAllCaps(false);

        LinearLayout.LayoutParams declineParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                );

        declineParams.setMargins(
                0,
                0,
                dp(6),
                0
        );

        declineButton.setLayoutParams(
                declineParams
        );


        // View Details
        Button viewDetailsButton =
                new Button(this);

        viewDetailsButton.setText(
                "View Details"
        );

        viewDetailsButton.setAllCaps(false);
        viewDetailsButton.setTextColor(
                Color.WHITE
        );

        viewDetailsButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#6C4FB3")
                )
        );

        LinearLayout.LayoutParams viewParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                );

        viewParams.setMargins(
                dp(6),
                0,
                0,
                0
        );

        viewDetailsButton.setLayoutParams(
                viewParams
        );


        declineButton.setOnClickListener(v -> {

            declineRequest(requestId);
        });


        viewDetailsButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    PlayerDetailActivity.class
            );

            intent.putExtra(
                    "PLAYER_UID",
                    fromUid
            );

            intent.putExtra(
                    "REQUEST_ID",
                    requestId
            );

            intent.putExtra(
                    "FROM_NOTIFICATION",
                    true
            );

            startActivity(intent);
        });


        buttonRow.addView(declineButton);
        buttonRow.addView(viewDetailsButton);

        card.addView(buttonRow);

        requestsContainer.addView(card);
    }

    private void declineRequest(
            String requestId
    ) {

        db.collection("playerRequests")
                .document(requestId)
                .update(
                        "status",
                        "declined"
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            NotificationsActivity.this,
                            "Request declined.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadRequests();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            NotificationsActivity.this,
                            "Failed to decline request.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void showNoRequests() {

        TextView noRequests =
                new TextView(this);

        noRequests.setText(
                "No new notifications."
        );

        noRequests.setTextSize(16);
        noRequests.setGravity(
                Gravity.CENTER
        );

        noRequests.setPadding(
                0,
                dp(40),
                0,
                0
        );

        requestsContainer.addView(
                noRequests
        );
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

    private int dp(int value) {

        return (int) (
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
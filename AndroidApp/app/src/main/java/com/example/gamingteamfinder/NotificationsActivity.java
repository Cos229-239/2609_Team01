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
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FirebaseFirestoreException;

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

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    MessagesActivity.class
            );

            startActivity(intent);
        });

        buttonProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    ProfileActivity.class
            );

            startActivity(intent);
        });

        Button buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> {

            Intent intent = new Intent(
                    NotificationsActivity.this,
                    HomeActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadRequests();
        loadTeamJoinRequests();
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


    private void loadTeamJoinRequests() {

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();

        db.collectionGroup("joinRequests")
                .whereEqualTo("ownerId", currentUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot request : snapshot.getDocuments()) {

                        String teamId = request.getString("teamId");
                        String requesterId =
                                request.getString("requesterId");

                        if (teamId == null || requesterId == null) {
                            continue;
                        }

                        loadTeamJoinRequestDetails(
                                request.getReference(),
                                teamId,
                                requesterId
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(
                            "TeamRequests",
                            "Failed to load team requests",
                            e
                    );

                    Toast.makeText(
                            this,
                            "Failed to load team requests",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    private void loadTeamJoinRequestDetails(
            DocumentReference requestRef,
            String teamId,
            String requesterId
    ) {

        db.collection("teams").document(teamId)
                .get()
                .addOnSuccessListener(teamDoc -> {

                    if (!teamDoc.exists()) return;

                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser == null ||
                            !currentUser.getUid().equals(
                                    teamDoc.getString("ownerId"))) {
                        return;
                    }

                    String teamName =
                            teamDoc.getString("teamName");

                    if (teamName == null) {
                        teamName = "Unnamed Team";
                    }

                    final String finalTeamName = teamName;

                    db.collection("users")
                            .document(requesterId)
                            .get()
                            .addOnSuccessListener(userDoc -> {

                                String username =
                                        userDoc.getString("displayName");

                                if (username == null ||
                                        username.trim().isEmpty()) {
                                    username = userDoc.getString("username");
                                }

                                if (username == null ||
                                        username.trim().isEmpty()) {
                                    username = "Player";
                                }

                                addTeamJoinRequestCard(
                                        requestRef,
                                        teamId,
                                        requesterId,
                                        username,
                                        finalTeamName
                                );
                            });
                });
    }


    private void addTeamJoinRequestCard(
            DocumentReference requestRef,
            String teamId,
            String requesterId,
            String username,
            String teamName
    ) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#D9D9D9"));
        background.setCornerRadius(dp(12));
        card.setBackground(background);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(16);
        card.setLayoutParams(cardParams);

        TextView message = new TextView(this);
        message.setText(
                username + " wants to join " + teamName
        );
        message.setTextSize(17);
        message.setTextColor(Color.parseColor("#222222"));
        message.setPadding(0, 0, 0, dp(12));
        card.addView(message);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);

        Button denyButton = new Button(this);
        denyButton.setText("Deny");
        denyButton.setAllCaps(false);
        denyButton.setLayoutParams(
                new LinearLayout.LayoutParams(0, dp(50), 1)
        );

        Button acceptButton = new Button(this);
        acceptButton.setText("Accept");
        acceptButton.setAllCaps(false);
        acceptButton.setTextColor(Color.WHITE);
        acceptButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#6C4FB3")
                )
        );
        acceptButton.setLayoutParams(
                new LinearLayout.LayoutParams(0, dp(50), 1)
        );

        denyButton.setOnClickListener(v -> {
            denyButton.setEnabled(false);
            acceptButton.setEnabled(false);
            respondToTeamRequest(
                    requestRef, teamId, requesterId, false,
                    denyButton, acceptButton, card
            );
        });

        acceptButton.setOnClickListener(v -> {
            denyButton.setEnabled(false);
            acceptButton.setEnabled(false);
            respondToTeamRequest(
                    requestRef, teamId, requesterId, true,
                    denyButton, acceptButton, card
            );
        });

        buttonRow.addView(denyButton);
        buttonRow.addView(acceptButton);
        card.addView(buttonRow);

        requestsContainer.addView(card);
    }


    private void respondToTeamRequest(
            DocumentReference requestRef,
            String teamId,
            String requesterId,
            boolean accept,
            Button denyButton,
            Button acceptButton,
            LinearLayout card
    ) {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference teamRef =
                db.collection("teams").document(teamId);

        DocumentReference memberRef =
                teamRef.collection("members").document(requesterId);

        db.runTransaction(transaction -> {

            DocumentSnapshot team = transaction.get(teamRef);
            DocumentSnapshot request = transaction.get(requestRef);
            DocumentSnapshot member = transaction.get(memberRef);

            if (!team.exists() || !request.exists()) {
                throw new FirebaseFirestoreException(
                        "Team or request not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                );
            }

            if (!user.getUid().equals(team.getString("ownerId")) ||
                    !user.getUid().equals(request.getString("ownerId")) ||
                    !teamId.equals(request.getString("teamId")) ||
                    !requesterId.equals(request.getString("requesterId"))) {
                throw new FirebaseFirestoreException(
                        "Not authorized",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED
                );
            }

            if (!"pending".equals(request.getString("status"))) {
                throw new FirebaseFirestoreException(
                        "Request already processed",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            if (!accept) {
                transaction.update(requestRef, "status", "denied");
                return null;
            }

            Long current = team.getLong("currentPlayers");
            Long maximum = team.getLong("maxPlayers");

            long currentCount = current == null ? 1 : current;
            long maxCount = maximum == null ? 5 : maximum;

            if (currentCount >= maxCount) {
                throw new FirebaseFirestoreException(
                        "Team is full",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                );
            }

            if (member.exists()) {
                throw new FirebaseFirestoreException(
                        "Player is already a member",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS
                );
            }

            java.util.Map<String, Object> memberData =
                    new java.util.HashMap<>();

            memberData.put("userId", requesterId);
            memberData.put("role", "member");
            memberData.put(
                    "joinedAt",
                    FieldValue.serverTimestamp()
            );

            transaction.set(memberRef, memberData);

            transaction.update(
                    teamRef,
                    "memberIds",
                    FieldValue.arrayUnion(requesterId),
                    "currentPlayers",
                    FieldValue.increment(1)
            );

            transaction.update(requestRef, "status", "accepted");

            return null;

        }).addOnSuccessListener(unused -> {

            requestsContainer.removeView(card);

            Toast.makeText(
                    this,
                    accept ? "Player accepted!" : "Request denied.",
                    Toast.LENGTH_SHORT
            ).show();

        }).addOnFailureListener(e -> {

            denyButton.setEnabled(true);
            acceptButton.setEnabled(true);

            Toast.makeText(
                    this,
                    "Could not process request: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
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
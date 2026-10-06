package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class PlayerDetailActivity extends AppCompatActivity {

    private String requestId;
    private boolean fromNotification;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String playerUid;

    private TextView textDetailName;
    private TextView textDetailSummary;
    private TextView textDetailAvatar;
    private TextView textDetailRegion;
    private TextView textDetailAbout;
    private TextView textDetailAvailability;
    private TextView textDetailRiotId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        textDetailName = findViewById(R.id.textDetailName);
        textDetailSummary = findViewById(R.id.textDetailSummary);
        textDetailAvatar = findViewById(R.id.textDetailAvatar);
        textDetailRegion = findViewById(R.id.textDetailRegion);
        textDetailAbout = findViewById(R.id.textDetailAbout);
        textDetailAvailability = findViewById(R.id.textDetailAvailability);
        textDetailRiotId = findViewById(R.id.textDetailRiotId);

        Button buttonClose = findViewById(R.id.buttonClose);
        Button buttonRequestAdd = findViewById(R.id.buttonRequestAdd);

        fromNotification =
                getIntent().getBooleanExtra("FROM_NOTIFICATION", false);

        if (fromNotification) {
            buttonRequestAdd.setText("Accept");
        }

        buttonClose.setOnClickListener(v -> finish());

        buttonRequestAdd.setOnClickListener(v -> {

            if (fromNotification) {

                acceptPlayerRequest();

            } else {

                sendPlayerRequest();
            }
        });

        playerUid = getIntent().getStringExtra("PLAYER_UID");

        requestId = getIntent().getStringExtra("REQUEST_ID");


        if (playerUid == null || playerUid.isEmpty()) {

            Toast.makeText(
                    this,
                    "Player could not be loaded.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        loadPlayer(playerUid);
    }

    private void acceptPlayerRequest() {

        if (requestId == null || requestId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Request could not be found.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

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

        db.collection("playerRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                PlayerDetailActivity.this,
                                "Request could not be found.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String fromUid =
                            documentSnapshot.getString("fromUid");

                    String toUid =
                            documentSnapshot.getString("toUid");

                    if (fromUid == null || toUid == null) {

                        Toast.makeText(
                                PlayerDetailActivity.this,
                                "Invalid request.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    // Always create the same friend document ID
                    // no matter which user is A or B
                    String friendId;

                    if (fromUid.compareTo(toUid) < 0) {
                        friendId = fromUid + "_" + toUid;
                    } else {
                        friendId = toUid + "_" + fromUid;
                    }

                    db.collection("users")
                            .document(fromUid)
                            .get()
                            .addOnSuccessListener(fromUserDoc -> {

                                String fromName =
                                        getValue(fromUserDoc.getString("displayName"));

                                db.collection("users")
                                        .document(toUid)
                                        .get()
                                        .addOnSuccessListener(toUserDoc -> {

                                            String toName =
                                                    getValue(toUserDoc.getString("displayName"));

                                            Map<String, Object> friend =
                                                    new HashMap<>();

                                            // Keep UID list for searching/filtering friends later
                                            friend.put(
                                                    "members",
                                                    Arrays.asList(fromUid, toUid)
                                            );

                                            // Add readable usernames
                                            friend.put("user1Uid", fromUid);
                                            friend.put("user1Name", fromName);

                                            friend.put("user2Uid", toUid);
                                            friend.put("user2Name", toName);

                                            friend.put(
                                                    "createdAt",
                                                    FieldValue.serverTimestamp()
                                            );

                                            db.collection("friends")
                                                    .document(friendId)
                                                    .set(friend)
                                                    .addOnSuccessListener(unused -> {

                                                        db.collection("playerRequests")
                                                                .document(requestId)
                                                                .update("status", "accepted")
                                                                .addOnSuccessListener(unused2 -> {

                                                                    Toast.makeText(
                                                                            PlayerDetailActivity.this,
                                                                            "Player added!",
                                                                            Toast.LENGTH_SHORT
                                                                    ).show();

                                                                    finish();
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {

                                                        Toast.makeText(
                                                                PlayerDetailActivity.this,
                                                                "Failed: " + e.getMessage(),
                                                                Toast.LENGTH_LONG
                                                        ).show();
                                                    });
                                        });
                            });
                });
    }
    private void loadPlayer(String playerUid) {

        db.collection("users")
                .document(playerUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        displayPlayer(documentSnapshot);

                    } else {

                        Toast.makeText(
                                PlayerDetailActivity.this,
                                "Player profile not found.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            PlayerDetailActivity.this,
                            "Failed to load player profile.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void sendPlayerRequest() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please log in first.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String fromUid = currentUser.getUid();

        if (fromUid.equals(playerUid)) {

            Toast.makeText(
                    this,
                    "You cannot send a request to yourself.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String requestId = fromUid + "_" + playerUid;

        // Get sender profile
        db.collection("users")
                .document(fromUid)
                .get()
                .addOnSuccessListener(fromUserDoc -> {

                    String fromName =
                            getValue(fromUserDoc.getString("displayName"));

                    // Get receiver profile
                    db.collection("users")
                            .document(playerUid)
                            .get()
                            .addOnSuccessListener(toUserDoc -> {

                                String toName =
                                        getValue(toUserDoc.getString("displayName"));

                                Map<String, Object> request =
                                        new HashMap<>();

                                request.put("fromUid", fromUid);
                                request.put("fromName", fromName);

                                request.put("toUid", playerUid);
                                request.put("toName", toName);

                                request.put("status", "pending");

                                request.put(
                                        "createdAt",
                                        FieldValue.serverTimestamp()
                                );

                                db.collection("playerRequests")
                                        .document(requestId)
                                        .set(request)
                                        .addOnSuccessListener(unused -> {

                                            Toast.makeText(
                                                    PlayerDetailActivity.this,
                                                    "Request sent!",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        })
                                        .addOnFailureListener(e -> {

                                            Toast.makeText(
                                                    PlayerDetailActivity.this,
                                                    "Failed to send request.",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        });
                            });
                });
    }

    private void displayPlayer(DocumentSnapshot document) {

        String displayName = getValue(
                document.getString("displayName")
        );

        String game = getValue(
                document.getString("game")
        );

        String rank = getValue(
                document.getString("rank")
        );

        String role = getValue(
                document.getString("role")
        );

        String region = getValue(
                document.getString("region")
        );

        String about = getValue(
                document.getString("about")
        );

        String availability = getValue(
                document.getString("availability")
        );

        String riotId = getValue(
                document.getString("riotId")
        );

        textDetailName.setText(displayName);

        textDetailSummary.setText(
                game + " · " + rank + " · " + role
        );

        textDetailRegion.setText(
                "Region: " + region
        );

        textDetailAbout.setText(about);

        textDetailAvailability.setText(
                availability
        );

        textDetailRiotId.setText(riotId);

        textDetailAvatar.setText(
                getInitials(displayName)
        );
    }

    private String getValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Not set";
        }

        return value;
    }

    private String getInitials(String displayName) {

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
}
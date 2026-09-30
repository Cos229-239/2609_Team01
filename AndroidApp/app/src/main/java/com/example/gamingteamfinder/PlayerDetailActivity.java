package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;

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

        textDetailName = findViewById(R.id.textDetailName);
        textDetailSummary = findViewById(R.id.textDetailSummary);
        textDetailAvatar = findViewById(R.id.textDetailAvatar);
        textDetailRegion = findViewById(R.id.textDetailRegion);
        textDetailAbout = findViewById(R.id.textDetailAbout);
        textDetailAvailability = findViewById(R.id.textDetailAvailability);
        textDetailRiotId = findViewById(R.id.textDetailRiotId);

        Button buttonCloseTop = findViewById(R.id.buttonCloseTop);
        Button buttonClose = findViewById(R.id.buttonClose);
        Button buttonRequestAdd = findViewById(R.id.buttonRequestAdd);

        buttonCloseTop.setOnClickListener(v -> finish());
        buttonClose.setOnClickListener(v -> finish());

        buttonRequestAdd.setOnClickListener(v -> {
            Toast.makeText(
                    PlayerDetailActivity.this,
                    "Request feature coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });

        String playerUid = getIntent().getStringExtra("PLAYER_UID");

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
package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView textDisplayName;
    private TextView textRegion;
    private TextView textAbout;
    private TextView textGame;
    private TextView textRiotId;
    private TextView textRank;
    private TextView textRole;
    private TextView textAvailability;
    private TextView textAvatar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Profile views
        textDisplayName = findViewById(R.id.textDisplayName);
        textRegion = findViewById(R.id.textRegion);
        textAbout = findViewById(R.id.textAbout);
        textGame = findViewById(R.id.textGame);
        textRiotId = findViewById(R.id.textRiotId);
        textRank = findViewById(R.id.textRank);
        textRole = findViewById(R.id.textRole);
        textAvailability = findViewById(R.id.textAvailability);
        textAvatar = findViewById(R.id.textAvatar);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonEditProfile = findViewById(R.id.buttonEditProfile);

        ImageButton buttonNavHome = findViewById(R.id.buttonNavHome);
        ImageButton buttonNavSearch = findViewById(R.id.buttonNavSearch);
        ImageButton buttonNavMessages = findViewById(R.id.buttonNavMessages);
        ImageButton buttonNavProfile = findViewById(R.id.buttonNavProfile);

        // Edit Profile
        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    EditProfileActivity.class
            );
            startActivity(intent);
        });

        // Back
        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    HomeActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });

        // Bottom navigation
        buttonNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    HomeActivity.class
            );
            startActivity(intent);
        });

        buttonNavSearch.setOnClickListener(v -> {
            Intent intent = new Intent(
                    ProfileActivity.this,
                    SearchPlayerActivity.class
            );
            startActivity(intent);
        });

        buttonNavMessages.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ProfileActivity.this,
                    MessagesActivity.class
            );

            startActivity(intent);
        });

        buttonNavProfile.setOnClickListener(v -> {
            // Already on Profile screen
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload after returning from Edit Profile
        loadProfile();
    }

    private void loadProfile() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    ProfileActivity.this,
                    "No user is logged in.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        displayProfile(documentSnapshot);

                    } else {

                        // User has an account but has not created a profile yet
                        clearProfile();

                        Toast.makeText(
                                ProfileActivity.this,
                                "Create your profile first.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Failed to load profile.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void displayProfile(DocumentSnapshot document) {

        String displayName = getValue(document, "displayName");
        String region = getValue(document, "region");
        String about = getValue(document, "about");
        String game = getValue(document, "game");
        String riotId = getValue(document, "riotId");
        String rank = getValue(document, "rank");
        String role = getValue(document, "role");
        String availability = getValue(document, "availability");

        textDisplayName.setText(displayName);
        textRegion.setText(region);
        textAbout.setText(about);
        textGame.setText(game);
        textRiotId.setText(riotId);
        textRank.setText(rank);
        textRole.setText(role);
        textAvailability.setText(availability);

        updateAvatar(displayName);
    }

    private String getValue(
            DocumentSnapshot document,
            String field
    ) {

        String value = document.getString(field);

        if (value == null || value.trim().isEmpty()) {
            return "Not set";
        }

        return value;
    }

    private void updateAvatar(String displayName) {

        if (displayName == null
                || displayName.trim().isEmpty()
                || displayName.equals("Not set")) {

            textAvatar.setText("");
            return;
        }

        String[] parts = displayName.trim().split("\\s+");

        String initials;

        if (parts.length >= 2) {

            initials =
                    parts[0].substring(0, 1).toUpperCase()
                            + parts[1].substring(0, 1).toUpperCase();

        } else {

            initials =
                    displayName.substring(0, 1).toUpperCase();
        }

        textAvatar.setText(initials);
    }

    private void clearProfile() {

        textDisplayName.setText("No profile yet");
        textRegion.setText("");
        textAbout.setText("");
        textGame.setText("");
        textRiotId.setText("");
        textRank.setText("");
        textRole.setText("");
        textAvailability.setText("");
        textAvatar.setText("");
    }
}
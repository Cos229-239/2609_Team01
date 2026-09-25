package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

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

        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        loadProfile();


        Button buttonNavHome = findViewById(R.id.buttonNavHome);
        Button buttonNavSearch = findViewById(R.id.buttonNavSearch);
        Button buttonNavMessages = findViewById(R.id.buttonNavMessages);
        Button buttonNavProfile = findViewById(R.id.buttonNavProfile);

        buttonNavHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        buttonNavSearch.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SearchPlayerActivity.class);
            startActivity(intent);
        });

        buttonNavMessages.setOnClickListener(v -> {
            Toast.makeText(
                    ProfileActivity.this,
                    "Messages coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {

        textDisplayName.setText(ProfileData.displayName);
        textRegion.setText(ProfileData.region);
        textAbout.setText(ProfileData.about);

        textGame.setText(ProfileData.game);
        textRiotId.setText(ProfileData.riotId);
        textRank.setText(ProfileData.rank);
        textRole.setText(ProfileData.role);

        textAvailability.setText(ProfileData.availability);

        String displayName = ProfileData.displayName;
        textDisplayName.setText(displayName);

        if (displayName != null && !displayName.trim().isEmpty()) {

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

        } else {
            textAvatar.setText("");
        }
    }
}
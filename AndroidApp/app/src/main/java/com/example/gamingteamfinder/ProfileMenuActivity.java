package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_menu);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonMyProfile = findViewById(R.id.buttonMyProfile);
        Button buttonEditProfile = findViewById(R.id.buttonEditProfile);

        buttonBack.setOnClickListener(v -> finish());

        buttonMyProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileMenuActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileMenuActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
    }
}
package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button buttonLfg = findViewById(R.id.buttonLfg);

        Button buttonProfile = findViewById(R.id.buttonProfile);

        Button buttonFindPlayers = findViewById(R.id.buttonFindPlayers);

        Button buttonTeam = findViewById(R.id.buttonTeam);

        buttonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileMenuActivity.class);
            startActivity(intent);
        });

        buttonFindPlayers.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
        });

        buttonLfg.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LfgActivity.class);
            startActivity(intent);
        });

        buttonTeam.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TeamActivity.class);
            startActivity(intent);
        });
    }
}
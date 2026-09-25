package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TeamActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team);

        Button buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> finish());
    }
}
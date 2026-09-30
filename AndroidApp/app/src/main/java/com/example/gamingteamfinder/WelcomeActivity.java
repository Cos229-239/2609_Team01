package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button buttonEmailLogin = findViewById(R.id.buttonEmailLogin);

        buttonEmailLogin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WelcomeActivity.this,
                    EmailLoginActivity.class
            );

            startActivity(intent);
        });
    }
}
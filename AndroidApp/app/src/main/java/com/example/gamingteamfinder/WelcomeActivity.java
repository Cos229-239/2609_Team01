
package com.example.gamingteamfinder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private boolean keepSplashScreen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen splashScreen =
                SplashScreen.installSplashScreen(this);

        splashScreen.setKeepOnScreenCondition(
                () -> keepSplashScreen
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mAuth = FirebaseAuth.getInstance();

        // Existing Email Login button
        Button buttonEmailLogin =
                findViewById(R.id.buttonEmailLogin);

        buttonEmailLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    WelcomeActivity.this,
                    EmailLoginActivity.class
            );

            startActivity(intent);
        });

        // Check saved Remember Me preference
        SharedPreferences loginPreferences =
                getSharedPreferences(
                        "gtf_login_preferences",
                        MODE_PRIVATE
                );

        boolean rememberMe = loginPreferences.getBoolean(
                "remember_me",
                false
        );

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Remember Me is OFF
        if (!rememberMe) {

            // Clear any previously saved Firebase session
            if (currentUser != null) {
                mAuth.signOut();
            }

            // Show Welcome Screen
            keepSplashScreen = false;
            return;
        }

        // Remember Me is ON
        if (currentUser != null) {

            buttonEmailLogin.setEnabled(false);

            // Refresh email verification status
            currentUser.reload().addOnCompleteListener(task -> {

                FirebaseUser updatedUser =
                        mAuth.getCurrentUser();

                if (task.isSuccessful()
                        && updatedUser != null
                        && updatedUser.isEmailVerified()) {

                    // Automatically open Home
                    Intent intent = new Intent(
                            WelcomeActivity.this,
                            HomeActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();

                } else {

                    // Disable auto-login for unverified accounts
                    if (task.isSuccessful()
                            && updatedUser != null
                            && !updatedUser.isEmailVerified()) {

                        loginPreferences.edit()
                                .putBoolean("remember_me", false)
                                .apply();

                        mAuth.signOut();
                    }

                    buttonEmailLogin.setEnabled(true);

                    // Verification failed or account is not eligible for auto-login
                    keepSplashScreen = false;
                }
            });

        } else {

            // No saved Firebase session
            keepSplashScreen = false;
        }
    }
}

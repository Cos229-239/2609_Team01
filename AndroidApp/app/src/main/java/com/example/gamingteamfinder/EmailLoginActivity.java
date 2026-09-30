package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import android.widget.TextView;

public class EmailLoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editEmail;
    private EditText editPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        // Connect Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Connect Java variables to XML
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        TextView buttonBackToWelcome = findViewById(R.id.buttonBackToWelcome);

        buttonBackToWelcome.setOnClickListener(v -> finish());

        // LOGIN
        buttonLogin.setOnClickListener(v -> loginUser());

        // CREATE ACCOUNT
        buttonCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(
                    EmailLoginActivity.this,
                    SignUpActivity.class
            );

            startActivity(intent);
        });
    }

    private void loginUser() {

        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();

        // Check email
        if (email.isEmpty()) {
            editEmail.setError("Enter your email");
            return;
        }

        // Check password
        if (password.isEmpty()) {
            editPassword.setError("Enter your password");
            return;
        }

        // Firebase Login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                EmailLoginActivity.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                EmailLoginActivity.this,
                                HomeActivity.class
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        Toast.makeText(
                                EmailLoginActivity.this,
                                "Email or password is incorrect.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
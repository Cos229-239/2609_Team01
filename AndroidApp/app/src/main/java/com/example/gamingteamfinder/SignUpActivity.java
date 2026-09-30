package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editEmail;
    private EditText editPassword;
    private EditText editConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Connect Java variables to XML

        editEmail = findViewById(R.id.editSignUpEmail);
        editPassword = findViewById(R.id.editSignUpPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        Button buttonSignUp = findViewById(R.id.buttonSignUp);
        TextView textSignIn = findViewById(R.id.textSignIn);

        // Return to Login screen
        textSignIn.setOnClickListener(v -> finish());

        // Create account
        buttonSignUp.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {


        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString();
        String confirmPassword = editConfirmPassword.getText().toString();



        if (email.isEmpty()) {
            editEmail.setError("Enter your email");
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError("Enter a password");
            return;
        }

        if (confirmPassword.isEmpty()) {
            editConfirmPassword.setError("Confirm your password");
            return;
        }

        // Firebase requires at least 6 characters
        if (password.length() < 6) {
            editPassword.setError("Password must be at least 6 characters");
            return;
        }

        // Check matching passwords
        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Create Firebase account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                SignUpActivity.this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                SignUpActivity.this,
                                HomeActivity.class
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        String errorMessage = "Account creation failed.";

                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(
                                SignUpActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
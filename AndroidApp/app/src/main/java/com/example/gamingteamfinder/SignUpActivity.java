
package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

        // Check email
        if (email.isEmpty()) {
            editEmail.setError("Enter your email");
            return;
        }

        // Check password
        if (password.isEmpty()) {
            editPassword.setError("Enter a password");
            return;
        }

        // Check confirm password
        if (confirmPassword.isEmpty()) {
            editConfirmPassword.setError("Confirm your password");
            return;
        }

        // Firebase requires at least 6 characters
        if (password.length() < 6) {
            editPassword.setError(
                    "Password must be at least 6 characters"
            );
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

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user == null) {

                            Toast.makeText(
                                    SignUpActivity.this,
                                    "Unable to load your new account.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        // Send verification email
                        user.sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {

                                    if (emailTask.isSuccessful()) {

                                        Log.d(
                                                "EMAIL_VERIFY",
                                                "Verification email request succeeded."
                                        );

                                        Toast.makeText(
                                                SignUpActivity.this,
                                                "Account created! Please verify your email.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                    } else {

                                        Exception error =
                                                emailTask.getException();

                                        // Log actual Firebase error
                                        Log.e(
                                                "EMAIL_VERIFY",
                                                "Failed to send verification email",
                                                error
                                        );

                                        Toast.makeText(
                                                SignUpActivity.this,
                                                "Account created, but verification email could not be sent. Please try resending.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }

                                    // Open Email Verification screen
                                    Intent intent = new Intent(
                                            SignUpActivity.this,
                                            VerifyEmailActivity.class
                                    );

                                    // Mark this as a new registration
                                    intent.putExtra("from_signup", true);

                                    intent.setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);
                                    finish();
                                });

                    } else {

                        Log.e(
                                "EMAIL_VERIFY",
                                "Account creation failed",
                                task.getException()
                        );

                        String errorMessage =
                                "Account creation failed.";

                        if (task.getException() != null) {
                            errorMessage =
                                    task.getException().getMessage();
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

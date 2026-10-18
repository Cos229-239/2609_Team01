package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.widget.TextView;


import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;


public class EmailLoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editEmail;
    private EditText editPassword;
    private CheckBox checkRememberMe;
    private SharedPreferences loginPreferences;
    private TextView textRememberWarning;
    private boolean rememberWarningShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        // Connect Firebase Authentication
        mAuth = FirebaseAuth.getInstance();


        // Remember Me
        checkRememberMe = findViewById(R.id.checkRememberMe);


        textRememberWarning = findViewById(R.id.textRememberWarning);

        // Hide warning when Remember Me is checked
        checkRememberMe.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (isChecked) {
                        textRememberWarning.setVisibility(View.GONE);
                    }
                }
        );


        loginPreferences = getSharedPreferences(
                "gtf_login_preferences",
                MODE_PRIVATE
        );


        // Connect Java variables to XML
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        TextView buttonBackToWelcome = findViewById(R.id.buttonBackToWelcome);

        TextView textForgotPassword =
                findViewById(R.id.textForgotPassword);

        textForgotPassword.setOnClickListener(v -> {

            Intent intent = new Intent(
                    EmailLoginActivity.this,
                    ForgotPasswordActivity.class
            );

            // Pass entered email to Forgot Password
            intent.putExtra(
                    "prefill_email",
                    editEmail.getText().toString().trim()
            );

            startActivity(intent);
        });

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



        // Show Remember Me warning without blocking Firebase login
        if (!checkRememberMe.isChecked()) {
            textRememberWarning.setVisibility(View.VISIBLE);
        } else {
            textRememberWarning.setVisibility(View.GONE);
        }


        // Firebase Login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {


                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user == null) {
                            Toast.makeText(
                                    EmailLoginActivity.this,
                                    "Unable to load your account.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        // Refresh email verification status
                        user.reload().addOnCompleteListener(reloadTask -> {

                            if (!reloadTask.isSuccessful()) {

                                Toast.makeText(
                                        EmailLoginActivity.this,
                                        "Unable to verify account status. Please try again.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }


                            FirebaseUser updatedUser = mAuth.getCurrentUser();

                            if (updatedUser != null
                                    && updatedUser.isEmailVerified()) {


                                // Correct credentials, but Remember Me was not selected
                                if (!checkRememberMe.isChecked() && !rememberWarningShown) {

                                    rememberWarningShown = true;

                                    // Stay on Login so the user can choose
                                    // Remember Me or press LOGIN again
                                    return;
                                }

                                textRememberWarning.setVisibility(View.GONE);


                                // Verified email -> Home
                                // Save Remember Me only after successful verified login
                                boolean rememberMe = checkRememberMe.isChecked();

                                loginPreferences.edit()
                                        .putBoolean("remember_me", rememberMe)
                                        .apply();

                                Toast.makeText(
                                        EmailLoginActivity.this,
                                        "Login successful!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                Intent intent = new Intent(
                                        EmailLoginActivity.this,
                                        HomeActivity.class
                                );

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);
                                finish();

                            } else {

                                // Email is not verified
                                showEmailNotVerifiedDialog();
                            }
                        });

                    } else {


                        Toast.makeText(
                                EmailLoginActivity.this,
                                "Email or password is incorrect.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }



    private void showEmailNotVerifiedDialog() {

        Dialog dialog = new Dialog(EmailLoginActivity.this);

        dialog.setContentView(R.layout.dialog_email_not_verified);
        dialog.setCancelable(false);

        // Verify My Email
        dialog.findViewById(R.id.buttonVerifyFromLogin)
                .setOnClickListener(v -> {

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null) {
                        dialog.dismiss();

                        Toast.makeText(
                                EmailLoginActivity.this,
                                "Your login session has ended. Please log in again.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    dialog.dismiss();

                    Intent intent = new Intent(
                            EmailLoginActivity.this,
                            VerifyEmailActivity.class
                    );

                    startActivity(intent);
                    finish();
                });

        // Not Now
        dialog.findViewById(R.id.buttonNotNow)
                .setOnClickListener(v -> {

                    mAuth.signOut();
                    dialog.dismiss();
                });

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            float density = getResources()
                    .getDisplayMetrics().density;

            int screenWidth = getResources()
                    .getDisplayMetrics().widthPixels;

            int dialogWidth = Math.min(
                    (int) (340 * density),
                    screenWidth - (int) (40 * density)
            );

            window.setLayout(
                    dialogWidth,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );

            window.addFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.dimAmount = 0.45f;
            window.setAttributes(params);
        }
    }


}

package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;

import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText editNewPassword;
    private EditText editConfirmPassword;
    private Button buttonUpdatePassword;
    private TextView textSuccess;

    private String resetEmail;
    private String resetToken;
    private long resetDeadline;

    private boolean updating = false;
    private TextView textResetCountdown;
    private CountDownTimer resetTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // Connect XML
        editNewPassword =
                findViewById(R.id.editNewResetPassword);

        editConfirmPassword =
                findViewById(R.id.editConfirmResetPassword);

        buttonUpdatePassword =
                findViewById(R.id.buttonConfirmResetPassword);

        textSuccess =
                findViewById(R.id.textResetPasswordSuccess);

        textResetCountdown =
                findViewById(R.id.textResetPasswordCountdown);

        // Receive information from Verify OTP screen
        resetEmail = getIntent().getStringExtra(
                "otp_reset_email"
        );

        resetToken = getIntent().getStringExtra(
                "otp_reset_token"
        );

        resetDeadline = getIntent().getLongExtra(
                "otp_reset_deadline",
                0L
        );

        // Back to Log in
        findViewById(R.id.buttonResetBackToLogin)
                .setOnClickListener(v -> showBackToLoginConfirmation());

        // Show the same warning when pressing Android Back
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showBackToLoginConfirmation();
                    }
                }
        );

        // Validate reset session
        if (!hasValidResetSession()) {

            Toast.makeText(
                    this,
                    "Reset session is invalid or expired. "
                            + "Please request a new OTP.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // OTP verified, enable password form
        editNewPassword.setEnabled(true);
        editConfirmPassword.setEnabled(true);
        buttonUpdatePassword.setEnabled(true);

        buttonUpdatePassword.setOnClickListener(v ->
                updatePassword()
        );

        // Start the 5-minute reset countdown
        startResetCountdown();
    }


    private void showBackToLoginConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Return to Login?")
                .setMessage(
                        "If you leave now, you will need to "
                                + "request a new OTP and verify it again "
                                + "to reset your password."
                )
                .setNegativeButton(
                        "Stay Here",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Back to Login",
                        (dialog, which) -> openLogin(false)
                )
                .show();
    }


    // Validate the temporary OTP reset session
    private boolean hasValidResetSession() {

        return resetEmail != null
                && !resetEmail.trim().isEmpty()
                && resetToken != null
                && resetToken.matches("(?i)[a-f0-9]{64}")
                && resetDeadline > SystemClock.elapsedRealtime();
    }

    // Change password using Vercel backend
    private void updatePassword() {

        if (updating) {
            return;
        }

        if (!hasValidResetSession()) {

            Toast.makeText(
                    this,
                    "Your reset session has expired. "
                            + "Please request a new OTP.",
                    Toast.LENGTH_LONG
            ).show();

            buttonUpdatePassword.setEnabled(false);
            return;
        }

        String newPassword = editNewPassword.getText()
                .toString();

        String confirmPassword = editConfirmPassword.getText()
                .toString();

        // Empty password
        if (newPassword.isEmpty()) {
            editNewPassword.setError(
                    "Enter your new password"
            );
            return;
        }

        // Password length: 6-128 characters
        if (newPassword.length() < 6
                || newPassword.length() > 128) {

            editNewPassword.setError(
                    "Password must be 6-128 characters"
            );
            return;
        }

        // Confirm password
        if (!newPassword.equals(confirmPassword)) {

            editConfirmPassword.setError(
                    "Passwords do not match"
            );
            return;
        }

        // Prevent repeated requests
        updating = true;

        buttonUpdatePassword.setEnabled(false);
        buttonUpdatePassword.setText("Updating...");

        // Update password through OTP backend
        OtpApiClient.completePasswordReset(
                resetEmail,
                resetToken,
                newPassword,
                (success, message) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    updating = false;

                    if (success) {

                        // Prevent reusing the reset token
                        resetToken = null;
                        resetDeadline = 0L;

                        // Stop countdown
                        if (resetTimer != null) {
                            resetTimer.cancel();
                        }

                        // Clear passwords from the screen
                        editNewPassword.setText("");
                        editConfirmPassword.setText("");

                        // Automatically return to Login
                        openLogin(true);
                        return;

                    } else {

                        buttonUpdatePassword.setText(
                                "Update Password"
                        );

                        buttonUpdatePassword.setEnabled(
                                hasValidResetSession()
                        );

                        Toast.makeText(
                                ResetPasswordActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // Return to Log in

    private void openLogin(boolean passwordChanged) {

        Intent intent = new Intent(
                ResetPasswordActivity.this,
                EmailLoginActivity.class
        );

        // Tell Login screen whether password reset succeeded
        intent.putExtra(
                "password_reset_success",
                passwordChanged
        );

        // Prefill the account email on Login
        intent.putExtra(
                "prefill_email",
                resetEmail
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }



    private void startResetCountdown() {

        long remaining = resetDeadline
                - SystemClock.elapsedRealtime();

        if (remaining <= 0) {
            showResetExpired();
            return;
        }

        if (resetTimer != null) {
            resetTimer.cancel();
        }

        resetTimer = new CountDownTimer(remaining, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                updateResetCountdown();
            }

            @Override
            public void onFinish() {
                showResetExpired();
            }
        };

        resetTimer.start();
        updateResetCountdown();
    }

    private void updateResetCountdown() {

        if (resetToken == null) {
            return;
        }

        long remaining = Math.max(
                0L,
                resetDeadline - SystemClock.elapsedRealtime()
        );

        if (remaining <= 0) {
            showResetExpired();
            return;
        }

        long totalSeconds = (remaining + 999L) / 1000L;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String time = String.format(
                java.util.Locale.US,
                "%02d:%02d",
                minutes,
                seconds
        );

        textResetCountdown.setText(
                "Time Remaining: " + time
        );
    }

    private void showResetExpired() {

        if (resetToken == null) {
            return;
        }

        textResetCountdown.setText(
                "Time expired. Please request a new OTP."
        );

        // Prevent new password reset requests
        buttonUpdatePassword.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (textResetCountdown != null && resetToken != null) {
            updateResetCountdown();
        }
    }

    @Override
    protected void onDestroy() {

        if (resetTimer != null) {
            resetTimer.cancel();
        }

        super.onDestroy();
    }

}

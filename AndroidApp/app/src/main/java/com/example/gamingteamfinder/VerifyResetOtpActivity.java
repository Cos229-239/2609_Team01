
package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.os.CountDownTimer;

import androidx.appcompat.app.AppCompatActivity;

public class VerifyResetOtpActivity extends AppCompatActivity {

    private EditText editResetOtp;
    private Button buttonVerifyResetOtp;

    private TextView textOtpDescription;
    private TextView textOtpCountdown;
    private TextView textOtpRequestsRemaining;
    private TextView textResendOtp;
    private TextView textBackToResetEmail;

    private String resetEmail;
    private boolean verifying = false;

    private static final long OTP_DURATION_MS = 3 * 60 * 1000L;

    private long otpDeadline;
    private CountDownTimer otpTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_verify_reset_otp);

        // Connect Java variables to XML
        editResetOtp = findViewById(R.id.editResetOtp);

        buttonVerifyResetOtp =
                findViewById(R.id.buttonVerifyResetOtp);

        textOtpDescription =
                findViewById(R.id.textOtpDescription);

        textOtpCountdown =
                findViewById(R.id.textOtpCountdown);

        textOtpRequestsRemaining =
                findViewById(R.id.textOtpRequestsRemaining);

        textResendOtp =
                findViewById(R.id.textResendOtp);

        textBackToResetEmail =
                findViewById(R.id.textBackToResetEmail);

        // Receive email from Forgot Password
        resetEmail = getIntent().getStringExtra("reset_email");

        if (resetEmail == null || resetEmail.isEmpty()) {

            Toast.makeText(
                    this,
                    "Email information is missing.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        textOtpDescription.setText(
                "Enter the 6-digit code sent to\n" + resetEmail
        );


        // Keep the countdown deadline during screen rotation
        if (savedInstanceState != null) {
            otpDeadline = savedInstanceState.getLong(
                    "otp_deadline",
                    0L
            );
        }

        if (otpDeadline <= 0L) {
            otpDeadline = SystemClock.elapsedRealtime()
                    + OTP_DURATION_MS;
        }

        startOtpCountdown();


        textOtpRequestsRemaining.setText(
                "Request information is not available yet"
        );

        // Resend will be implemented later
        textResendOtp.setEnabled(false);
        textResendOtp.setAlpha(0.5f);

        // Verify OTP
        buttonVerifyResetOtp.setOnClickListener(v ->
                verifyOtpCode()
        );

        // Back to Forgot Password
        textBackToResetEmail.setOnClickListener(v ->
                finish()
        );
    }

    private void verifyOtpCode() {

        if (verifying) {
            return;
        }

        if (SystemClock.elapsedRealtime() >= otpDeadline) {
            showOtpExpired();
            return;
        }

        String otp = editResetOtp.getText()
                .toString()
                .trim();

        // Empty OTP
        if (otp.isEmpty()) {
            editResetOtp.setError(
                    "Enter your verification code"
            );
            return;
        }

        // Validate 6 digits
        if (!otp.matches("\\d{6}")) {
            editResetOtp.setError(
                    "Enter a valid 6-digit code"
            );
            return;
        }

        // Prevent repeated clicks
        verifying = true;

        buttonVerifyResetOtp.setEnabled(false);
        buttonVerifyResetOtp.setText("Verifying...");

        // Verify OTP with Vercel backend
        OtpApiClient.verifyOtp(
                resetEmail,
                otp,
                (verified, resetToken, expiresInSeconds, message) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    verifying = false;

                    buttonVerifyResetOtp.setEnabled(
                            SystemClock.elapsedRealtime() < otpDeadline
                    );
                    buttonVerifyResetOtp.setText("Verify Code");

                    if (verified
                            && resetToken != null
                            && expiresInSeconds > 0) {

                        Toast.makeText(
                                VerifyResetOtpActivity.this,
                                "OTP verified successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                        // Calculate reset deadline
                        long resetDeadline =
                                SystemClock.elapsedRealtime()
                                        + expiresInSeconds * 1000L;

                        // Open existing Reset Password screen
                        Intent intent = new Intent(
                                VerifyResetOtpActivity.this,
                                ResetPasswordActivity.class
                        );

                        intent.putExtra(
                                "otp_reset_email",
                                resetEmail
                        );

                        intent.putExtra(
                                "otp_reset_token",
                                resetToken
                        );

                        intent.putExtra(
                                "otp_reset_deadline",
                                resetDeadline
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        Toast.makeText(
                                VerifyResetOtpActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    private void startOtpCountdown() {

        long remaining = otpDeadline
                - SystemClock.elapsedRealtime();

        if (remaining <= 0) {
            showOtpExpired();
            return;
        }

        otpTimer = new CountDownTimer(remaining, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdown();
            }

            @Override
            public void onFinish() {
                showOtpExpired();
            }
        };

        otpTimer.start();
        updateCountdown();
    }

    private void updateCountdown() {

        long remaining = Math.max(
                0L,
                otpDeadline - SystemClock.elapsedRealtime()
        );

        if (remaining == 0L) {
            showOtpExpired();
            return;
        }

        long seconds = (remaining + 999L) / 1000L;

        long minutes = seconds / 60;
        long secondsLeft = seconds % 60;

        String time = String.format(
                java.util.Locale.US,
                "%02d:%02d",
                minutes,
                secondsLeft
        );

        textOtpCountdown.setText(
                "Time Remaining: " + time
        );
    }

    private void showOtpExpired() {

        textOtpCountdown.setText(
                "Code expired. Please request a new OTP."
        );

        buttonVerifyResetOtp.setEnabled(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("otp_deadline", otpDeadline);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (otpTimer != null) {
            otpTimer.cancel();
        }

        super.onDestroy();
    }

}


package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editResetEmail;
    private Button buttonSendOtp;
    private TextView textOtpLimitCountdown;
    private CountDownTimer otpLimitTimer;
    private boolean sendingOtp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

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

        // Connect XML components
        editResetEmail = findViewById(R.id.editResetEmail);
        buttonSendOtp = findViewById(R.id.buttonSendOtp);
        textOtpLimitCountdown =
                findViewById(R.id.textOtpLimitCountdown);

        // Prefill email from Login screen
        String emailFromLogin = getIntent()
                .getStringExtra("prefill_email");

        if (emailFromLogin != null
                && !emailFromLogin.isEmpty()) {

            editResetEmail.setText(emailFromLogin);
        }

        // Back to Login
        findViewById(R.id.textReturnToLogin)
                .setOnClickListener(v -> finish());

        // Send OTP
        buttonSendOtp.setOnClickListener(v ->
                sendOtp()
        );

        editResetEmail.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

                updateSendOtpButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        // Check saved OTP request history
        updateSendOtpButton();


    }

    // Request OTP from Vercel backend
    private void sendOtp() {

        String email = editResetEmail.getText()
                .toString()
                .trim();

        // Check empty email
        if (email.isEmpty()) {
            editResetEmail.setError(
                    "Enter your email address"
            );
            return;
        }

        // Check email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editResetEmail.setError(
                    "Enter a valid email address"
            );
            return;
        }


        if (OtpRequestHistory.getRemaining(this, email) <= 0) {

            // Do not send requests during a backend rate limit
            if (OtpRateLimitStore.getRemainingMs(this) > 0L) {
                updateSendOtpButton();
                return;
            }

            updateSendOtpButton();

            Toast.makeText(
                    this,
                    "OTP request limit reached. Please try again later.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // Prevent repeated clicks
        sendingOtp = true;
        buttonSendOtp.setEnabled(false);
        buttonSendOtp.setText("Sending OTP...");

        // Request OTP using Firebase App Check
        OtpApiClient.requestOtp(
                email,
                (accepted, message, statusCode, retryAfterSeconds) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    sendingOtp = false;

                    // Restore button
                    buttonSendOtp.setEnabled(true);
                    buttonSendOtp.setText("Send OTP");


                    if (statusCode == 429) {

                        // Save the backend rate-limit deadline
                        OtpRateLimitStore.saveRateLimit(
                                ForgotPasswordActivity.this,
                                retryAfterSeconds
                        );

                        // Update button state
                        updateSendOtpButton();

                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                "Too many OTP requests. Please try again later.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }





                    if (accepted) {

                        // Save the accepted OTP request on this device
                        OtpRequestHistory.recordAcceptedRequest(
                                ForgotPasswordActivity.this,
                                email
                        );

                        // Update button after saving request
                        updateSendOtpButton();

                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                "Check your email for the verification code.",
                                Toast.LENGTH_SHORT
                        ).show();


                        // Move to Verify OTP screen
                        Intent intent = new Intent(
                                ForgotPasswordActivity.this,
                                VerifyResetOtpActivity.class
                        );

                        // Pass email to OTP verification
                        intent.putExtra(
                                "reset_email",
                                email
                        );

                        startActivity(intent);


                    }
                    else {
                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                }
        );
    }



    private void updateSendOtpButton() {

        // Stop the previous countdown
        if (otpLimitTimer != null) {
            otpLimitTimer.cancel();
            otpLimitTimer = null;
        }

        if (sendingOtp) {
            return;
        }

        String email = editResetEmail.getText()
                .toString()
                .trim();

        // Invalid email: hide countdown
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            textOtpLimitCountdown.setVisibility(View.GONE);

            buttonSendOtp.setEnabled(true);
            buttonSendOtp.setText("Send OTP");

            buttonSendOtp.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#006B68")
                    )
            );

            return;
        }

        int remaining = OtpRequestHistory.getRemaining(
                this,
                email
        );

// Check whether a backend rate limit is active
        long blockedMs = OtpRateLimitStore.getRemainingMs(this);

        if (blockedMs > 0L) {

            textOtpLimitCountdown.setVisibility(View.GONE);

            buttonSendOtp.setEnabled(false);
            buttonSendOtp.setText("Too Many Requests");

            buttonSendOtp.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#A7AAB5")
                    )
            );

            // Recheck automatically when the backend wait ends
            otpLimitTimer = new CountDownTimer(
                    blockedMs,
                    blockedMs
            ) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // No countdown displayed for HTTP 429
                }

                @Override
                public void onFinish() {
                    updateSendOtpButton();
                }
            };

            otpLimitTimer.start();
            return;
        }



        long remainingMs =
                OtpRequestHistory.getTimeUntilNextRequestMs(
                        this,
                        email
                );

        // Local request limit reached
        if (remaining <= 0 && remainingMs > 0) {

            buttonSendOtp.setEnabled(false);
            buttonSendOtp.setText("OTP Limit Reached");

            // Grey button
            buttonSendOtp.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#A7AAB5")
                    )
            );

            textOtpLimitCountdown.setVisibility(View.VISIBLE);

            updateLimitCountdownText(remainingMs);

            // Start countdown until the oldest request expires
            otpLimitTimer = new CountDownTimer(
                    remainingMs,
                    1000
            ) {

                @Override
                public void onTick(long millisUntilFinished) {

                    long timeLeft =
                            OtpRequestHistory.getTimeUntilNextRequestMs(
                                    ForgotPasswordActivity.this,
                                    email
                            );

                    if (timeLeft <= 0) {
                        cancel();
                        updateSendOtpButton();
                        return;
                    }

                    updateLimitCountdownText(timeLeft);
                }

                @Override
                public void onFinish() {
                    updateSendOtpButton();
                }
            };

            otpLimitTimer.start();
            return;
        }

        // Local requests are available again
        textOtpLimitCountdown.setVisibility(View.GONE);

        buttonSendOtp.setEnabled(true);
        buttonSendOtp.setText("Send OTP");

        buttonSendOtp.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#006B68")
                )
        );
    }


    private void updateLimitCountdownText(long remainingMs) {

        long totalSeconds = (remainingMs + 999L) / 1000L;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String time = String.format(
                Locale.US,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );

        textOtpLimitCountdown.setText(
                "Next OTP available in: " + time
        );
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (editResetEmail != null && buttonSendOtp != null) {
            updateSendOtpButton();
        }
    }

    @Override
    protected void onPause() {

        if (otpLimitTimer != null) {
            otpLimitTimer.cancel();
            otpLimitTimer = null;
        }

        super.onPause();
    }




}

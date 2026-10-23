
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



        // Check backend rate limit first
        if (OtpRateLimitStore.getRemainingMs(this) > 0L) {

            updateSendOtpButton();

            Toast.makeText(
                    this,
                    "Too many OTP requests. Please try again later.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }



        // Check the locally recorded 60-second cooldown
        if (OtpRequestHistory.getCooldownRemainingMs(this, email) > 0L) {

            updateSendOtpButton();

            Toast.makeText(
                    this,
                    "Please wait before requesting another OTP.",
                    Toast.LENGTH_SHORT
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
                                "Request received. Check your email if a code was sent.",
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

        // Stop the previous timer
        if (otpLimitTimer != null) {
            otpLimitTimer.cancel();
            otpLimitTimer = null;
        }

        if (sendingOtp) {
            return;
        }

        // Do not display an inaccurate local 24-hour countdown
        textOtpLimitCountdown.setVisibility(View.GONE);

        // Check backend IP/global rate-limit deadline
        long blockedMs = OtpRateLimitStore.getRemainingMs(this);

        if (blockedMs > 0L) {

            buttonSendOtp.setEnabled(false);
            buttonSendOtp.setText("Too Many Requests");

            // Grey button
            buttonSendOtp.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#A7AAB5")
                    )
            );

            // Automatically unlock after the backend rate limit expires
            otpLimitTimer = new CountDownTimer(
                    blockedMs,
                    1000
            ) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // No visible countdown for HTTP 429
                }

                @Override
                public void onFinish() {
                    otpLimitTimer = null;
                    updateSendOtpButton();
                }
            };

            otpLimitTimer.start();
            return;
        }

        // Normal button state
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


package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import android.os.CountDownTimer;

import androidx.activity.OnBackPressedCallback;
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
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;

    private long resendDeadline;
    private CountDownTimer resendTimer;
    private boolean sendingOtp = false;
    private final Handler rateLimitHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable rateLimitRefresh = () -> {
        if (resetEmail != null) {
            updateRequestCount();
            updateResendCountdown();
        }
    };

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


        updateRequestCount();

        // Start the initial 60-second resend cooldown
        if (savedInstanceState != null) {
            resendDeadline = savedInstanceState.getLong(
                    "resend_deadline",
                    0L
            );
        }

        if (resendDeadline <= 0L) {
            resendDeadline = SystemClock.elapsedRealtime()
                    + RESEND_COOLDOWN_MS;
        }

        startResendCountdown();

        // Resend a new OTP
        textResendOtp.setOnClickListener(v ->
                resendOtp()
        );

        // Verify OTP
        buttonVerifyResetOtp.setOnClickListener(v ->
                verifyOtpCode()
        );

        // Back to Forgot Password
        textBackToResetEmail.setOnClickListener(v ->
                attemptBackToForgotPassword()
        );

        // Handle Android system Back button / gesture
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        attemptBackToForgotPassword();
                    }
                }
        );
    }

    private void verifyOtpCode() {

        if (verifying) {
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
                (verified, resetToken, expiresInSeconds,
                 message, remaining, maxRequests) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    verifying = false;

                    buttonVerifyResetOtp.setEnabled(true);

                    buttonVerifyResetOtp.setText("Verify Code");

                    if (verified
                            && resetToken != null
                            && expiresInSeconds > 0) {


                        String successMessage = "OTP verified successfully!";

                        // Only display the server quota if it was
                        // returned after successful OTP verification.
                        if (remaining >= 0
                                && maxRequests == 10
                                && remaining <= maxRequests) {

                            successMessage =
                                    "OTP verified! Requests remaining: "
                                            + remaining
                                            + "/"
                                            + maxRequests;
                        }

                        Toast.makeText(
                                VerifyResetOtpActivity.this,
                                successMessage,
                                Toast.LENGTH_LONG
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

                        // Pass the verified server quota when available
                        intent.putExtra("otp_remaining", remaining);
                        intent.putExtra("otp_max_requests", maxRequests);

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
                "Estimated code time ended. Check your email for a newer code."
        );

        // Backend verifies the actual OTP expiration.
        // Do not block a potentially valid newer OTP.
        if (!verifying) {
            buttonVerifyResetOtp.setEnabled(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("otp_deadline", otpDeadline);
        outState.putLong("resend_deadline", resendDeadline);
        super.onSaveInstanceState(outState);
    }


    private void attemptBackToForgotPassword() {

        long remainingMs = Math.max(
                0L,
                resendDeadline - SystemClock.elapsedRealtime()
        );

        // Resend cooldown is still active
        if (remainingMs > 0L) {

            long seconds = (remainingMs + 999L) / 1000L;

            Toast.makeText(
                    this,
                    "Please wait " + seconds
                            + " seconds before going back.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Cooldown finished: allow going back
        finish();
    }


    @Override
    protected void onDestroy() {
        if (otpTimer != null) {
            otpTimer.cancel();
        }

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        super.onDestroy();
        rateLimitHandler.removeCallbacks(rateLimitRefresh);
    }


    private void startResendCountdown() {

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        long remaining = resendDeadline
                - SystemClock.elapsedRealtime();

        if (remaining <= 0) {
            updateResendCountdown();
            return;
        }

        resendTimer = new CountDownTimer(remaining, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                updateResendCountdown();
            }

            @Override
            public void onFinish() {
                updateResendCountdown();
            }
        };

        resendTimer.start();
        updateResendCountdown();
    }

    private void updateResendCountdown() {


        rateLimitHandler.removeCallbacks(rateLimitRefresh);

        // Check backend rate-limit status
        long blockedMs = OtpRateLimitStore.getRemainingMs(this);

        if (blockedMs > 0L) {

            textResendOtp.setText("Too Many Requests");
            textResendOtp.setEnabled(false);
            textResendOtp.setAlpha(0.5f);

            // Refresh automatically when the block expires
            rateLimitHandler.postDelayed(
                    rateLimitRefresh,
                    blockedMs
            );

            return;
        }




        long remaining = Math.max(
                0L,
                resendDeadline - SystemClock.elapsedRealtime()
        );

        if (remaining > 0) {

            long seconds = (remaining + 999L) / 1000L;

            textResendOtp.setText(
                    "Resend Code (" + seconds + "s)"
            );

            textResendOtp.setEnabled(false);
            textResendOtp.setAlpha(0.5f);

        } else {

            textResendOtp.setText(
                    sendingOtp ? "Sending..." : "Resend Code"
            );

            textResendOtp.setEnabled(
                    !sendingOtp && !verifying
            );

            textResendOtp.setAlpha(
                    sendingOtp || verifying ? 0.5f : 1.0f
            );
        }
    }


    private void resendOtp() {



        // Prevent repeated requests
        if (sendingOtp || verifying) {
            return;
        }

        // Do not resend during a backend rate limit
        if (OtpRateLimitStore.getRemainingMs(this) > 0L) {
            updateResendCountdown();
            return;
        }

        // Check 60-second cooldown
        if (SystemClock.elapsedRealtime() < resendDeadline) {
            return;
        }

        sendingOtp = true;

        textResendOtp.setEnabled(false);
        textResendOtp.setAlpha(0.5f);
        textResendOtp.setText("Sending...");

        // Request another OTP from Vercel
        OtpApiClient.requestOtp(
                resetEmail,
                (accepted, message, statusCode, retryAfterSeconds) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    sendingOtp = false;

                    if (statusCode == 429) {

                        // Save backend rate-limit information
                        OtpRateLimitStore.saveRateLimit(
                                VerifyResetOtpActivity.this,
                                retryAfterSeconds
                        );

                        // Disable Resend Code
                        updateResendCountdown();

                        Toast.makeText(
                                VerifyResetOtpActivity.this,
                                "Too many OTP requests. Please try again later.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }




                    if (accepted) {

                        // Record a request accepted by the API on this device.
                        // HTTP 202 does not guarantee an OTP was sent.
                        OtpRequestHistory.recordAcceptedRequest(
                                VerifyResetOtpActivity.this,
                                resetEmail
                        );

                        // Refresh the locally estimated remaining count
                        updateRequestCount();

                        // Start another 60-second cooldown
                        resendDeadline =
                                SystemClock.elapsedRealtime()
                                        + RESEND_COOLDOWN_MS;


                        // Restart the estimated 3-minute OTP timer
                        otpDeadline =
                                SystemClock.elapsedRealtime()
                                        + OTP_DURATION_MS;


                        // Stop old OTP countdown
                        if (otpTimer != null) {
                            otpTimer.cancel();
                        }

                        // Start new countdowns
                        startOtpCountdown();
                        startResendCountdown();

                        buttonVerifyResetOtp.setEnabled(true);

                        Toast.makeText(
                                VerifyResetOtpActivity.this,
                                "Request received. Check your email if a new code was sent.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        // Allow retry if request fails
                        updateResendCountdown();

                        Toast.makeText(
                                VerifyResetOtpActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    private void updateRequestCount() {

        int remaining = OtpRequestHistory.getRemaining(
                this,
                resetEmail
        );

        textOtpRequestsRemaining.setText(
                "OTP Requests Remaining: "
                        + remaining
                        + "/10"
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (textResendOtp != null && resetEmail != null) {
            updateRequestCount();
            updateResendCountdown();
        }
    }

    @Override
    protected void onPause() {
        rateLimitHandler.removeCallbacks(rateLimitRefresh);
        super.onPause();
    }




}

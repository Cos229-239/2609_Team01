
package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editResetEmail;
    private Button buttonSendOtp;

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

        // Prevent repeated clicks
        buttonSendOtp.setEnabled(false);
        buttonSendOtp.setText("Sending OTP...");

        // Request OTP using Firebase App Check
        OtpApiClient.requestOtp(
                email,
                (accepted, message) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    // Restore button
                    buttonSendOtp.setEnabled(true);
                    buttonSendOtp.setText("Send OTP");



                    if (accepted) {

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
}

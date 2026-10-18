
package com.example.gamingteamfinder;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editResetEmail;
    private Button buttonSendResetEmail;
    private LinearLayout layoutResetStatus;


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

        mAuth = FirebaseAuth.getInstance();

        // Connect XML
        editResetEmail = findViewById(R.id.editResetEmail);

        buttonSendResetEmail =
                findViewById(R.id.buttonSendResetEmail);

        // Prefill email from Login screen
        String emailFromLogin = getIntent()
                .getStringExtra("prefill_email");

        if (emailFromLogin != null
                && !emailFromLogin.isEmpty()) {

            editResetEmail.setText(emailFromLogin);
        }


        findViewById(R.id.textReturnToLogin)
                .setOnClickListener(v -> finish());

        // Send reset link
        buttonSendResetEmail.setOnClickListener(v ->
                sendPasswordReset()
        );
        layoutResetStatus = findViewById(R.id.layoutResetStatus);

    }

    private void sendPasswordReset() {

        String email = editResetEmail.getText()
                .toString().trim();

        // Empty email
        if (email.isEmpty()) {
            editResetEmail.setError(
                    "Enter your email address"
            );
            return;
        }

        // Invalid email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editResetEmail.setError(
                    "Enter a valid email address"
            );
            return;
        }

        // Prevent repeated clicks
        buttonSendResetEmail.setEnabled(false);
        buttonSendResetEmail.setText("Sending...");
        layoutResetStatus.setVisibility(View.GONE);




        // Send password reset link
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {


                    buttonSendResetEmail.setEnabled(true);
                    buttonSendResetEmail.setText("Send Reset Link");

                    if (task.isSuccessful()) {

                        // Show success message inside Forgot Password screen
                        layoutResetStatus.setVisibility(View.VISIBLE);

                    } else {

                        Log.e(
                                "PASSWORD_RESET",
                                "Unable to send password reset request",
                                task.getException()
                        );

                        if (task.getException() instanceof
                                FirebaseTooManyRequestsException) {

                            Toast.makeText(
                                    ForgotPasswordActivity.this,
                                    "Too many requests. Please try again later.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } else {

                            Toast.makeText(
                                    ForgotPasswordActivity.this,
                                    "Unable to process the reset request. "
                                            + "Please try again later.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }


                });

    }
}


package com.example.gamingteamfinder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class ResetPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText editNewPassword;
    private EditText editConfirmPassword;
    private Button buttonUpdatePassword;
    private TextView textSuccess;

    private String resetCode;

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

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Connect XML
        editNewPassword =
                findViewById(R.id.editNewResetPassword);

        editConfirmPassword =
                findViewById(R.id.editConfirmResetPassword);

        buttonUpdatePassword =
                findViewById(R.id.buttonConfirmResetPassword);

        textSuccess =
                findViewById(R.id.textResetPasswordSuccess);

        // Disable password form until the reset link is verified
        editNewPassword.setEnabled(false);
        editConfirmPassword.setEnabled(false);
        buttonUpdatePassword.setEnabled(false);

        // Back to Login
        findViewById(R.id.buttonResetBackToLogin)
                .setOnClickListener(v -> openLogin());

        // Update Password
        buttonUpdatePassword.setOnClickListener(v ->
                updatePassword()
        );

        // Read the password reset link
        resetCode = extractResetCode(getIntent().getData());

        if (resetCode == null || resetCode.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please open a valid password reset link.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Password reset is intended for signed-out users
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        verifyResetLink();
    }

    // Extract Firebase code from the email link
    private String extractResetCode(Uri uri) {

        for (int i = 0; i < 4 && uri != null; i++) {

            String code = uri.getQueryParameter("oobCode");
            String mode = uri.getQueryParameter("mode");

            if (code != null && mode != null
                    && mode.equals("resetPassword")) {

                return code;
            }

            // Firebase Hosting links may wrap the action URL
            String nestedLink = uri.getQueryParameter("link");

            if (nestedLink == null) {
                nestedLink = uri.getQueryParameter("deep_link_id");
            }

            if (nestedLink == null) {
                break;
            }

            uri = Uri.parse(nestedLink);
        }

        return null;
    }

    // Verify Firebase reset code
    private void verifyResetLink() {

        mAuth.verifyPasswordResetCode(resetCode)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        editNewPassword.setEnabled(true);
                        editConfirmPassword.setEnabled(true);
                        buttonUpdatePassword.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Reset link verified. Enter your new password.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                "This reset link is invalid or expired. "
                                        + "Please request a new one.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // Change Password
    private void updatePassword() {

        if (resetCode == null) {
            return;
        }

        String newPassword = editNewPassword.getText()
                .toString();

        String confirmPassword = editConfirmPassword.getText()
                .toString();

        // Check empty password
        if (newPassword.isEmpty()) {
            editNewPassword.setError("Enter your new password");
            return;
        }

        // Minimum password length
        if (newPassword.length() < 6) {
            editNewPassword.setError(
                    "Password must be at least 6 characters"
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

        buttonUpdatePassword.setEnabled(false);
        buttonUpdatePassword.setText("Updating...");

        // Update Firebase Authentication password
        mAuth.confirmPasswordReset(resetCode, newPassword)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        // Prevent submitting the same code again
                        resetCode = null;

                        editNewPassword.setText("");
                        editConfirmPassword.setText("");

                        editNewPassword.setEnabled(false);
                        editConfirmPassword.setEnabled(false);

                        buttonUpdatePassword.setText(
                                "Password Updated"
                        );

                        // Show success message
                        textSuccess.setVisibility(View.VISIBLE);

                        Toast.makeText(
                                this,
                                "Password changed successfully!",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        buttonUpdatePassword.setEnabled(true);
                        buttonUpdatePassword.setText(
                                "Update Password"
                        );

                        String message =
                                "Unable to update password. "
                                        + "Please try again.";

                        if (task.getException() instanceof
                                FirebaseAuthWeakPasswordException) {

                            message =
                                    "Password is too weak. "
                                            + "Choose a stronger password.";
                        }

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // Return to Login
    private void openLogin() {

        Intent intent = new Intent(
                ResetPasswordActivity.this,
                EmailLoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

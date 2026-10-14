
package com.example.gamingteamfinder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationChangeEmailActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private TextView textCurrentEmail;
    private EditText editNewEmail;
    private Button buttonSendVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration_change_email);

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
        textCurrentEmail =
                findViewById(R.id.textCurrentRegistrationEmail);

        editNewEmail =
                findViewById(R.id.editRegistrationNewEmail);

        buttonSendVerification =
                findViewById(R.id.buttonUpdateRegistrationEmail);

        // Back to Verify Email
        findViewById(R.id.buttonRegistrationEmailBack)
                .setOnClickListener(v -> finish());

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Display current email
        String currentEmail = user.getEmail();

        textCurrentEmail.setText(
                currentEmail != null ? currentEmail : ""
        );

        // Prefill old email so it can be edited immediately
        if (currentEmail != null) {
            editNewEmail.setText(currentEmail);
            editNewEmail.selectAll();
        }

        // Automatically focus the email field
        editNewEmail.requestFocus();

        // Send verification to new email
        buttonSendVerification.setOnClickListener(v ->
                sendNewEmailVerification()
        );
    }

    private void sendNewEmailVerification() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Your session has ended. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        String newEmail = editNewEmail.getText()
                .toString().trim();

        String currentEmail = user.getEmail();

        // Check empty email
        if (newEmail.isEmpty()) {
            editNewEmail.setError("Enter your new email");
            return;
        }

        // Check email format
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            editNewEmail.setError("Enter a valid email address");
            return;
        }

        // Check if email is unchanged
        if (currentEmail != null
                && newEmail.equalsIgnoreCase(currentEmail)) {

            editNewEmail.setError(
                    "Please enter a different email address"
            );

            return;
        }

        buttonSendVerification.setEnabled(false);

        // Firebase sends a verification link to the NEW email.
        // The account email changes only after the link is opened.
        user.verifyBeforeUpdateEmail(newEmail)
                .addOnCompleteListener(task -> {

                    buttonSendVerification.setEnabled(true);

                    if (task.isSuccessful()) {

                        // Remember which email is waiting for verification
                        SharedPreferences prefs =
                                getSharedPreferences(
                                        "verification_email_state",
                                        MODE_PRIVATE
                                );

                        prefs.edit()
                                .putString(
                                        "pending_email_" + user.getUid(),
                                        newEmail
                                )
                                .putLong(
                                        "last_sent_" + user.getUid(),
                                        SystemClock.elapsedRealtime()
                                )
                                .apply();

                        Toast.makeText(
                                this,
                                "Verification email sent to " + newEmail,
                                Toast.LENGTH_LONG
                        ).show();

                        // Return to the existing Verify Email screen
                        finish();

                    } else {

                        Exception error = task.getException();

                        String message =
                                "Unable to send verification email.";

                        if (error instanceof
                                FirebaseAuthRecentLoginRequiredException) {

                            message = "For security, please sign in again "
                                    + "before changing your email.";

                        } else if (error instanceof
                                FirebaseAuthUserCollisionException) {

                            message = "This email address is already in use.";
                        }

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}

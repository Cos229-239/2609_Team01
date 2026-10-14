
package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.content.SharedPreferences;
import com.google.android.gms.tasks.Task;

public class VerifyEmailActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private boolean fromSignUp;
    private TextView textVerificationEmail;
    private TextView buttonCheckVerification;
    private TextView buttonResendVerification;

    private long lastResendTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_email);

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

        fromSignUp = getIntent().getBooleanExtra(
                "from_signup",
                false
        );


        // Connect XML
        textVerificationEmail =
                findViewById(R.id.textVerificationEmail);

        buttonCheckVerification =
                findViewById(R.id.buttonCheckVerification);

        buttonResendVerification =
                findViewById(R.id.buttonResendVerification);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        // Display current email
        textVerificationEmail.setText(user.getEmail());

        // Check Email Verification
        buttonCheckVerification.setOnClickListener(v -> {
            checkEmailVerification();
        });

        // Resend Email Verification
        buttonResendVerification.setOnClickListener(v -> {
            resendVerificationEmail();
        });

        // Back behavior depends on where the user came from
        findViewById(R.id.buttonVerifyBack)
                .setOnClickListener(v -> handleVerificationBack());

// Handle Android system Back button too
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleVerificationBack();
                    }
                }
        );

        // Open Change Email Screen
        findViewById(R.id.buttonChangeVerificationEmail)
                .setOnClickListener(v -> {

                    Intent intent = new Intent(
                            VerifyEmailActivity.this,
                            RegistrationChangeEmailActivity.class
                    );

                    startActivity(intent);
                });


        // Cancel Sign Up button
        TextView buttonCancelSignUp =
                findViewById(R.id.buttonCancelSignUp);

        // Only show Cancel Sign Up for new registrations
        if (fromSignUp) {

            buttonCancelSignUp.setVisibility(View.VISIBLE);

            buttonCancelSignUp.setOnClickListener(v ->
                    showCancelSignUpDialog()
            );

        } else {

            // Users coming from Login cannot delete their account here
            buttonCancelSignUp.setVisibility(View.GONE);
        }

    }

    private void handleVerificationBack() {

        if (fromSignUp) {

            showCancelSignUpDialog();

        } else {

            backToLogin();
        }
    }


    private void showCancelSignUpDialog() {

        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.dialog_cancel_signup);
        dialog.setCancelable(false);

        TextView buttonKeep =
                dialog.findViewById(R.id.buttonKeepSigningUp);

        TextView buttonCancel =
                dialog.findViewById(R.id.buttonConfirmCancelSignup);

        // Keep Signing Up
        buttonKeep.setOnClickListener(v -> dialog.dismiss());

        // Cancel Sign Up
        buttonCancel.setOnClickListener(v -> {

            // Prevent repeated clicks while Firebase processes the request
            buttonKeep.setEnabled(false);
            buttonCancel.setEnabled(false);
            buttonCancel.setText("Cancelling...");

            cancelUnverifiedRegistration(
                    dialog,
                    buttonKeep,
                    buttonCancel
            );
        });

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );

            float density =
                    getResources().getDisplayMetrics().density;

            int screenWidth =
                    getResources().getDisplayMetrics().widthPixels;

            int dialogWidth = Math.min(
                    (int) (340 * density),
                    screenWidth - (int) (40 * density)
            );

            window.setLayout(
                    dialogWidth,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.dimAmount = 0.45f;

            window.addFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            );

            window.setAttributes(params);
        }
    }


    private void cancelUnverifiedRegistration(
            Dialog dialog,
            TextView buttonKeep,
            TextView buttonCancel) {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            dialog.dismiss();
            openLogin();
            return;
        }

        // Refresh status before deleting to protect verified accounts
        user.reload().addOnCompleteListener(reloadTask -> {

            if (!reloadTask.isSuccessful()) {

                resetCancelButtons(buttonKeep, buttonCancel);

                Toast.makeText(
                        this,
                        "Unable to check account status. Please try again.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            FirebaseUser updatedUser = mAuth.getCurrentUser();

            if (updatedUser == null) {

                dialog.dismiss();
                openLogin();
                return;
            }

            // Never delete a verified account
            if (updatedUser.isEmailVerified()) {

                resetCancelButtons(buttonKeep, buttonCancel);

                dialog.dismiss();

                Toast.makeText(
                        this,
                        "Your email is already verified. "
                                + "You can continue to your account.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String userId = updatedUser.getUid();

            // Delete the Firebase Authentication account
            updatedUser.delete().addOnCompleteListener(deleteTask -> {

                if (deleteTask.isSuccessful()) {

                    // Clear locally stored pending email information
                    SharedPreferences prefs = getSharedPreferences(
                            "verification_email_state",
                            MODE_PRIVATE
                    );

                    prefs.edit()
                            .remove("pending_email_" + userId)
                            .remove("last_sent_" + userId)
                            .apply();

                    mAuth.signOut();

                    dialog.dismiss();

                    Toast.makeText(
                            this,
                            "Sign up cancelled successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    openLogin();

                } else {

                    resetCancelButtons(buttonKeep, buttonCancel);

                    Exception error = deleteTask.getException();

                    String message =
                            "Unable to cancel sign up. Please try again.";

                    if (error instanceof
                            FirebaseAuthRecentLoginRequiredException) {

                        message = "For security, please sign in again "
                                + "before cancelling your registration.";
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });
    }

    // Restore dialog buttons after a failed request
    private void resetCancelButtons(
            TextView buttonKeep,
            TextView buttonCancel) {

        buttonKeep.setEnabled(true);
        buttonCancel.setEnabled(true);
        buttonCancel.setText("Cancel Sign Up");
    }




    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(
                "verification_email_state",
                MODE_PRIVATE
        );

        String pendingEmail = prefs.getString(
                "pending_email_" + user.getUid(),
                null
        );

        if (pendingEmail != null) {

            // Show the new email waiting for verification
            textVerificationEmail.setText(pendingEmail);

        } else {

            // Show the current Firebase email
            textVerificationEmail.setText(user.getEmail());
        }

        long savedSendTime = prefs.getLong(
                "last_sent_" + user.getUid(),
                0
        );

        if (savedSendTime > 0) {
            lastResendTime = savedSendTime;
        }
    }


    // Check if email has been verified
    private void checkEmailVerification() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        buttonCheckVerification.setEnabled(false);

        // Refresh Firebase user information
        user.reload().addOnCompleteListener(task -> {

            buttonCheckVerification.setEnabled(true);

            if (!task.isSuccessful()) {

                Toast.makeText(
                        this,
                        "Unable to check verification. Please try again.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            FirebaseUser updatedUser = mAuth.getCurrentUser();

            SharedPreferences prefs = getSharedPreferences(
                    "verification_email_state",
                    MODE_PRIVATE
            );

            String pendingEmail = updatedUser == null ? null :
                    prefs.getString(
                            "pending_email_" + updatedUser.getUid(),
                            null
                    );

            if (updatedUser != null
                    && updatedUser.isEmailVerified()
                    && (pendingEmail == null
                    || pendingEmail.equalsIgnoreCase(updatedUser.getEmail()))) {

                // Clear pending email after successful verification
                prefs.edit()
                        .remove("pending_email_" + updatedUser.getUid())
                        .remove("last_sent_" + updatedUser.getUid())
                        .apply();

                Toast.makeText(
                        this,
                        "Email verified successfully!",
                        Toast.LENGTH_SHORT
                ).show();

                openHome();

            } else {

                Toast.makeText(
                        this,
                        "Your email is not verified yet. Please check your inbox.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // Resend verification email

    private void resendVerificationEmail() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        long currentTime = SystemClock.elapsedRealtime();

        // Prevent repeated requests within 60 seconds
        if (lastResendTime > 0
                && currentTime >= lastResendTime
                && currentTime - lastResendTime < 60000) {

            Toast.makeText(
                    this,
                    "Please wait before requesting another email.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        SharedPreferences prefs = getSharedPreferences(
                "verification_email_state",
                MODE_PRIVATE
        );

        String pendingEmail = prefs.getString(
                "pending_email_" + user.getUid(),
                null
        );

        buttonResendVerification.setEnabled(false);

        Task<Void> sendTask;

        if (pendingEmail != null) {

            // Resend verification to the NEW email
            sendTask = user.verifyBeforeUpdateEmail(pendingEmail);

        } else {

            // Normal signup email verification
            sendTask = user.sendEmailVerification();
        }

        sendTask.addOnCompleteListener(task -> {

            buttonResendVerification.setEnabled(true);

            if (task.isSuccessful()) {

                lastResendTime = SystemClock.elapsedRealtime();

                prefs.edit()
                        .putLong(
                                "last_sent_" + user.getUid(),
                                lastResendTime
                        )
                        .apply();

                Toast.makeText(
                        this,
                        "Verification email sent! Check your inbox.",
                        Toast.LENGTH_LONG
                ).show();

            } else {

                Toast.makeText(
                        this,
                        "Unable to resend verification email. Please try again later.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // Back to Log in
    private void backToLogin() {

        mAuth.signOut();
        openLogin();
    }

    private void openLogin() {

        Intent intent = new Intent(
                VerifyEmailActivity.this,
                EmailLoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // Open Home after verification
    private void openHome() {

        Intent intent = new Intent(
                VerifyEmailActivity.this,
                HomeActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

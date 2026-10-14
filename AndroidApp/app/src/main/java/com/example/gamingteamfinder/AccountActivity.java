
package com.example.gamingteamfinder;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);

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

        // Back to Settings
        findViewById(R.id.buttonAccountBack).setOnClickListener(v -> {

            Intent intent = new Intent(
                    AccountActivity.this,
                    SettingsActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
        });

        // Open custom Log Out dialog
        findViewById(R.id.accountLogout).setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {

        Dialog dialog = new Dialog(AccountActivity.this);
        dialog.setContentView(R.layout.dialog_logout);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        dialog.show();

        if (window != null) {

            int margin = (int) (
                    48 * getResources().getDisplayMetrics().density
            );

            int width = getResources()
                    .getDisplayMetrics().widthPixels - margin;

            window.setLayout(
                    width,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );

            window.addFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
            );

            WindowManager.LayoutParams params =
                    window.getAttributes();

            params.dimAmount = 0.55f;
            window.setAttributes(params);
        }

        // Cancel Log Out
        dialog.findViewById(R.id.buttonCancelLogout)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                });


        // Confirm Log Out
        View confirmButton =
                dialog.findViewById(R.id.buttonConfirmLogout);

        confirmButton.setOnClickListener(v -> {

            confirmButton.setEnabled(false);
            dialog.findViewById(R.id.buttonCancelLogout)
                    .setEnabled(false);

            dialog.setCancelable(false);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            if (user == null) {
                finishLogout(dialog, auth);
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(document -> {

                        // No profile yet: log out normally
                        if (!document.exists()) {
                            finishLogout(dialog, auth);
                            return;
                        }

                        // Profile exists: update Offline status
                        document.getReference()
                                .update(
                                        "isOnline", false,
                                        "lastSeen",
                                        FieldValue.serverTimestamp()
                                )
                                .addOnSuccessListener(unused -> {
                                    finishLogout(dialog, auth);
                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(
                                            AccountActivity.this,
                                            "Online status could not be updated.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    finishLogout(dialog, auth);
                                });

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(
                                AccountActivity.this,
                                "Could not check profile status.",
                                Toast.LENGTH_LONG
                        ).show();

                        finishLogout(dialog, auth);
                    });
        });

    }

    private void finishLogout(Dialog dialog, FirebaseAuth auth) {

        auth.signOut();

        dialog.dismiss();

        openWelcome();
    }


    private void openWelcome() {

        Intent intent = new Intent(
                AccountActivity.this,
                WelcomeActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

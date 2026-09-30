package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editDisplayName;
    private EditText editRegion;
    private EditText editAbout;
    private EditText editGame;
    private EditText editRiotId;
    private EditText editRank;
    private EditText editRole;
    private EditText editAvailability;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Connect XML fields
        editDisplayName = findViewById(R.id.editDisplayName);
        editRegion = findViewById(R.id.editRegion);
        editAbout = findViewById(R.id.editAbout);
        editGame = findViewById(R.id.editGame);
        editRiotId = findViewById(R.id.editRiotId);
        editRank = findViewById(R.id.editRank);
        editRole = findViewById(R.id.editRole);
        editAvailability = findViewById(R.id.editAvailability);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        loadProfile();

        buttonBack.setOnClickListener(v -> finish());

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        editDisplayName.setText(
                                documentSnapshot.getString("displayName")
                        );

                        editRegion.setText(
                                documentSnapshot.getString("region")
                        );

                        editAbout.setText(
                                documentSnapshot.getString("about")
                        );

                        editGame.setText(
                                documentSnapshot.getString("game")
                        );

                        editRiotId.setText(
                                documentSnapshot.getString("riotId")
                        );

                        editRank.setText(
                                documentSnapshot.getString("rank")
                        );

                        editRole.setText(
                                documentSnapshot.getString("role")
                        );

                        editAvailability.setText(
                                documentSnapshot.getString("availability")
                        );
                    }
                });
    }

    private void saveProfile() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    EditProfileActivity.this,
                    "You must be logged in to save a profile.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String displayName = editDisplayName.getText().toString().trim();
        String region = editRegion.getText().toString().trim();
        String about = editAbout.getText().toString().trim();
        String game = editGame.getText().toString().trim();
        String riotId = editRiotId.getText().toString().trim();
        String rank = editRank.getText().toString().trim();
        String role = editRole.getText().toString().trim();
        String availability = editAvailability.getText().toString().trim();

        if (displayName.isEmpty()) {
            editDisplayName.setError("Enter a display name");
            return;
        }



        // Firestore profile
        Map<String, Object> profile = new HashMap<>();

        profile.put("uid", currentUser.getUid());
        profile.put("email", currentUser.getEmail());
        profile.put("displayName", displayName);
        profile.put("region", region);
        profile.put("about", about);
        profile.put("game", game);
        profile.put("riotId", riotId);
        profile.put("rank", rank);
        profile.put("role", role);
        profile.put("availability", availability);

        db.collection("users")
                .document(currentUser.getUid())
                .set(profile)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            EditProfileActivity.this,
                            "Profile saved!",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            EditProfileActivity.this,
                            "Failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
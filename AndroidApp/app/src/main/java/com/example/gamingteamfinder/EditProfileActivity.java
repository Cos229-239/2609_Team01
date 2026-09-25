package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editDisplayName;
    private EditText editRegion;
    private EditText editAbout;
    private EditText editGame;
    private EditText editRiotId;
    private EditText editRank;
    private EditText editRole;
    private EditText editAvailability;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

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

        // Load current temporary profile data into the form
        editDisplayName.setText(ProfileData.displayName);
        editRegion.setText(ProfileData.region);
        editAbout.setText(ProfileData.about);
        editGame.setText(ProfileData.game);
        editRiotId.setText(ProfileData.riotId);
        editRank.setText(ProfileData.rank);
        editRole.setText(ProfileData.role);
        editAvailability.setText(ProfileData.availability);

        buttonBack.setOnClickListener(v -> finish());

        buttonSaveProfile.setOnClickListener(v -> {

            ProfileData.displayName =
                    editDisplayName.getText().toString();

            ProfileData.region =
                    editRegion.getText().toString();

            ProfileData.about =
                    editAbout.getText().toString();

            ProfileData.game =
                    editGame.getText().toString();

            ProfileData.riotId =
                    editRiotId.getText().toString();

            ProfileData.rank =
                    editRank.getText().toString();

            ProfileData.role =
                    editRole.getText().toString();

            ProfileData.availability =
                    editAvailability.getText().toString();

            finish();
        });
    }
}
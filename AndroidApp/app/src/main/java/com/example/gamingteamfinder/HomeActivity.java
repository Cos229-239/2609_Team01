package com.example.gamingteamfinder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        LinearLayout friendsContainer = findViewById(R.id.friendsContainer);

        addFriend(friendsContainer, "KP", "Kien", true);
        addFriend(friendsContainer, "GA", "Gabriel", true);
        addFriend(friendsContainer, "TF", "Tiffany", false);

        // Quick Actions
        Button buttonFindTeam = findViewById(R.id.buttonFindTeam);
        Button buttonFindDuo = findViewById(R.id.buttonFindDuo);
        Button buttonFindPlayers = findViewById(R.id.buttonFindPlayers);
        Button buttonTeam = findViewById(R.id.buttonTeam);

        // Bottom Navigation
        Button buttonHome = findViewById(R.id.buttonHome);
        Button buttonSearch = findViewById(R.id.buttonSearch);
        Button buttonMessages = findViewById(R.id.buttonMessages);
        Button buttonProfile = findViewById(R.id.buttonProfile);


        // Find Team
        buttonFindTeam.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LfgActivity.class);
            startActivity(intent);
        });


        // Find Duo - temporary
        buttonFindDuo.setOnClickListener(v -> {
            Toast.makeText(
                    HomeActivity.this,
                    "Find Duo coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });


        // Browse Players
        buttonFindPlayers.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchPlayerActivity.class);
            startActivity(intent);
        });


        // Create Team
        buttonTeam.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TeamActivity.class);
            startActivity(intent);
        });


        // Home
        buttonHome.setOnClickListener(v -> {
            // Already on Home screen
        });


        // Search
        buttonSearch.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchPlayerActivity.class);
            startActivity(intent);
        });


        // Messages - temporary
        buttonMessages.setOnClickListener(v -> {
            Toast.makeText(
                    HomeActivity.this,
                    "Messages coming soon",
                    Toast.LENGTH_SHORT
            ).show();
        });


        // Profile
        buttonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void addFriend(
            LinearLayout container,
            String initials,
            String name,
            boolean isOnline
    ) {

        int size = (int) (72 * getResources().getDisplayMetrics().density);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout friendItem = new LinearLayout(this);
        friendItem.setOrientation(LinearLayout.VERTICAL);
        friendItem.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams itemParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        itemParams.setMargins(0, 0, margin, 0);
        friendItem.setLayoutParams(itemParams);

        TextView avatar = new TextView(this);

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(size, size);

        avatar.setLayoutParams(avatarParams);
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(initials);
        avatar.setTextSize(20);
        avatar.setTextColor(Color.parseColor("#30364F"));
        avatar.setBackgroundResource(R.drawable.friend_avatar_background);

        TextView friendName = new TextView(this);
        friendName.setText(name);
        friendName.setTextSize(14);
        friendName.setGravity(Gravity.CENTER);
        friendName.setPadding(0, 8, 0, 0);

        TextView onlineStatus = new TextView(this);

        if (isOnline) {
            onlineStatus.setText("● Online");
            onlineStatus.setTextColor(Color.parseColor("#3FAE64"));
        } else {
            onlineStatus.setText("● Offline");
            onlineStatus.setTextColor(Color.parseColor("#9E9E9E"));
        }

        onlineStatus.setTextSize(11);
        onlineStatus.setGravity(Gravity.CENTER);
        friendItem.addView(avatar);
        friendItem.addView(friendName);
        friendItem.addView(onlineStatus);

        container.addView(friendItem);
    }
}
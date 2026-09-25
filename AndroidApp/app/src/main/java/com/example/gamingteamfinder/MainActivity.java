package com.example.gamingteamfinder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private EditText editRiotId;
    private TextView textResult;
    private ImageView profileIcon;
    private LinearLayout playerResultLayout;
    private TextView textPlayerFound;
    private Button buttonSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editRiotId = findViewById(R.id.editRiotId);
        buttonSearch = findViewById(R.id.buttonSearch);
        Button buttonBack = findViewById(R.id.buttonBack);
        textResult = findViewById(R.id.textResult);
        profileIcon = findViewById(R.id.profileIcon);
        playerResultLayout = findViewById(R.id.playerResultLayout);
        textPlayerFound = findViewById(R.id.textPlayerFound);

        buttonSearch.setOnClickListener(v -> searchPlayer());
        buttonBack.setOnClickListener(v -> finish());

        editRiotId.setOnEditorActionListener((v, actionId, event) -> {
            searchPlayer();
            return true;
        });
    }

    private void searchPlayer() {

        String riotId = editRiotId.getText().toString().trim();

        if (riotId.isEmpty() || !riotId.contains("#")) {
            textPlayerFound.setText("Search Failed");
            textPlayerFound.setVisibility(View.VISIBLE);

            textResult.setText("Please enter a valid Riot ID, for example: abc#123.");
            playerResultLayout.setVisibility(View.VISIBLE);
            profileIcon.setVisibility(View.GONE);

            return;
        }

        String[] parts = riotId.split("#", 2);

        String gameName = parts[0].trim();
        String tagLine = parts[1].trim();

        if (gameName.isEmpty() || tagLine.isEmpty()) {
            textPlayerFound.setText("Search Failed");
            textPlayerFound.setVisibility(View.VISIBLE);

            textResult.setText("Please enter a valid Riot ID, for example: abc#123.");
            playerResultLayout.setVisibility(View.VISIBLE);
            profileIcon.setVisibility(View.GONE);

            return;
        }

        buttonSearch.setEnabled(false);
        buttonSearch.setText("Searching...");

        textPlayerFound.setText("Searching...");
        textPlayerFound.setVisibility(View.VISIBLE);

        textResult.setText("");
        profileIcon.setImageDrawable(null);

        playerResultLayout.setVisibility(View.GONE);

        new Thread(() -> {

            HttpURLConnection accountConnection = null;
            HttpURLConnection summonerConnection = null;

            try {
                // Encode Riot ID values for URL
                String encodedGameName = URLEncoder.encode(
                        gameName,
                        StandardCharsets.UTF_8.toString()
                );

                String encodedTagLine = URLEncoder.encode(
                        tagLine,
                        StandardCharsets.UTF_8.toString()
                );

                // -----------------------------
                // 1. ACCOUNT-V1
                // Riot ID -> PUUID
                // -----------------------------
                String accountUrlString =
                        "https://americas.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                                + encodedGameName + "/" + encodedTagLine;

                URL accountUrl = new URL(accountUrlString);

                accountConnection =
                        (HttpURLConnection) accountUrl.openConnection();

                accountConnection.setRequestMethod("GET");
                accountConnection.setRequestProperty(
                        "X-Riot-Token",
                        BuildConfig.RIOT_API_KEY
                );

                int accountResponseCode =
                        accountConnection.getResponseCode();

                if (accountResponseCode != 200) {
                    int finalCode = accountResponseCode;

                    runOnUiThread(() -> {
                        textPlayerFound.setText("Search Failed");
                        textPlayerFound.setVisibility(View.VISIBLE);

                        textResult.setText(
                                getFriendlyErrorMessage(finalCode)
                        );

                        playerResultLayout.setVisibility(View.VISIBLE);
                        profileIcon.setVisibility(View.GONE);

                        buttonSearch.setEnabled(true);
                        buttonSearch.setText("Search Player");
                    });

                    return;
                }

                BufferedReader accountReader =
                        new BufferedReader(
                                new InputStreamReader(
                                        accountConnection.getInputStream()
                                )
                        );

                StringBuilder accountResponse =
                        new StringBuilder();

                String line;

                while ((line = accountReader.readLine()) != null) {
                    accountResponse.append(line);
                }

                accountReader.close();

                JSONObject accountJson =
                        new JSONObject(accountResponse.toString());

                String puuid =
                        accountJson.getString("puuid");

                String returnedGameName =
                        accountJson.getString("gameName");

                String returnedTagLine =
                        accountJson.getString("tagLine");

                if (!gameName.equals(returnedGameName)
                        || !tagLine.equals(returnedTagLine)) {

                    runOnUiThread(() -> {
                        textPlayerFound.setText("Search Failed");
                        textPlayerFound.setVisibility(View.VISIBLE);

                        textResult.setText(
                                "Riot ID capitalization does not match. Please enter the exact Riot ID."
                        );

                        playerResultLayout.setVisibility(View.VISIBLE);
                        profileIcon.setVisibility(View.GONE);

                        buttonSearch.setEnabled(true);
                        buttonSearch.setText("Search Player");
                    });

                    return;
                }


                // -----------------------------
                // 2. SUMMONER-V4
                // PUUID -> Summoner Level
                // -----------------------------
                String summonerUrlString =
                        "https://na1.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/"
                                + puuid;

                URL summonerUrl = new URL(summonerUrlString);

                summonerConnection =
                        (HttpURLConnection) summonerUrl.openConnection();

                summonerConnection.setRequestMethod("GET");
                summonerConnection.setRequestProperty(
                        "X-Riot-Token",
                        BuildConfig.RIOT_API_KEY
                );

                int summonerResponseCode =
                        summonerConnection.getResponseCode();

                if (summonerResponseCode != 200) {
                    int finalCode = summonerResponseCode;

                    runOnUiThread(() -> {
                        textPlayerFound.setText("Search Failed");
                        textPlayerFound.setVisibility(View.VISIBLE);

                        textResult.setText(
                                getFriendlyErrorMessage(finalCode)
                        );

                        playerResultLayout.setVisibility(View.VISIBLE);
                        profileIcon.setVisibility(View.GONE);

                        buttonSearch.setEnabled(true);
                        buttonSearch.setText("Search Player");
                    });

                    return;
                }

                BufferedReader summonerReader =
                        new BufferedReader(
                                new InputStreamReader(
                                        summonerConnection.getInputStream()
                                )
                        );

                StringBuilder summonerResponse =
                        new StringBuilder();

                while ((line = summonerReader.readLine()) != null) {
                    summonerResponse.append(line);
                }

                summonerReader.close();

                JSONObject summonerJson =
                        new JSONObject(summonerResponse.toString());

                long summonerLevel =
                        summonerJson.getLong("summonerLevel");

                int profileIconId =
                        summonerJson.getInt("profileIconId");

                URL versionsUrl = new URL(
                        "https://ddragon.leagueoflegends.com/api/versions.json"
                );

                BufferedReader versionsReader =
                        new BufferedReader(
                                new InputStreamReader(
                                        versionsUrl.openStream()
                                )
                        );

                StringBuilder versionsResponse = new StringBuilder();
                String versionLine;

                while ((versionLine = versionsReader.readLine()) != null) {
                    versionsResponse.append(versionLine);
                }

                versionsReader.close();

                org.json.JSONArray versionsArray =
                        new org.json.JSONArray(versionsResponse.toString());

                String latestVersion = versionsArray.getString(0);

                String iconUrlString =
                        "https://ddragon.leagueoflegends.com/cdn/"
                                + latestVersion
                                + "/img/profileicon/"
                                + profileIconId
                                + ".png";

                URL iconUrl = new URL(iconUrlString);

                Bitmap profileBitmap =
                        BitmapFactory.decodeStream(iconUrl.openStream());


                // -----------------------------
                // 3. LEAGUE-V4
                // PUUID -> Rank + LP
                // -----------------------------
                String leagueUrlString =
                        "https://na1.api.riotgames.com/lol/league/v4/entries/by-puuid/"
                                + puuid;

                URL leagueUrl = new URL(leagueUrlString);

                HttpURLConnection leagueConnection =
                        (HttpURLConnection) leagueUrl.openConnection();

                leagueConnection.setRequestMethod("GET");
                leagueConnection.setRequestProperty(
                        "X-Riot-Token",
                        BuildConfig.RIOT_API_KEY
                );

                int leagueResponseCode =
                        leagueConnection.getResponseCode();

                String rankText = "Unranked";
                String lpText = "";

                if (leagueResponseCode == 200) {

                    BufferedReader leagueReader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            leagueConnection.getInputStream()
                                    )
                            );

                    StringBuilder leagueResponse =
                            new StringBuilder();

                    while ((line = leagueReader.readLine()) != null) {
                        leagueResponse.append(line);
                    }

                    leagueReader.close();

                    org.json.JSONArray leagueArray =
                            new org.json.JSONArray(leagueResponse.toString());

                    for (int i = 0; i < leagueArray.length(); i++) {

                        JSONObject entry = leagueArray.getJSONObject(i);

                        String queueType = entry.getString("queueType");

                        if (queueType.equals("RANKED_SOLO_5x5")) {

                            String tier = entry.getString("tier");
                            String rank = entry.getString("rank");
                            int leaguePoints = entry.getInt("leaguePoints");

                            rankText = tier + " " + rank;

                            if (tier.equals("MASTER")
                                    || tier.equals("GRANDMASTER")
                                    || tier.equals("CHALLENGER")) {

                                lpText = leaguePoints + " LP";

                            } else {

                                lpText = leaguePoints + " / 100 LP";
                            }

                            break;
                        }
                    }
                }

                leagueConnection.disconnect();

                // -----------------------------
                // Display result
                // -----------------------------
                String result =
                        "Riot ID: "
                                + returnedGameName
                                + "#"
                                + returnedTagLine
                                + "\n\n"
                                + "Level: "
                                + summonerLevel
                                + "\n"
                                + "Rank: "
                                + rankText
                                + (lpText.isEmpty() ? "" : "\nLP: " + lpText);

                runOnUiThread(() -> {
                    textPlayerFound.setText("Player Found!");
                    textPlayerFound.setVisibility(View.VISIBLE);

                    textResult.setText(result);
                    profileIcon.setImageBitmap(profileBitmap);
                    playerResultLayout.setVisibility(View.VISIBLE);
                    profileIcon.setVisibility(View.VISIBLE);

                    buttonSearch.setEnabled(true);
                    buttonSearch.setText("Search Player");
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    textPlayerFound.setText("Search Failed");
                    textPlayerFound.setVisibility(View.VISIBLE);

                    textResult.setText(
                            "Something went wrong. Please try again."
                    );

                    playerResultLayout.setVisibility(View.VISIBLE);
                    profileIcon.setVisibility(View.GONE);

                    buttonSearch.setEnabled(true);
                    buttonSearch.setText("Search Player");
                });

            } finally {

                if (accountConnection != null) {
                    accountConnection.disconnect();
                }

                if (summonerConnection != null) {
                    summonerConnection.disconnect();
                }
            }
        }).start();
    }
    private String getFriendlyErrorMessage(int responseCode) {
        switch (responseCode) {
            case 400:
                return "Invalid request. Please check the Riot ID.";

            case 401:
                return "API key is missing or expired.";

            case 403:
                return "API access was denied. Please check the API key.";

            case 404:
                return "Player not found. Please check the Riot ID.";

            case 429:
                return "Too many requests. Please wait a moment and try again.";

            case 500:
            case 503:
                return "Riot servers are temporarily unavailable. Please try again later.";

            default:
                return "Something went wrong. Error code: " + responseCode;
        }
    }
}
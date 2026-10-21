
package com.example.gamingteamfinder;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class OtpRequestHistory {

    private static final String PREFS_NAME =
            "gtf_otp_request_history";

    private static final int MAX_REQUESTS = 10;

    private static final long WINDOW_MS =
            24L * 60 * 60 * 1000;

    private OtpRequestHistory() {
    }

    // Create a separate storage key for each email
    private static String getEmailKey(String email) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            normalizedEmail.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result = new StringBuilder();

            for (byte value : hash) {
                result.append(
                        Character.forDigit(
                                (value >> 4) & 0xF, 16
                        )
                );

                result.append(
                        Character.forDigit(
                                value & 0xF, 16
                        )
                );
            }

            return "otp_" + result;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to initialize OTP history"
            );
        }
    }

    // Load requests made during the last 24 hours
    private static JSONArray loadRecentRequests(
            SharedPreferences prefs,
            String key
    ) {

        long now = System.currentTimeMillis();

        JSONArray recent = new JSONArray();

        try {
            JSONArray saved = new JSONArray(
                    prefs.getString(key, "[]")
            );

            for (int i = 0; i < saved.length(); i++) {

                long timestamp = saved.optLong(i, 0L);

                if (timestamp > now - WINDOW_MS
                        && timestamp <= now) {

                    recent.put(timestamp);
                }
            }

        } catch (Exception ignored) {
            // Invalid local history starts empty
        }

        return recent;
    }

    // Save an accepted OTP request
    public static synchronized int recordAcceptedRequest(
            Context context,
            String email
    ) {

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );

        String key = getEmailKey(email);

        JSONArray requests = loadRecentRequests(
                prefs,
                key
        );

        requests.put(System.currentTimeMillis());

        prefs.edit()
                .putString(key, requests.toString())
                .apply();

        return Math.max(
                0,
                MAX_REQUESTS - requests.length()
        );
    }

    // Read remaining requests estimated on this device
    public static synchronized int getRemaining(
            Context context,
            String email
    ) {

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );

        String key = getEmailKey(email);

        JSONArray requests = loadRecentRequests(
                prefs,
                key
        );

        // Remove requests older than 24 hours
        prefs.edit()
                .putString(key, requests.toString())
                .apply();

        return Math.max(
                0,
                MAX_REQUESTS - requests.length()
        );
    }


    public static synchronized long getTimeUntilNextRequestMs(
            Context context,
            String email
    ) {
        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );

        JSONArray requests = loadRecentRequests(
                prefs,
                getEmailKey(email)
        );

        if (requests.length() < MAX_REQUESTS) {
            return 0L;
        }

        long oldestRequest = Long.MAX_VALUE;

        for (int i = 0; i < requests.length(); i++) {
            long timestamp = requests.optLong(i, 0L);

            if (timestamp > 0L && timestamp < oldestRequest) {
                oldestRequest = timestamp;
            }
        }

        if (oldestRequest == Long.MAX_VALUE) {
            return 0L;
        }

        return Math.max(
                0L,
                oldestRequest + WINDOW_MS - System.currentTimeMillis()
        );
    }



}

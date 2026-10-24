
package com.example.gamingteamfinder;

import android.content.Context;
import android.content.SharedPreferences;

public final class OtpRateLimitStore {

    private static final String PREFS_NAME =
            "gtf_otp_rate_limit";

    private static final String KEY_BLOCKED_UNTIL =
            "otp_send_blocked_until_ms";

    private static final long DEFAULT_RETRY_SECONDS = 60L;
    private static final long MAX_RETRY_SECONDS = 86400L;

    private OtpRateLimitStore() {
    }

    // Save the time when sending OTP can be retried
    public static void saveRateLimit(
            Context context,
            long retryAfterSeconds
    ) {

        long seconds = retryAfterSeconds > 0
                ? Math.min(retryAfterSeconds, MAX_RETRY_SECONDS)
                : DEFAULT_RETRY_SECONDS;

        long blockedUntil =
                System.currentTimeMillis() + seconds * 1000L;

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );

        long previousDeadline =
                prefs.getLong(KEY_BLOCKED_UNTIL, 0L);

        prefs.edit()
                .putLong(
                        KEY_BLOCKED_UNTIL,
                        Math.max(previousDeadline, blockedUntil)
                )
                .apply();
    }

    // Check whether the backend rate limit is still active
    public static long getRemainingMs(Context context) {

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );

        long blockedUntil =
                prefs.getLong(KEY_BLOCKED_UNTIL, 0L);

        long remaining =
                blockedUntil - System.currentTimeMillis();

        if (remaining <= 0L) {
            prefs.edit()
                    .remove(KEY_BLOCKED_UNTIL)
                    .apply();

            return 0L;
        }

        return remaining;
    }
}

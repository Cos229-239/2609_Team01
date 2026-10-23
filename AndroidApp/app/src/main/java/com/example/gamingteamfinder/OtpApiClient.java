
package com.example.gamingteamfinder;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.appcheck.FirebaseAppCheck;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class OtpApiClient {

    private static final String REQUEST_OTP_URL =
            "https://gtf-otp-backend.vercel.app/api/request-otp";

    private static final String VERIFY_OTP_URL =
            "https://gtf-otp-backend.vercel.app/api/verify-otp";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(
                boolean accepted,
                String message,
                int statusCode,
                long retryAfterSeconds
        );
    }
    private OtpApiClient() {
    }

    static int[] readVerifiedOtpQuota(JSONObject result) {

        // Only accept quota from successful verification
        if (!result.optBoolean("verified", false)) {
            return new int[]{-1, -1};
        }

        int remaining = result.optInt("remaining", -1);
        int maxRequests = result.optInt("maxRequests", -1);

        // Validate the values received from the backend
        if (maxRequests != 10
                || remaining < 0
                || remaining > maxRequests) {

            return new int[]{-1, -1};
        }

        return new int[]{remaining, maxRequests};
    }


    public static void requestOtp(
            String email,
            Callback callback
    ) {

        // Get Firebase App Check token
        FirebaseAppCheck.getInstance()
                .getAppCheckToken(false)
                .addOnSuccessListener(appCheckToken -> {

                    String token = appCheckToken.getToken();

                    // Network request must run off the UI thread
                    executor.execute(() -> {

                        HttpURLConnection connection = null;

                        boolean accepted = false;
                        String message;
                        int statusCode = 0;
                        long retryAfterSeconds = 0L;

                        try {

                            URL url = new URL(REQUEST_OTP_URL);

                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();

                            connection.setRequestMethod("POST");

                            connection.setRequestProperty(
                                    "Content-Type",
                                    "application/json"
                            );

                            connection.setRequestProperty(
                                    "X-Firebase-AppCheck",
                                    token
                            );

                            connection.setConnectTimeout(10000);
                            connection.setReadTimeout(15000);
                            connection.setDoOutput(true);

                            // Create request JSON
                            JSONObject body = new JSONObject();
                            body.put("email", email);

                            byte[] requestBody = body.toString()
                                    .getBytes(StandardCharsets.UTF_8);

                            try (OutputStream output =
                                         connection.getOutputStream()) {

                                output.write(requestBody);
                            }

                            statusCode = connection.getResponseCode();


                            if (statusCode == 202) {

                                accepted = true;

                                message =
                                        "If an account with this email exists, " +
                                                "a verification code will be sent " +
                                                "if permitted.";

                            } else if (statusCode == 401) {

                                message =
                                        "App verification failed. " +
                                                "Please try again.";

                            } else if (statusCode == 429) {

                                // Read retry time from Vercel
                                String retryHeader =
                                        connection.getHeaderField("Retry-After");

                                if (retryHeader != null) {
                                    try {
                                        retryAfterSeconds = Math.max(
                                                0L,
                                                Math.min(
                                                        86400L,
                                                        Long.parseLong(retryHeader.trim())
                                                )
                                        );
                                    } catch (NumberFormatException ignored) {
                                        retryAfterSeconds = 0L;
                                    }
                                }

                                message =
                                        "Too many requests. " +
                                                "Please try again later.";

                            } else {

                                message =
                                        "OTP service unavailable. " +
                                                "Please try again later.";
                            }


                        } catch (Exception e) {

                            message =
                                    "Unable to connect to the server. " +
                                            "Please try again.";

                        } finally {

                            if (connection != null) {
                                connection.disconnect();
                            }
                        }

                        boolean finalAccepted = accepted;
                        String finalMessage = message;
                        int finalStatusCode = statusCode;
                        long finalRetryAfterSeconds = retryAfterSeconds;

                        // Return result to Android UI thread

                        mainHandler.post(() ->
                                callback.onResult(
                                        finalAccepted,
                                        finalMessage,
                                        finalStatusCode,
                                        finalRetryAfterSeconds
                                )
                        );

                    });

                })
                .addOnFailureListener(e -> {

                    mainHandler.post(() ->
                            callback.onResult(
                                    false,
                                    "Unable to verify the app. Please try again.",
                                    0,
                                    0L
                            )
                    );
                });
    }


    // Callback for OTP verification
    public interface VerifyOtpCallback {
        void onResult(
                boolean verified,
                String resetToken,
                int expiresInSeconds,
                String message,
                int remaining,
                int maxRequests
        );
    }

    public static void verifyOtp(
            String email,
            String code,
            VerifyOtpCallback callback
    ) {

        // Obtain Firebase App Check token
        FirebaseAppCheck.getInstance()
                .getAppCheckToken(false)
                .addOnSuccessListener(appCheckResult -> {

                    String appCheckToken =
                            appCheckResult.getToken();

                    executor.execute(() -> {

                        HttpURLConnection connection = null;

                        boolean verified = false;
                        String resetToken = null;
                        int expiresInSeconds = 0;
                        int remaining = -1;
                        int maxRequests = -1;

                        String message =
                                "Unable to verify OTP.";

                        try {
                            URL url = new URL(VERIFY_OTP_URL);

                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();

                            connection.setRequestMethod("POST");

                            connection.setRequestProperty(
                                    "Content-Type",
                                    "application/json"
                            );

                            connection.setRequestProperty(
                                    "X-Firebase-AppCheck",
                                    appCheckToken
                            );

                            connection.setConnectTimeout(10000);
                            connection.setReadTimeout(15000);
                            connection.setDoOutput(true);

                            JSONObject body = new JSONObject();
                            body.put("email", email);
                            body.put("code", code);

                            byte[] requestBody = body.toString()
                                    .getBytes(StandardCharsets.UTF_8);

                            try (OutputStream output =
                                         connection.getOutputStream()) {
                                output.write(requestBody);
                            }

                            int responseCode =
                                    connection.getResponseCode();

                            if (responseCode == 200) {

                                StringBuilder response =
                                        new StringBuilder();

                                try (BufferedReader reader =
                                             new BufferedReader(
                                                     new InputStreamReader(
                                                             connection.getInputStream(),
                                                             StandardCharsets.UTF_8
                                                     )
                                             )) {

                                    String line;

                                    while ((line = reader.readLine()) != null) {
                                        response.append(line);

                                        if (response.length() > 4096) {
                                            throw new IllegalStateException(
                                                    "Response too large"
                                            );
                                        }
                                    }
                                }

                                JSONObject result =
                                        new JSONObject(response.toString());

                                String receivedToken =
                                        result.optString("resetToken", "");

                                int receivedExpiry =
                                        result.optInt("expiresInSeconds", 0);

                                int[] receivedQuota = readVerifiedOtpQuota(result);

                                if (result.optBoolean("verified", false)
                                        && receivedToken.matches("(?i)[a-f0-9]{64}")
                                        && receivedExpiry > 0
                                        && receivedExpiry <= 300) {

                                    verified = true;
                                    resetToken = receivedToken;
                                    expiresInSeconds = receivedExpiry;

                                    remaining = receivedQuota[0];
                                    maxRequests = receivedQuota[1];

                                    message =
                                            "OTP verified successfully!";

                                } else {
                                    message =
                                            "Invalid server response.";
                                }

                            } else if (responseCode == 400) {

                                message =
                                        "Invalid or expired verification code.";

                            } else if (responseCode == 429) {

                                message =
                                        "Too many attempts. Please try again later.";

                            } else if (responseCode == 401) {

                                message =
                                        "App verification failed. Please try again.";

                            } else {

                                message =
                                        "Verification service unavailable.";
                            }

                        } catch (Exception e) {

                            message =
                                    "Unable to connect to verification service.";

                        } finally {

                            if (connection != null) {
                                connection.disconnect();
                            }
                        }

                        boolean finalVerified = verified;
                        String finalResetToken = resetToken;
                        int finalExpiry = expiresInSeconds;
                        String finalMessage = message;

                        int finalRemaining = remaining;
                        int finalMaxRequests = maxRequests;

                        mainHandler.post(() ->
                                callback.onResult(
                                        finalVerified,
                                        finalResetToken,
                                        finalExpiry,
                                        finalMessage,
                                        finalRemaining,
                                        finalMaxRequests
                                )
                        );
                    });

                })
                .addOnFailureListener(e -> {

                    mainHandler.post(() ->
                            callback.onResult(
                                    false,
                                    null,
                                    0,
                                    "Unable to verify the app.",
                                    -1,
                                    -1
                            )
                    );
                });
    }


    // Callback for password reset completion
    public interface CompleteResetCallback {
        void onResult(boolean success, String message);
    }

    public static void completePasswordReset(
            String email,
            String resetToken,
            String newPassword,
            CompleteResetCallback callback
    ) {

        FirebaseAppCheck.getInstance()
                .getAppCheckToken(false)
                .addOnSuccessListener(appCheckResult -> {

                    String appCheckToken =
                            appCheckResult.getToken();

                    executor.execute(() -> {

                        HttpURLConnection connection = null;

                        boolean success = false;
                        String message =
                                "Unable to reset password.";

                        try {

                            URL url = new URL(
                                    "https://gtf-otp-backend.vercel.app"
                                            + "/api/complete-password-reset"
                            );

                            connection =
                                    (HttpURLConnection) url.openConnection();

                            connection.setRequestMethod("POST");

                            connection.setRequestProperty(
                                    "Content-Type",
                                    "application/json"
                            );

                            connection.setRequestProperty(
                                    "X-Firebase-AppCheck",
                                    appCheckToken
                            );

                            connection.setConnectTimeout(10000);
                            connection.setReadTimeout(15000);
                            connection.setDoOutput(true);

                            JSONObject body = new JSONObject();

                            body.put("email", email);
                            body.put("resetToken", resetToken);
                            body.put("newPassword", newPassword);

                            byte[] requestBody = body.toString()
                                    .getBytes(StandardCharsets.UTF_8);

                            try (OutputStream output =
                                         connection.getOutputStream()) {

                                output.write(requestBody);
                            }

                            int responseCode =
                                    connection.getResponseCode();

                            if (responseCode == 200) {

                                StringBuilder response =
                                        new StringBuilder();

                                try (BufferedReader reader =
                                             new BufferedReader(
                                                     new InputStreamReader(
                                                             connection.getInputStream(),
                                                             StandardCharsets.UTF_8
                                                     )
                                             )) {

                                    String line;

                                    while ((line = reader.readLine()) != null) {

                                        response.append(line);

                                        if (response.length() > 4096) {
                                            throw new IllegalStateException(
                                                    "Response too large"
                                            );
                                        }
                                    }
                                }

                                JSONObject result =
                                        new JSONObject(response.toString());

                                success = result.optBoolean(
                                        "success",
                                        false
                                );

                                message = success
                                        ? "Password changed successfully!"
                                        : "Password reset failed.";

                            } else if (responseCode == 400) {

                                message =
                                        "Invalid or expired reset session. "
                                                + "Please request a new OTP.";

                            } else if (responseCode == 401) {

                                message =
                                        "App verification failed. "
                                                + "Please try again.";

                            } else if (responseCode == 429) {

                                message =
                                        "Too many requests. "
                                                + "Please try again later.";

                            } else {

                                message =
                                        "Password reset service unavailable.";
                            }

                        } catch (Exception e) {

                            message =
                                    "Unable to confirm password reset. "
                                            + "Please check your connection.";

                        } finally {

                            if (connection != null) {
                                connection.disconnect();
                            }
                        }

                        boolean finalSuccess = success;
                        String finalMessage = message;

                        mainHandler.post(() ->
                                callback.onResult(
                                        finalSuccess,
                                        finalMessage
                                )
                        );
                    });

                })
                .addOnFailureListener(e -> {

                    mainHandler.post(() ->
                            callback.onResult(
                                    false,
                                    "Unable to verify the app."
                            )
                    );
                });
    }


}

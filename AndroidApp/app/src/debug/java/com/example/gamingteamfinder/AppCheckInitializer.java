
package com.example.gamingteamfinder;

import android.util.Log;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

final class AppCheckInitializer {

    static void install() {

        FirebaseAppCheck appCheck =
                FirebaseAppCheck.getInstance();

        // Install Debug Provider
        appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );

        // Test if Firebase App Check works
        appCheck.getAppCheckToken(true)
                .addOnSuccessListener(result -> {
                    Log.d(
                            "GTF_APP_CHECK",
                            "App Check token received successfully!"
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(
                            "GTF_APP_CHECK",
                            "Failed to get App Check token",
                            e
                    );
                });
    }
}

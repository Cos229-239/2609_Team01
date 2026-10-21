
package com.example.gamingteamfinder;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

final class AppCheckInitializer {

    static void install() {
        FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                );
    }
}

package com.example.gamingteamfinder;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GamingTeamFinderApp extends Application
        implements Application.ActivityLifecycleCallbacks {

    private int startedActivities = 0;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public void onCreate() {
        super.onCreate();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerActivityLifecycleCallbacks(this);
    }

    private void setOnlineStatus(boolean isOnline) {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();

        Map<String, Object> updates = new HashMap<>();

        updates.put("isOnline", isOnline);
        updates.put("lastSeen", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .update(updates);
    }

    @Override
    public void onActivityStarted(Activity activity) {

        startedActivities++;

        if (startedActivities == 1) {
            setOnlineStatus(true);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

        startedActivities--;

        if (startedActivities < 0) {
            startedActivities = 0;
        }

        if (startedActivities == 0) {
            setOnlineStatus(false);
        }
    }

    @Override
    public void onActivityCreated(
            Activity activity,
            Bundle savedInstanceState
    ) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(
            Activity activity,
            Bundle outState
    ) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
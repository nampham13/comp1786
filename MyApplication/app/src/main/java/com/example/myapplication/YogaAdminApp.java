package com.example.myapplication;

import android.app.Application;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import com.example.myapplication.firebase.UserAutoSyncWorker;

import com.example.myapplication.util.NotificationHelper;

public class YogaAdminApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);
        // Schedule periodic user sync with WorkManager
        PeriodicWorkRequest userSyncRequest = new PeriodicWorkRequest.Builder(
                UserAutoSyncWorker.class,
                1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "UserSyncWork",
                        ExistingPeriodicWorkPolicy.KEEP,
                        userSyncRequest
                );
    }
}
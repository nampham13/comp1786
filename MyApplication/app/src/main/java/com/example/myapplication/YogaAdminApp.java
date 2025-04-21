package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.util.NotificationHelper;

public class YogaAdminApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);
    }
}
package com.example.myapplication.firebase;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.myapplication.firebase.FirebaseService;

public class UserAutoSyncWorker extends Worker {
    public UserAutoSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    @NonNull
    @Override
    public Result doWork() {
        // Trigger user sync to Firebase
        FirebaseService firebaseService = FirebaseService.getInstance();
        try {
            firebaseService.syncUsersToFirebase(); // Make sure this method takes no arguments
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}


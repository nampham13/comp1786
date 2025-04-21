package com.example.myapplication.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Course;
import com.example.myapplication.model.Instance;
import com.example.myapplication.ui.MainActivity;

public class NotificationHelper {
    private static final String CHANNEL_ID = "yoga_admin_channel";
    private static final String CHANNEL_NAME = "Yoga Admin Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for Yoga Admin App";
    
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public static void sendNewInstanceNotification(Context context, Course course, Instance instance) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("course_id", course.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
        );
        
        String title = "New Yoga Class Added";
        String message = String.format("A new %s class has been scheduled on %s at %s", 
                course.getName(), instance.getDate(), instance.getTime());
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        

        try {
            notificationManager.notify((int) instance.getId(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
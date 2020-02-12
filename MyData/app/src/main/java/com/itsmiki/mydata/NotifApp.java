package com.itsmiki.mydata;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class NotifApp extends Application {
    public static final String CHANNEL_ID = "gpsNotifChannel";
    public static final String CHANNEL_ID2 = "surveyChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotifChannel();
    }

    private void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChan = new NotificationChannel(
                    CHANNEL_ID,
                    "Test Service Name",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChan);

            NotificationChannel serviceChan2 = new NotificationChannel(
                    CHANNEL_ID2,
                    "Survey Service Name",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(serviceChan2);
        }
    }
}

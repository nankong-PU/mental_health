package com.itsmiki.mydata;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.TaskStackBuilder;
import static com.itsmiki.mydata.NotifApp.CHANNEL_ID2;

public class SurveyPublisher extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Notification notification = null;
        Intent intent2 = new Intent(context, SurveyActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(intent2);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(context.getApplicationContext(), CHANNEL_ID2)
                    .setSmallIcon(R.drawable.ic_android24dp)
                    .setContentTitle("Survey Reminder")
                    .setContentText("Please do the mental health survey.")
                    .setOnlyAlertOnce(true)
                    .setContentIntent(resultPendingIntent)
                    .build();
        }
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null) manager.notify(69, notification);
    }
}

package com.itsmiki.mydata;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;


public class ExampleJob extends JobService {
    private static final String TAG = "ExampleJob";
    private boolean jobCancled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Job started");
        //doBackgroundWork(params);
        startService();
        return false;
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, GpsService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    public void stopService() {
        Log.d(TAG, "gps stopping");
        Intent serviceIntent = new Intent(this, GpsService.class);
        stopService(serviceIntent);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancled");
        stopService();
        return true;
    }
}

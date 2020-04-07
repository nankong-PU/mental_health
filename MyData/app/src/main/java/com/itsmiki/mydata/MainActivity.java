package com.itsmiki.mydata;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Main activity";
    public static final String ID_CODE = "01";
    public static final String SERVER_HOST = "upload.dash.catalyzecare.org";
    public static final String SERVER_USERNAME = "mhas";
    public static final String DESTINATION_DIRECTORY = ".";
    private static final long UPDATE_INTERVAL = 1000 * 60 * 15;
    private static final long FASTEST_INTERVAL = 1000 * 60 * 10;
    private static final long MAX_WAIT_TIME = 1000 * 60 * 20;
    private FusedLocationProviderClient mFusedLocClient;
    private LocationRequest mLocReq;
    private final int STORAGE_PERM_CODE = 701;
    private final int GPS_PERM_CODE = 56709;
    private final int ALL_REQ_CODE = 311;
    private final int USAGE_REQ_CODE = 2522;
    private PendingIntent surveyAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //basic setup for GPS
        mFusedLocClient = LocationServices.getFusedLocationProviderClient(this);
        mLocReq = new LocationRequest();
        mLocReq.setInterval(UPDATE_INTERVAL);
        mLocReq.setFastestInterval(FASTEST_INTERVAL);
        mLocReq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocReq.setMaxWaitTime(MAX_WAIT_TIME);
    }

    public void scheduleJob(View view) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && usagePerm()) {
            //Location Permission already granted
//            ComponentName componentName = new ComponentName(this, ExampleJob.class);
//            JobInfo info = new JobInfo.Builder(123, componentName)
//                    .setPersisted(true)
//                    .setPeriodic(MINS * 60 * 1000)
//                    .build();
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
//            int result = scheduler.schedule(info);
//            if (result == JobScheduler.RESULT_SUCCESS) {
//                Log.d(TAG, "Job scheduled gps");
//            } else {
//                Log.d(TAG, "Job scheduler gps failed");
//            }

            ComponentName dailyJ = new ComponentName(this, DailyJob.class);
            JobInfo secondjob = new JobInfo.Builder(404, dailyJ)
                    .setPersisted(true)
                    .setPeriodic(24 * 60 * 60 * 1000)
                    .build();
            int result = scheduler.schedule(secondjob);
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Job scheduled daily");
            } else {
                Log.d(TAG, "Job scheduler daily failed");
            }
            scheduleNotification();
            //scheduleGPS();
        } else {
            requestGps();
            requestStorage();
            requestUsage();
        }
    }

    private PendingIntent gpsPending() {
        Intent intent = new Intent(this, LocBR.class);
        intent.setAction(LocBR.GPS_ACTION);
        return PendingIntent.getBroadcast(this, 2244, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void scheduleNotification() {
        AlarmManager mAlarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        surveyAlarm = PendingIntent.getBroadcast(getApplicationContext(), 1234, new Intent(getApplicationContext(), SurveyPublisher.class), 0);
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 60 * 24 * 7 * 2, surveyAlarm);
    }

    public void cancelJob(View view) {
        Log.i(TAG, "Removing location updates");
        mFusedLocClient.removeLocationUpdates(gpsPending());
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 123) {
                scheduler.cancel(123);
                Toast.makeText(this, "gps collection canceled", Toast.LENGTH_SHORT).show();
            }
            if (jobInfo.getId() == 404) {
                scheduler.cancel(404);
                Toast.makeText(this, "usage collection canceled", Toast.LENGTH_SHORT).show();
            }
        }
        if (surveyAlarm != null) {
            ((AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE)).cancel(surveyAlarm);
            Toast.makeText(this, "usage collection canceled", Toast.LENGTH_SHORT).show();
        }
    }

    public void scheduleAppU(View view) {
        if (!usagePerm()) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Usage Permission Required!")
                    .setMessage("Please enable app usage permissions for MyData in the settings.")
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    }).create().show();
        } else {
            getAppManual();
        }
    }

    public int getAppManual() {
        Toast.makeText(this, "Usage Stats recorded", Toast.LENGTH_SHORT);
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startD(), endD());
        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEs = new ArrayList<>();
        HashMap<String, Long> map = new HashMap<>();
        while (usageEvents.hasNextEvent()) {
            currentEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(currentEvent);
            if (currentEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    currentEvent.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                allEs.add(currentEvent);
                String key = currentEvent.getPackageName();
                if (map.get(key) == null)
                    map.put(key, 0L);
            }
        }
        for (int i = 0; i < allEs.size() - 1; i++) {
            UsageEvents.Event first = allEs.get(i);
            if (first.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                for (int a = i + 1; a < allEs.size(); a++) {
                    UsageEvents.Event second = allEs.get(a);
                    if (second.getPackageName().equals(first.getPackageName()) && second.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        long diff = second.getTimeStamp() - first.getTimeStamp();
                        i = a;
                        map.replace(first.getPackageName(), map.get(first.getPackageName()) + diff);
                        break;
                    }
                }
            }
        }
        List<Map.Entry<String, Long>> list = new LinkedList<Map.Entry<String, Long>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        for (Map.Entry<String, Long> entry : list) {
            long foregroundtime = entry.getValue();
            int seconds = (int) ((foregroundtime / 1000) % 60);
            int minutes = (int) ((foregroundtime / 60000) % 60);
            int hours = (int) ((foregroundtime / (60000 * 60)) % 24);
            Log.d(TAG, entry.getKey() + ":" + hours + "." + minutes + "." + seconds);
        }
        if (map.entrySet().size() == 0) {
            return 0;
        }
        return 1;
    }

    private long startD() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long endD() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public boolean usagePerm() {
        AppOpsManager appOps = (AppOpsManager) MainActivity.this.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), MainActivity.this.getPackageName());
        return (mode == AppOpsManager.MODE_ALLOWED);
    }

    private void requestGps() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("Gps permissions needed to record location data")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERM_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERM_CODE);
        }
    }

    public void requestPerms(View view) {
        boolean fineLocPerm = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        boolean backLocPerm = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        boolean storePerm = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        boolean shouldProvideRationale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            shouldProvideRationale = fineLocPerm || backLocPerm || storePerm;
        } else {
            shouldProvideRationale = fineLocPerm || storePerm;
        }
        if (cando() && usagePerm()) {
            Log.d(TAG, "start stuff");
            startStuff();
        } else if (!cando()) {
            if (shouldProvideRationale) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("Gps permissions needed to record location data. Storage permissions required to write to file.")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, ALL_REQ_CODE);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            ALL_REQ_CODE);
                    Log.d(TAG, "requesting O perm");
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            ALL_REQ_CODE);
                    Log.d(TAG, "requesting norm perm");
                }

            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("App usage permissions needed to record daily app usage.")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_REQ_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == ALL_REQ_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (cando()) {
                if (!usagePerm()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Needed")
                            .setMessage("App usage permissions needed to record daily app usage.")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_REQ_CODE);
                                }
                            })
                            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create().show();
                } else {
                    Log.d(TAG, "Start stuff");
                    startStuff();
                }
            } else {
                Toast.makeText(this, "Permissions denied. Please retry.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USAGE_REQ_CODE) {
            if (!usagePerm()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("App usage permissions needed to record daily app usage.")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_REQ_CODE);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            } else {
                Log.d(TAG, "Start stuff");
                startStuff();
            }
        }
    }

    private boolean cando() {
        boolean fperm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean bperm = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean eperm = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return (fperm && bperm && eperm);
        } else {
            return (fperm && eperm);
        }
    }

    private void requestStorage() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "external storage access granted", Toast.LENGTH_SHORT).show();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("External storage permissions needed to record location data")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERM_CODE);
                            }
                        })
                        .setNegativeButton("cancle", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERM_CODE);
            }
        }
    }

    private void requestUsage() {
        if (!usagePerm()) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Usage Permission Required!")
                    .setMessage("Please enable app usage permissions for MyData in the settings.")
                    .setNegativeButton("cancle", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    }).create().show();
        }
    }

    private void startStuff() {
        Toast.makeText(this, "Jobs scheduled!", Toast.LENGTH_LONG).show();
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        ComponentName dailyJ = new ComponentName(this, DailyJob.class);
        JobInfo secondJob = new JobInfo.Builder(404, dailyJ)
                .setPersisted(true)
                .setPeriodic(24 * 60 * 60 * 1000)
                .build();
        int result = scheduler.schedule(secondJob);
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled daily");
        } else {
            Log.d(TAG, "Job scheduler daily failed");
        }
        scheduleNotification();
        try {
            Log.i(TAG, "Starting location updates");
            mFusedLocClient.requestLocationUpdates(mLocReq, gpsPending());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void startSurvey(View view) {
        Intent intent = new Intent(this, SurveyActivity.class);
        startActivity(intent);
    }

    public void sendSftpData(View view) {
       new SFTPconnection(this).execute();
    }//end SFTP send data function

}

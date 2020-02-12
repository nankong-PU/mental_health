package com.itsmiki.mydata;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DailyJob extends JobService {
    private static final String TAG = "DailyJob";
    public static final int LISTSIZE = 10;
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");
        backgroundingLalala(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void backgroundingLalala(final JobParameters params){
        new Thread(new Runnable() {
            @Override
            public void run() {
                getAppManual();
                jobFinished(params,false);
            }
        }).start();
    }
    public void getAppManual(){
        //Toast.makeText(this, "Usage Stats recorded", Toast.LENGTH_SHORT);
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startD(), endD());
        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEs = new ArrayList<>();
        HashMap<String, Long> map = new HashMap<>();
        while(usageEvents.hasNextEvent()){
            currentEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(currentEvent);
            if (currentEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    currentEvent.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                allEs.add(currentEvent);
                String key = currentEvent.getPackageName();
                if(map.get(key)==null)
                    map.put(key,0L);
            }
        }
        Collections.sort(allEs,new Comparator<UsageEvents.Event>(){
            @Override
            public int compare(UsageEvents.Event a, UsageEvents.Event b) {
                return (int)(b.getTimeStamp()-a.getTimeStamp());
            }
        });
        for(int i = 0;i<allEs.size()-1;i++){
            UsageEvents.Event first = allEs.get(i);
            if(first.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND){
                for(int a = i+1; a < allEs.size();a++){
                    UsageEvents.Event second = allEs.get(a);
                    if(second.getPackageName().equals(first.getPackageName()) && second.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND){
                        long diff = second.getTimeStamp()-first.getTimeStamp();
                        i = a;
                        map.replace(first.getPackageName(),map.get(first.getPackageName())+diff);
                        break;
                    }
                }
            }
        }
        List<Map.Entry<String,Long>> list = new LinkedList<Map.Entry<String, Long>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>()
        {
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return a.getValue().compareTo(b.getValue());
            }
        });
        String toprint = DateFormat.format("dd-MM-yyyy:", new java.util.Date()).toString();
        for(int z = 0; z<list.size() && z < LISTSIZE; z++){
            Map.Entry<String,Long> entry = list.get(z);
            //Map.Entry<String,Long> entry : list
            long foregroundtime = entry.getValue();
            int seconds = (int) ((foregroundtime / 1000) % 60);
            int minutes = (int) ((foregroundtime / 60000) % 60);
            int hours = (int) ((foregroundtime / (60000 * 60)) % 24);
            toprint+=entry.getKey()+":"+hours+"."+minutes+"."+seconds+" ";
        }
        if(map.entrySet().size() == 0){
            toprint+="error getting usageStats";
        }
        writeFile(toprint);
        toprint+="\n";
        Log.d(TAG, toprint);
    }
    private long startD() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE,-1);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTimeInMillis();
    }
    private long endD() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE,0);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        return cal.getTimeInMillis();
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public void writeFile(String input) {
        if (isExternalStorageWritable()) {
            File file = new File(getExternalFilesDir(null), "appU.txt");
            try {
                Log.d(TAG, file.getAbsolutePath());
                if (!file.exists()) {
                    file.createNewFile();
                    Log.d(TAG, "trying to create file");
                }
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write((input+"\n").getBytes());
                fos.flush();
                fos.close();
                //Toast.makeText(this, "file saved to: " + getExternalFilesDir(null), Toast.LENGTH_SHORT).show();
                if (fos != null) {
                    fos.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

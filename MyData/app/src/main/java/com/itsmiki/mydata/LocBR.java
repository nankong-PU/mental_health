package com.itsmiki.mydata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.itsmiki.mydata.MainActivity.ID_CODE;

public class LocBR extends BroadcastReceiver {
    static final String GPS_ACTION =
            "com.itsmiki.LocBR.action.PROCESS_UPDATES";
    static final String TAG = "LocBR";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"Entered recieve");
        if (intent != null) {
            final String action = intent.getAction();
            if (GPS_ACTION.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locationList = result.getLocations();
                    if (locationList.size() > 0) {
                        Location location = locationList.get(locationList.size() - 1);
                        String write = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString()) + " Location: " + location.getLatitude() + " " + location.getLongitude() + "\n";
                        Log.d(TAG, write);
                        writeFile(write,context);
                    }
                }
            }
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public void writeFile(String input, Context context) {
        if (isExternalStorageWritable()) {
            File file = new File(context.getExternalFilesDir(null), ID_CODE + "_gps.txt");
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

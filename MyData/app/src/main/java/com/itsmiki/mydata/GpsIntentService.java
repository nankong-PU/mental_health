package com.itsmiki.mydata;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationResult;

import java.util.List;

public class GpsIntentService extends BroadcastReceiver {
    static final String ACTION_PROCESS_UPDATES =
            "com.itsmiki.mydata.GpsIntentService.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        String write = "";
        if(intent != null){
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locationList = result.getLocations();
                    if (locationList.size() > 0) {
                        Location location = locationList.get(locationList.size() - 1);
                        Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                        write = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString()) + " Location: " + location.getLatitude() + " " + location.getLongitude() + "\n";
                    }
                    Log.i("gpsintentservice", write);
                }
            }
        }
    }
}

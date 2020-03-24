package com.itsmiki.mydata;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.itsmiki.mydata.NotifApp.CHANNEL_ID;
import static com.itsmiki.mydata.MainActivity.ID_CODE;

public class GpsService extends Service {
    public static final String TAG = "GPSSERVICE";
    private PowerManager.WakeLock wl;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    private FusedLocationProviderClient mFused;
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mFused != null) {
                    mFused.removeLocationUpdates(mLocationCallback);
                }
                String write = (DateFormat.format("dd-MM-yyyy hh:mm:ss", new java.util.Date()).toString()) + " Location: " + location.getLatitude() + " " + location.getLongitude() + "\n";
                Log.d(TAG, write);
                writeFile(write);
                stopSelf();
                wl.release();
            }
        }
    };

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public void writeFile(String input) {
        if (isExternalStorageWritable()) {
            File file = new File(getExternalFilesDir(null), ID_CODE + "_gps.txt");
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
                Toast.makeText(this, "file saved to: " + getExternalFilesDir(null), Toast.LENGTH_SHORT).show();
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFused = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notifi = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Grabbing gps data")
                .setContentText("stop from the app")
                .setSmallIcon(R.drawable.ic_android24dp)
                .setContentIntent(pendingIntent)
                .build();
        PowerManager pm = (PowerManager)this.getSystemService(
                this.POWER_SERVICE);
        wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK
                        | PowerManager.ON_AFTER_RELEASE,
                "mydata:wakelock");
        wl.acquire();
        startForeground(1, notifi);
        StartGPS();
        return START_NOT_STICKY;
    }

    public void StartGPS() {
        Log.d(TAG, "inside onclick");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // two minute interval
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(GpsService.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFused.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            } else {
                //Request Location Permission
                Log.i("MapsActivity", "permissions not granted");
            }
        } else {
            mFused.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }


    //26:32
}

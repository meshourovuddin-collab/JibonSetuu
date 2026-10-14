package com.jibonsetu.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LocationTrackingService extends Service {

    public static final String EXTRA_REQUEST_ID = "request_id";
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Location tracking implementation ekhane add korte hobe
        return START_NOT_STICKY;
    }
}
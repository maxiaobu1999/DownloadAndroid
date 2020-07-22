package com.malong.sample;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class App extends Application {
    public static final String TAG = "【App】";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, " attachBaseContext()");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, " onCreate()");
    }
}

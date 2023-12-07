package com.locnavi.iot.background_location_demo_app;


import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "e71cd704d7", false);
        LocNaviBroadcastReceiver receiver = new LocNaviBroadcastReceiver();
        this.registerReceiver(receiver, new IntentFilter("com.locanavi.locationonline.broadcast"));
    }
}


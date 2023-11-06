package com.locnavi.iot.background_location_demo_app;


import android.app.Application;
import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "e71cd704d7", false);
    }
}


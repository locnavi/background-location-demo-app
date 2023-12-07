package com.locnavi.iot.background_location_demo_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.locnavi.location.online.LocNaviClient;

import java.util.Date;
import java.util.HashMap;

public class LocNaviBroadcastReceiver extends BroadcastReceiver {
    protected static String TAG = "LocNaviBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();
        LocNaviClient client = LocNaviClient.getInstanceForApplication(app);

        Bundle bundle = intent.getExtras();
        String action = intent.getAction();


        Log.d(TAG, "收到通知：" + action);

        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            int level = intent.getIntExtra("level", 0);
            batteryChanged(app, level);
        } else if (action.equals("com.locanavi.locationonline.setBaseUri")) {
            String data = bundle.get("data").toString();
            setBaseUri(client, data);
        } else if (action.equals("com.locanavi.locationonline.setUserInfo")) {
            setUserInfo(client, bundle);
        } else if (action.equals("com.locanavi.locationonline.startLocation")) {
            startLocation(client, bundle);
        } else if (action.equals("com.locanavi.locationonline.event")) {
            trackEvent(client, bundle);
        }
    }

    private void batteryChanged(Context app, int level) {
        SharedPreferences battery = app.getSharedPreferences("battery", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = battery.edit();
        long time = new Date().getTime();
        editor.putInt(time + "", level);
        editor.commit();
    }

    private void setBaseUri(LocNaviClient client, String data) {
        client.setBaseUri(data);
    }

    private void setUserInfo(LocNaviClient client, Bundle bundle) {
        try {
            HashMap map = (HashMap) bundle.getSerializable("data");
            client.setUserInfo(map.get("name").toString(),  map.get("id").toString());
        } catch (Exception e) {
            Log.d(TAG, "setUserInfo" + e);
            client.setUserInfo(null, null);
        }
    }
    private void startLocation(LocNaviClient client, Bundle bundle) {
        try {
            HashMap map = (HashMap) bundle.getSerializable("data");
            String function = map.get("function").toString();
            String mode = map.get("mode").toString();
            if (function.equals("start")) {
                client.start(mode);
            } else if (function.equals("stop")) {
                client.stop(mode);
            }
        } catch (Exception e) {
            Log.e(TAG, "startLocation" + e);
            client.stop();
        }
    }

    private void trackEvent(LocNaviClient client, Bundle bundle) {
        try {
            String event = bundle.get("event").toString();
            HashMap map = (HashMap) bundle.getSerializable("data");
            client.track(event, map);
        }catch (Exception e) {
            Log.d(TAG,  "event" + e);
        }
    }
}

package com.locnavi.iot.background_location_demo_app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.locnavi.location.online.LocNaviClient;
import com.locnavi.location.online.event.LocNaviConstants;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    EditText editTextURL;
    EditText editTextUserName;
    EditText editTextUserId;

    protected static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextURL = findViewById(R.id.editTextTextURL);
        editTextUserName = findViewById(R.id.editTextUserName);
        editTextUserId = findViewById(R.id.editTextUserId);
        editTextURL.setText(Constants.serverUrl);
        editTextUserName.setText(Constants.userName);
        editTextUserId.setText(Constants.userId);

        //初始化SDK
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        Log.d("App", client.getVersionName());

        verifyBluetooth();
        verifyLocation();
        requestPermissions();
    }

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;

    private void verifyBluetooth() {
        try {
            if (!LocNaviClient.getInstanceForApplication(this).checkAvailability()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                boolean isEnabled = bluetoothAdapter.isEnabled();
                if (!isEnabled) {
                    //打开蓝牙
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    bluetoothAdapter.enable();
                }
                // new AlertDialog.Builder(this)
                //     .setTitle("蓝牙未开启")
                //     .setMessage("室内定位基于蓝牙扫描，请打开蓝牙使用")
                //     .setPositiveButton(android.R.string.ok, null)
                //     .show();
            }
        }
        catch (RuntimeException e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.bluetooth_unavailable)
                    .setMessage(R.string.bluetooth_unavailable_tip)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    //判断手机定位功能是否开启
    private void verifyLocation() {
        if (!this.isLocationEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.gps_unopen)
                    .setMessage(R.string.gps_unopen_tip)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //openGPS(MainActivity.this);
                            //跳转GPS设置界面
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            MainActivity.this.startActivity(intent);
                        }
                    });
            builder.setCancelable(true);
            builder.show();
        }
    }

    //判断定位功能是否开启
    private boolean isLocationEnabled() {
        int locationMode = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            String locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        //请求背景定位
                        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                PERMISSION_REQUEST_BACKGROUND_LOCATION);
                    }
                }
            } else {
                //未授权或者拒绝的都直接申请，其中拒绝且点击不再提醒的会直接调用回调
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERMISSION_REQUEST_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "定位授权成功");
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.tip)
                            .setMessage(R.string.tip_set_gps)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                return;
            }
            case PERMISSION_REQUEST_BACKGROUND_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "背景定位授权成功");
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.tip)
                            .setMessage(R.string.tip_set_gps_bg)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                return;
            }
        }
    }

    public void onClickSetURL(View view) {
        Log.d(TAG, "onClickSetURL");
        String text = editTextURL.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.setBaseUri(text);
    }

    public void onClickLoginIn(View view) {
        Log.d(TAG, "onClickLoginIn");
        String name = editTextUserName.getText().toString();
        String id = editTextUserId.getText().toString();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(id)) {
            return;
        }
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.setUserInfo(name, id);
    }

    public void onClickLoginOut(View view) {
        Log.d(TAG, "onClickLoginOut");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.setUserInfo(null, null);
    }

    public void onClickStartLocation(View view) {
        Log.d(TAG, "onClickStartLocation");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.start();
    }

    public void onClickStopLocation(View view) {
        Log.d(TAG, "onClickStopLocation");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.stop();
    }

    public void onClickStart(View view) {
        Log.d(TAG, "onClickStart");
        HashMap properties = new HashMap();
        properties.put("DELIVERY_CODE", "12344");
        properties.put("DEPT_STORE_ID", "568");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.track(LocNaviConstants.EVENT_START_DELIVERY, properties);
    }

    public void onClickEnd(View view) {
        Log.d(TAG, "onClickEnd");
        HashMap properties = new HashMap();
        properties.put("DELIVERY_CODE", "12344");
        properties.put("DEPT_STORE_ID", "568");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.track(LocNaviConstants.EVENT_END_DELIVERY, properties);
    }

    public void onClickCancel(View view) {
        Log.d(TAG, "onClickCancel");
        HashMap properties = new HashMap();
        properties.put("DELIVERY_CODE", "12344");
        properties.put("DEPT_STORE_ID", "568");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.track(LocNaviConstants.EVENT_CANCEL_DELIVERY, properties);
    }
}
# background-location-demo-app
背景定位測試APP，支持接受一些事件，並上傳至預設的伺服器。

## AndroidX工程的Library引用
通過jitpack將github上的aar引入到工程中。
在setting.gradle中添加
```bash
    maven { url 'https://jitpack.io' }
```

在app的build.gradle中添加
```bash
    // use jitpack from github
    implementation 'com.github.locnavi:AndroidLocationOnlineSDK:0.1.3'
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation 'org.altbeacon:android-beacon-library:2+'
```

## support工程的Library引用
Android support的項目調用AndroidX提供的aar可能會有問題。我們可以將aar轉成支持android support。通過jetifier-standalone工具轉化。參考官方鏈接(https://developer.android.google.cn/studio/command-line/jetifier)
轉換成功後即可用本地aar引入使用

## 加入權限
在AndroidMainfest.xxml中添加
```bash
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    //檢測到未打開藍牙時能自動打開藍牙
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
```

## SDK的使用

### 初始化
在Application的onCreate方法中添加
```java
        //初始化SDK
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.setBaseUri("http://192.168.2.16:8086");
        //在App獲取到用戶信息之後調用
        client.setUserInfo("pda", "123456");
        //需要跟我們確認采集方式，默認使用BEACON_MODE_IBEACON
        //client.setBeaconMode(LocNaviConstants.BEACON_MODE_BEACON);
```

#### 定位權限及藍牙功能檢測
在開啟定位之前先確認藍牙及定位的權限已經開啟
```java
        verifyBluetooth();
        verifyLocation();
        requestPermissions();
```


```java
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 2;

    private void verifyBluetooth() {
        try {
            if (!LocNaviClient.getInstanceForApplication(this).checkAvailability()) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                boolean isEnabled = bluetoothAdapter.isEnabled();
                if (!isEnabled) {
                    //打開藍牙
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
                //     .setTitle("藍牙未開啟")
                //     .setMessage("室內定位基於藍牙掃描，請打開藍牙使用")
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

    //判斷手機定位功能是否開啟
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
                            //跳轉GPS設置界面
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            MainActivity.this.startActivity(intent);
                        }
                    });
            builder.setCancelable(true);
            builder.show();
        }
    }

    //判斷定位功能是否開啟
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
                        //請求背景定位
                        requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                PERMISSION_REQUEST_BACKGROUND_LOCATION);
                    }
                }
            } else {
                //未授權或者拒絕的都直接申請，其中拒絕且點擊不再提醒的會直接調用回調
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
                    Log.d(TAG, "定位授權成功");
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
                    Log.d(TAG, "背景定位授權成功");
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
```

#### 開啟定位
```java
  client.start();
```

### 觸發事件

事件的字段需要提前溝通、後臺根據傳入的字段做解析
#### 1、登錄事件
```java
    //傳入用戶名、用戶id即可
    client.setUserInfo("pda", "123456");
```
#### 2、登出事件
```java
    client.setUserInfo(null, null);
```
#### 3、開始配送事件
```java
        //盡量將有用的配送單的信息都放傳入
        Map properties = new HashMap();
        properties.put("DELIVERY_CODE", "12344");
        properties.put("DEPT_STORE_ID", "568");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.track(LocNaviConstants.EVENT_START_DELIVERY, properties);
```
#### 4、結束配送事件
```java
        //可只傳入配送單id
        Map properties = new HashMap();
        properties.put("DELIVERY_CODE", "12344");
        properties.put("DEPT_STORE_ID", "568");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.track(LocNaviConstants.EVENT_END_DELIVERY, properties);
```
#### 5、取消配送事件
```java
        //可只傳入配送單id
        Map properties = new HashMap();
        properties.put("DELIVERY_CODE", "12344");
        properties.put("DEPT_STORE_ID", "568");
        LocNaviClient client = LocNaviClient.getInstanceForApplication(this);
        client.track(LocNaviConstants.EVENT_CANCEL_DELIVERY, properties);
```

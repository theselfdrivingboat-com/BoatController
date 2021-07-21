package com.selfdrivingboat.boatcontroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.datadog.android.log.Logger;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.hardware.SensorManager.DATA_X;
import static android.hardware.SensorManager.DATA_Y;
import static android.hardware.SensorManager.DATA_Z;


public class MainActivity extends AppCompatActivity implements OnBluetoothDeviceClickedListener,SensorEventListener {
    private final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1;


    private static final int REQUEST_CONNECT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    public static final String EXTRAS_DEVICE_NAME = "extras_device_name";
    public static final String EXTRAS_DEVICE_ADDRESS = "extras_device_address";
    public String mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000 * 10;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private MyBluetoothDeviceAdapter mBluetoothDeviceAdapter;
    private List<BluetoothDevice> mBluetoothDeviceList = new ArrayList<>();
    private MyBluetoothScanCallBack mBluetoothScanCallBack = new MyBluetoothScanCallBack();
    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;
    public Logger logger = DatadogLogger.getInstance();
    private SelfDriving selfDriving = new SelfDriving();
    public FusedLocationProviderClient fusedLocationClient;

    public RequestQueue volleyQueue;
    private SensorManager sensorManager;
    private Sensor mACCELEROMETER;
    private Sensor mMagneticField;
    private Sensor mtemperature;
    public float  acceleRometer_x = 0,  acceleRometer_y = 0,  acceleRometer_z = 0;
    public int accellerometer_count = 0;
    public int  temprature_count = 0;
    public float temperature;
    public float inclination = 0;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // SENSORS
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mACCELEROMETER = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mACCELEROMETER != null) {
            sensorManager.registerListener(this,mACCELEROMETER, SensorManager.SENSOR_DELAY_UI);
        }
        mMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mMagneticField != null) {
            sensorManager.registerListener(this, mMagneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        mtemperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (mtemperature != null) {
            sensorManager.registerListener(this, mtemperature,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        else{
            this.logger.i("[androidTemperature]SENSOR NOT FOUND");
        }
        InfluxDBWrites.sendBluetoothStatus(MainActivity.this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Bitmap mBitmap = screenShot(findViewById(android.R.id.content).getRootView());

        setContentView(R.layout.activity_main);

        volleyQueue = VolleySingleton.getInstance(this.getApplicationContext()).
                getRequestQueue();

        initView();
        requestPermission();
        initData();
        initService();
        logger.w("main activity started");
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    /* computes inclination from current acceleromoter values s
    stored in acceleromoeterReading
    TODO (mack): make sure this work, if it doesn't change it with your magnetometerReading with rotMatrix
    run the add and log the value of inclination and check if it changes when you move the phone
     */
    private float computeInclination(){
        float [] g = accelerometerReading.clone();

        double norm_Of_g = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

        // Normalize the accelerometer vector
        g[0] = (float) (g[0] / norm_Of_g);
        g[1] = (float) (g[1] / norm_Of_g);
        g[2] = (float) (g[2] / norm_Of_g);
        float inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));
        // logged to often
        // logger.i(String.valueOf(inclination));
        return inclination;

    }

    /* we want to keep a running max of accellerometer values
    that we send every X seconds to the database, instead of sending at every reading
     */
    private void updateAccelerometerValues(float[] currentAccelerometerValues, float currentInclination){
        // TODO (mack): can we fix this variable name and put R lower case? let's try to keep namin consistent
        // will increase our dev speed
        if(Math.abs(currentAccelerometerValues[0]) > Math.abs(acceleRometer_x)){
            acceleRometer_x = currentAccelerometerValues[0];
        };
        if(Math.abs(currentAccelerometerValues[1]) > Math.abs(acceleRometer_y)){
            acceleRometer_y = currentAccelerometerValues[1];
        };
        if(Math.abs(currentAccelerometerValues[2]) > Math.abs(acceleRometer_z)){
            acceleRometer_z = currentAccelerometerValues[2];
        };
        if(Math.abs(currentInclination) > Math.abs(inclination)){
            inclination = currentInclination;
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // commenting this logger cause this get called 2 times a second
        //this.logger.i("[androidAccelerometer] onSensorChanged");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
            float currentInclination = computeInclination();

            accellerometer_count += 1;
            updateAccelerometerValues(accelerometerReading, currentInclination);
            if (accellerometer_count == 200) {
                this.logger.i("[androidAccelerometer] count triggered");
                class sendDataTask extends AsyncTask<Void, Void, Boolean> {
                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        // TODO (mack): send all data including inclination to influxdb make sure they arrive on the other end
                        InfluxDBWrites.sendAndroidAccelerometer(acceleRometer_x, acceleRometer_z, acceleRometer_y, inclination);
                        logger.i(String.valueOf(inclination));
                        return true;
                    }

                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            MainActivity.this.logger.i("[androidAccelerometer] data sent to influxdb success");
                        } else {
                            MainActivity.this.logger.i("[androidAccelerometer] data sent to influxdb fail");
                        }
                    }

                }
                new sendDataTask().execute();
                accellerometer_count = 0;
                acceleRometer_x = 0;
                acceleRometer_y = 0;
                acceleRometer_z = 0;
                inclination = 0;
            }

        } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            this.logger.i("[androidTemperature] onSensorChanged");
            temperature = event.values[0];
            logger.i(String.valueOf(temperature));
            temprature_count += 1;
            if (temprature_count == 2) {
                class sendDataTask extends AsyncTask<Void, Void, Boolean> {
                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        InfluxDBWrites.sendMPU6050ambient_temperature(temperature);
                        return true;
                    }
                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            MainActivity.this.logger.i(" [androidTemperature] data sent to influx");
                        } else {
                            MainActivity.this.logger.i("[androidTemperature] data sent to influxdb fail");
                        }
                    }
                }
                new sendDataTask().execute();
                temperature = 0;
                temprature_count = 0;
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,mACCELEROMETER, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,mtemperature, SensorManager.SENSOR_DELAY_NORMAL);
        initReceiver();
        scanLeDevice(true);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("MainActivity", "unregisterReceiver()");
        unregisterReceiver(mGattUpdateReceiver);
        sensorManager.unregisterListener(this);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check.
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
    }

    private void initService() {
        Log.i("MainActivity", "initService()");

        if (mBluetoothLeService == null) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    private void initData() {
        mHandler = new Handler();
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        mBluetoothDeviceAdapter = new MyBluetoothDeviceAdapter(mBluetoothDeviceList, this);
        recyclerView.setAdapter(mBluetoothDeviceAdapter);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mBluetoothDeviceList != null) {
                    mBluetoothDeviceList.clear();
                }
                scanLeDevice(true);
            }
        });


    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("MainActivity", "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void scanLeDevice(boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    swipeRefresh.setRefreshing(false);
                    BluetoothScan.stopScan();
                }
            }, SCAN_PERIOD);
            swipeRefresh.setRefreshing(true);
            BluetoothScan.startScan(true, mBluetoothScanCallBack);
        } else {
            swipeRefresh.setRefreshing(false);
            BluetoothScan.stopScan();
        }
    }

    @Override
    public void onBluetoothDeviceClicked(String name, String address) {

        Log.i("MainActivity", "Attempt to connect device : " + name + "(" + address + ")");
        mDeviceName = name;
        mDeviceAddress = address;

        if (mBluetoothLeService != null) {

            if (mBluetoothLeService.connect(mDeviceAddress)) {
                showMsg("Attempt to connect device : " + name);

                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTING;
                swipeRefresh.setRefreshing(true);
            }
        }
    }

    private void initReceiver() {
        Log.i("MainActivity", "initReceiver()");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("MainActivity", "ACTION_GATT_CONNECTED!!!");
                showMsg("Connected device ..");

                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTED;
                swipeRefresh.setRefreshing(false);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("MainActivity", "ACTION_GATT_DISCONNECTED!!!");
                showMsg("disconnected");
                mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED;
                swipeRefresh.setRefreshing(false);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                Log.i("alex", "service discovered");
                mBluetoothLeService.getSupportedGattServices();
                // this spawn recurrent async tasks with volley
                selfDriving.start(MainActivity.this);

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                float received = buffer.getFloat();
                Log.i("MainActivity", "BLE notify " + received);
                selfDriving.receiveBLData(received);
            }
        }
    };


    public void btSendBytes(byte[] data) {
        Log.i("alex", String.valueOf(data));
        if (mBluetoothLeService != null &&
                mConnectionState.equals(BluetoothLeService.ACTION_GATT_CONNECTED)) {
            mBluetoothLeService.writeCharacteristic(data);
        }
    }

    private class MyBluetoothScanCallBack implements BluetoothScan.BluetoothScanCallBack {
        @Override
        public void onLeScanInitFailure(int failureCode) {
            Log.i("MainActivity", "onLeScanInitFailure()");
            switch (failureCode) {
                case BluetoothScan.SCAN_FEATURE_ERROR:
                    showMsg("scan_feature_error");
                    break;
                case BluetoothScan.SCAN_ADAPTER_ERROR:
                    showMsg("scan_adapter_error");
                    break;
                default:
                    showMsg("unKnow_error");
            }
        }

        @Override
        public void onLeScanInitSuccess(int successCode) {
            Log.i("MainActivity", "onLeScanInitSuccess()");
            switch (successCode) {
                case BluetoothScan.SCAN_BEGIN_SCAN:
                    Log.i("MainActivity", "successCode : " + successCode);
                    break;
                case BluetoothScan.SCAN_NEED_ENADLE:
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    break;
                case BluetoothScan.AUTO_ENABLE_FAILURE:
                    showMsg("auto_enable_bluetooth_error");
                    break;
                default:
                    showMsg("unKnow_error");
            }
        }

        @Override
        public void onLeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!mBluetoothDeviceList.contains(device) && device != null) {
                mBluetoothDeviceList.add(device);
                mBluetoothDeviceAdapter.notifyDataSetChanged();

                Log.i("MainActivity", "notifyDataSetChanged() " + "BluetoothName :　" + device.getName() +
                        "  BluetoothAddress :　" + device.getAddress());

                if ("MyESP32".equals(device.getName())) {
                    Log.i("alex", "we connected to MyESP32.. automatically connecting");

                    if (mBluetoothLeService != null) {

                        if (mBluetoothLeService.connect(device.getAddress())) {
                            showMsg("Attempt to connect device : " + device.getName());

                            mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTING;
                            swipeRefresh.setRefreshing(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                showMsg("enable_bluetooth_error");
                return;
            } else if (resultCode == Activity.RESULT_OK) {
                if (mBluetoothDeviceList != null) {
                    mBluetoothDeviceList.clear();
                }
                scanLeDevice(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static Toast toast = null;

    public static void showMsg(String msg) {


        try {
            if (toast == null) {
                toast = Toast.makeText(MyApplication.context(), msg, Toast.LENGTH_SHORT);
            } else {
                toast.setText(msg);
            }
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap screenShot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
        return bitmap;
    }

}
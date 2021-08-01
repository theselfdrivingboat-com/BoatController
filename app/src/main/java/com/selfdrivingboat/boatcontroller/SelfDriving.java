package com.selfdrivingboat.boatcontroller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.HandlerThread;
import android.os.Looper;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthWcdma;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.selfdrivingboat.boatcontroller.DatadogLogger.logger;

public class SelfDriving {

    MainActivity activity;
    String last_command; // "FORWARD", .. , ""
    int clock = 10;
    boolean testing_motors = false;
    int data_cursor = 0;
    int data_size = 23;
    float[] data = new float[data_size];
    float GPS_target_latitude;
    float GPS_target_longitude;
    boolean self_driving = false;

    ArrayList<Location> locations = new ArrayList<Location>();
    ArrayList<Location> selfdriving_locations = new ArrayList<Location>();

    private void boatStop() {
        sendStringToESP32("5");
    }

    private void boatForward() {
        sendStringToESP32("1", 20);
    }

    private void boatBackward() {
        sendStringToESP32("2");
    }

    private void boatLeft() {
        sendStringToESP32("3", 5);
    }

    private void boatRight() {
        sendStringToESP32("4", 5);
    }

    private void boatLowPower() {
        sendStringToESP32("6");
    }

    private void boatMidPower() {
        sendStringToESP32("7");
    }

    private void boatHighPower() {
        sendStringToESP32("8");
    }

    private void boatTestMotors() {
        boatForward();
    }

    private void runHerokuCommand(String command) {
        switch (command) {
            case "FORWARD":
                boatForward();
                break;
            case "LEFT":
                boatLeft();
                break;
            case "RIGHT":
                boatRight();
                break;
            case "BACK":
                boatBackward();
                break;
            case "TEST-MOTORS":
                testing_motors = true;
                break;
            default:
                if(command.contains("GPS")){
                    selfDrivingCommand(command);
                } else {
                    boatStop();
                }
        }
    }

    private void selfDrivingCommand(String GPS_string){
        //GPS--0.2421656731966487-51.572429083966554
        activity.logger.i( "received selfDrivingCommand");
        String[] parts = GPS_string.split(",");
        GPS_target_longitude = Float.parseFloat(parts[2]);
        GPS_target_latitude = Float.parseFloat(parts[1]);
        activity.logger.i( String.valueOf(GPS_target_longitude));
        activity.logger.i( String.valueOf(GPS_target_latitude));
        self_driving = true;
    }

    private void sendStringToESP32(String value, int time) {
        activity.logger.i( "sleeping " + time);
        activity.logger.i( value);
        activity.logger.i( Arrays.toString(value.getBytes()));
        activity.btSendBytes(value.getBytes());
        try {
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendStringToESP32(String value) {
        activity.logger.i( "sleeping 3");
        activity.logger.i( value);
        activity.logger.i( Arrays.toString(value.getBytes()));
        activity.btSendBytes(value.getBytes());
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void selfdriving_step() {
        // Initialize a new RequestQueue instance
        activity.logger.i( "new step");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            activity.logger.e(String.valueOf(e));
        }
        sendAndroidData();

        String url = "https://theselfdrivingboat.herokuapp.com/read_last_command?boat_name=5kgboat-001";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            activity.logger.i( "heorku renponse");
                            if (response.getString("command") == "null") {
                                last_command = "null";
                            } else {
                                testing_motors = false;
                                self_driving = false;
                                last_command = response.getJSONArray("command").getString(0);
                            }
                            activity.logger.i( last_command);
                            boatStop();
                            boatLowPower();
                            if (testing_motors) {
                                boatTestMotors();
                            }
                            if (self_driving) {
                                activity.logger.i( "self driving logic..");
                                Location before = locations.get(locations.size() - 1);
                                selfdriving_locations.add(before);
                                activity.logger.i( "BEFORE LOCATION");
                                activity.logger.i( String.valueOf(before));
                                boatForward();
                                boatForward();
                                Location after = locations.get(locations.size() - 1);
                                selfdriving_locations.add(after);
                                activity.logger.i( "AFTER LOCATION");
                                activity.logger.i( String.valueOf(after));
                                boolean goRight = selfDrivingPolicy_Right(
                                        before.getLongitude(),
                                        before.getLatitude(),
                                        after.getLongitude(),
                                        after.getLatitude(),
                                        GPS_target_longitude,
                                        GPS_target_latitude
                                );
                                // TODO: currently goRight is opposite of true, but also
                                // motors are wired opposite so NOT + NOT is YES
                                activity.logger.i( "GO RIGHT?");
                                activity.logger.i(String.valueOf(goRight));
                                if (goRight) {
                                    boatRight();
                                } else {
                                    boatLeft();
                                }
                            }
                            runHerokuCommand(last_command);
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException e) {
                                activity.logger.e(String.valueOf(e));
                            }
                            boatStop();
                            try {
                                TimeUnit.SECONDS.sleep(clock);
                            } catch (InterruptedException e) {
                                activity.logger.e(String.valueOf(e));
                            }
                            selfdriving_step();

                        } catch (JSONException e) {
                            activity.logger.e("Error from HEROKU!!", e);
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException e2) {
                                activity.logger.e(String.valueOf(e2));
                            }
                            selfdriving_step();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        activity.logger.e(String.valueOf(error));
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            activity.logger.e(String.valueOf(e));
                        }
                        selfdriving_step();
                    }
                });

        // Access the RequestQueue through your singleton class.
        activity.volleyQueue.add(jsonObjectRequest);

    }

    private boolean selfDrivingPolicy_Right(
            double before_lon, // A
            double before_lat, // A
            double after_lon, // B
            double after_lat, // B
            float target_lon, // T
            float target_lat){
        // longitude X
        // latitude Y
        return (after_lat - before_lat) * (target_lon - after_lon)
                > (after_lon - before_lon) * (target_lat - after_lat);
    }

    private void testDrive() {
        sendStringToESP32("7");
        sendStringToESP32("1");
        sendStringToESP32("5");
    }



    public void requestSingleUpdate() {
        // TODO: Comment-out this line.
        // Looper.prepare();


        // only works with SDK Version 23 or higher
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (activity.getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || activity.getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // permission is not granted
                Log.e("SiSoLocProvider", "Permission not granted.");
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},12
                        );
                return;
            } else {
                Log.d("SiSoLocProvider", "Permission granted.");
            }
        } else {
            Log.d("SiSoLocProvider", "SDK < 23, checking permissions should not be necessary");
        }

        // TODO: Start a background thread to receive location result.
        final HandlerThread handlerThread = new HandlerThread("RequestLocation");
        handlerThread.start();

        final long startTime = System.currentTimeMillis();
        LocationCallback fusedTrackerCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                activity.logger.i( "LOCATION: " + locationResult.getLastLocation().getLatitude() + "|" + locationResult.getLastLocation().getLongitude());
                activity.logger.i( "ACCURACY: " + locationResult.getLastLocation().getAccuracy());
                locations.addAll(locationResult.getLocations());
            }
        };

        LocationRequest req = new LocationRequest();
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        req.setFastestInterval(10000);
        req.setInterval(10000);
        // TODO: Pass looper of background thread indicates we want to receive location result in a background thread instead of UI thread.
        activity.fusedLocationClient.requestLocationUpdates(req, fusedTrackerCallback, handlerThread.getLooper());
    }


    public void start(MainActivity mainActivity) {
        activity = mainActivity;
        requestSingleUpdate();
        selfdriving_step();
    }

    private int getSignalStrength(){
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activity.logger.e("PERMISSION ERROR FOR 4G SIGNAL STRENGTH");
            return -1;
        }
        CellInfoWcdma cellInfoGsm = (CellInfoWcdma) activity.mTelephonyManager.getAllCellInfo().get(0);
        CellSignalStrengthWcdma cellSignalStrengthGsm = cellInfoGsm.getCellSignalStrength();
        int dbm = cellSignalStrengthGsm.getDbm();
        activity.logger.i("signal strength "+ dbm);
        return dbm;
    }

    public void sendAndroidData(){
        class sendDataTask extends AsyncTask<Void, Void, Boolean> {
            @Override
            protected Boolean doInBackground(Void... voids) {
                activity.logger.i( "sending influxdb anroid data.. doInBackground");
                InfluxDBWrites.sendBluetoothStatus(activity);
                activity.logger.i( "bluetooth status logging to influx  .. doInBackground");
                InfluxDBWrites.sendSignalStrength(getSignalStrength());
                activity.logger.i( "signalstrength logging to influx.. doInBackground");
                if (locations != null && !locations.isEmpty()) {
                    InfluxDBWrites.sendGPS(locations.get(locations.size() - 1));
                }
                return true;
            }

            protected void onPostExecute(Boolean result) {
                if (result) {
                    activity.logger.i( "android data sent to influxdb success");
                } else {
                    activity.logger.i( "android data sent to influxdb fail");
                }
            }

        }
        new sendDataTask().execute();

    }

    public void sendBLData() {

        class sendDataTask extends AsyncTask<Void, Void, Boolean> {
            @Override
            protected Boolean doInBackground(Void... voids) {
                activity.logger.i( "sending influxdb bluetooth data.. doInBackground");
                // WARNING: if you add some data here you need to increase data_size
                // or the app breaks with out of bound exception
                InfluxDBWrites.sendMPU6050Accelerometer(data[0], data[1], data[2]);
                InfluxDBWrites.sendMPU6050Gyroscope(data[3], data[4], data[5]);
                InfluxDBWrites.sendMPU6050Angle(data[6], data[7]);
                InfluxDBWrites.sendMPU6050Temperature(data[8]);
                InfluxDBWrites.sendBatteryLevel(data[9]);
                InfluxDBWrites.sendAndroidAccelerometer(data[10], data[11], data[12],data[13]);
                InfluxDBWrites.sendMPU6050ambient_temperature(data[14]);
                InfluxDBWrites.sendMPU6050magnetic_field(data[15], data[16], data[17]);
                InfluxDBWrites.sendMPU6050proximity(data[18]);
                InfluxDBWrites.sendMPU6050light(data[19]);
                InfluxDBWrites.sendMPU6050gravity(data[20], data[21], data[22]);
                if (locations != null && !locations.isEmpty()) {
                    InfluxDBWrites.sendGPS(locations.get(locations.size() - 1));
                }
                return true;
            }

            protected void onPostExecute(Boolean result) {
                if (result) {
                    activity.logger.i( "bluetooth data sent to influxdb success");
                } else {
                    activity.logger.i( "bluetooth data sent to influxdb fail");
                }
            }

        }
        new sendDataTask().execute();

    }

    public void receiveBLData(float val) {
        data[data_cursor] = val;
        data_cursor += 1;
        if (data_cursor == data_size) {
            sendBLData();
            data_cursor = 0;
        }
    }
}



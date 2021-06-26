package com.selfdrivingboat.boatcontroller;

import android.location.Location;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InfluxDBWrites {
    // https://docs.influxdata.com/influxdb/cloud/api/#operation/PostWrite

    private static String token = "XKGbhSKhGFiGATUD9rk9GphKjvJOsbdJSxbAJE8Al24oHN4GLSIcTpYYnV2VZ_5YgWc_094i1E6q5vKxAmT4hQ==";
    private static String org = "5dbd651117628225";
    private static String bucket = "alessandro.solbiati%27s%20Bucket";
    private static String url = "https://westeurope-1.azure.cloud2.influxdata.com/api/v2/write?org=5dbd651117628225&bucket=alessandro.solbiati%27s%20Bucket&precision=s";

    public static final MediaType MEDIA_TYPE_PLAIN
            = MediaType.parse("text/plain; charset=utf-8");

    private static String lineProtocol(
            String measurement,
            String tagk,
            String tagv,
            String pointk,
            String pointv){
        long unixTime = System.currentTimeMillis() / 1000L;
        return String.format("%s,%s=%s %s=%s %s\n",measurement,tagk,tagv,pointk,pointv,String.valueOf(unixTime));
    }

    public static void sendBluetoothStatus(MainActivity activity) {
        HTTPwrite("bluetooth",
                "device", "BV5500",
                "status", "\"" + activity.mConnectionState + "\"");
    }

    public static void sendMPU6050Accelerometer(float x, float y, float z,float inclination){
        HTTPwrite("accelerometer",
                "device", "MPU6050",
                "x", String.valueOf(x));
        HTTPwrite("accelerometer",
                "device", "MPU6050",
                "y", String.valueOf(y));
        HTTPwrite("accelerometer",
                "device", "MPU6050",
                "z", String.valueOf(z));
        HTTPwrite("accelerometer",
                "device", "MPU6050",
                "inclination", String.valueOf(inclination));
    }

    public static void sendAndroidAccelerometer( float x,float y, float z,float inclination){
        HTTPwrite("accelerometer",
                "device", "AndroidBV5500",
                "x", String.valueOf(x));
        HTTPwrite("accelerometer",
                "device", "AndroidBV5500",
                "y", String.valueOf(y));
        HTTPwrite("accelerometer",
                "device", "AndroidBV5500",
                "z", String.valueOf(z));
        HTTPwrite("accelerometer",
                "device", "AndroidBV5500",
                "i", String.valueOf(inclination));



    }

    public static void sendMPU6050Gyroscope(float x, float y, float z){
        HTTPwrite("gyroscope",
                "device", "MPU6050",
                "x", String.valueOf(x));
        HTTPwrite("gyroscope",
                "device", "MPU6050",
                "y", String.valueOf(y));
        HTTPwrite("gyroscope",
                "device", "MPU6050",
                "z", String.valueOf(z));
    }

    public static void sendMPU6050Angle(float x, float y){
        HTTPwrite("angle",
                "device", "MPU6050",
                "x", String.valueOf(x));
        HTTPwrite("angle",
                "device", "MPU6050",
                "y", String.valueOf(y));
    }

    public static void sendMPU6050Temperature(float temp){
        HTTPwrite("temperature",
                "device", "MPU6050",
                "x", String.valueOf(temp));
    }

    public static void sendBatteryLevel(float battery_level){
        HTTPwrite("battery",
                "device", "MPU6050",
                "V", String.valueOf(battery_level));
    }

    public static void sendGPS(Location location){
        HTTPwrite("GPS",
                "value", "BV5500",
                "speed", String.valueOf(location.getSpeed()));
        HTTPwrite("GPS",
                "value", "BV5500",
                "latitude", String.valueOf(location.getLatitude()));
        HTTPwrite("GPS",
                "value", "BV5500",
                "longitude", String.valueOf(location.getLongitude()));
    }

    private static void HTTPwrite(
            String measurement,
            String tagk,
            String tagv,
            String pointk,
            String pointv){
        // TODO: https://stackoverflow.com/questions/6343166/how-to-fix-android-os-networkonmainthreadexception#_=_
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        OkHttpClient client = new OkHttpClient();
        String postBody = lineProtocol(measurement,
                tagk, tagv,
                pointk, pointv);

        Request request;
        request = new Request.Builder()
                .url(url)
                .header("Authorization", "Token XKGbhSKhGFiGATUD9rk9GphKjvJOsbdJSxbAJE8Al24oHN4GLSIcTpYYnV2VZ_5YgWc_094i1E6q5vKxAmT4hQ==")
                .post(RequestBody.create(MEDIA_TYPE_PLAIN, postBody))
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            System.out.println(response.body().string());
        } catch (Exception e) {
            Log.i("influxdb", String.valueOf(e));
        }
    }
}
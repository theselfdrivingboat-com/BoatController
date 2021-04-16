package com.selfdrivingboat.boatcontroller;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class SelfDriving {

    MainActivity activity;
    String last_command; // "FORWARD", .. , ""
    int clock = 10;
    boolean testing_motors = false;

    private void boatStop(){
        sendStringToESP32("5");
    }

    private void boatForward(){
        sendStringToESP32("1");
    }

    private void boatBackward(){
        sendStringToESP32("2");
    }

    private void boatLeft(){
        sendStringToESP32("3");
    }

    private void boatRight(){
        sendStringToESP32("4");
    }

    private void boatLowPower(){
        sendStringToESP32("6");
    }

    private void boatMidPower(){
        sendStringToESP32("7");
    }

    private void boatHighPower(){
        sendStringToESP32("8");
    }

    private void boatTestMotors(){
        boatForward();
        boatStop();
        boatBackward();
    }

    private void runHerokuCommand(String command){
        switch(command) {
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
                boatStop();
        }
    }

    private void sendStringToESP32(String value){
        Log.i("alex", "sleeping 3");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i("alex", value);
        Log.i("alex", Arrays.toString(value.getBytes()));
        activity.btSendBytes(value.getBytes());
    }

    private void selfdriving_step(){
        // Initialize a new RequestQueue instance
        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(activity.getApplicationContext());

        String url = "https://theselfdrivingboat.herokuapp.com/read_last_command?boat_name=5kgboat-001";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getString("command") == "null"){
                                last_command = "null";
                            } else {
                                testing_motors = false;
                                last_command = response.getJSONArray("command").getString(0);
                            }
                            Log.i("selfdriving", last_command);
                            boatStop();
                            boatLowPower();
                            if (testing_motors) {
                                boatTestMotors();
                            }
                            runHerokuCommand(last_command);
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            boatStop();
                            try {
                                TimeUnit.SECONDS.sleep(clock);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            selfdriving_step();

                        } catch (JSONException e) {
                            Log.e("selfdriving", "no command key from heroku! server down or corrupted?");
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                    }
                });

        // Access the RequestQueue through your singleton class.
        requestQueue.add(jsonObjectRequest);

        InfluxDBWrites.sendBluetoothStatus(activity);
    }

    private void testDrive(){
        sendStringToESP32( "7");
        sendStringToESP32( "1");
        sendStringToESP32( "5");
    }

    public void start(MainActivity mainActivity){
        Log.i("selfdriving", "starting..");
        activity = mainActivity;
        selfdriving_step();
    }
}

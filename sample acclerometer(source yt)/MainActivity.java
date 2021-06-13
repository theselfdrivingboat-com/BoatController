package com.example.myapplicationas;

import androidx.appcompat.app.AppCompatActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
/*CODE FROM
* YT-Android Tutorial Spot
* video link-https://youtu.be/LsWJipo4knk*/
public class MainActivity extends AppCompatActivity implements SensorEventListener {
/*only the abv lines i understand*/

    private TextView textView;
    private SensorManager sensorManager;
    private Sensor sensor;




    /*i dnt know what type of variable or objects are created abv
    * i am only familiar with the one where we give data type first
    *  idk how this works */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*idk whats going in the brackets aftr on create
        *method are there parameters in brackets or something else idk..
        * what topic in java is it?*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*again idk whats going on above,did they declare mutiple methods in the above 3 lines
        * what topic in java do i need to study to understand these and their syntax*/
        textView = findViewById(R.id.text_accelerometer);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        /*again i am not exactly sure whats going on above
        * are they declaring a variable and then assigning the constant to the
        * get system service method and then its storing some value or something ? */
        sensor = sensorManager.getDefaultSensor(sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this,sensor,sensorManager.SENSOR_DELAY_NORMAL);
        /*i seriously dont know whats goin on in the brackets in the above lines*/

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate


/**/

        textView.setText(event.values[0]+"\n"+event.values[1]+"\n"+event.values[2]);
        /*so this is referring to the text view in the xml part and updating values i guess
        * but again idk the bracket part,whats goin on there whts its syntax
        * what do i need to study to get familiar to its syntax*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /*um is the abv method doin anything?*/

    }
}
package com.example.multiphidgets;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.os.Bundle;
import android.widget.TextView;

import com.phidget22.*;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mSensor;
    TextView simpleTextView;

    LCD lcd0;

    ArrayList<String> choices = new ArrayList<>();
    Random rand = new Random();

    int threshold = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        choices.add("Lemons");
        choices.add("Apples");
        choices.add("Bananas");
        choices.add("Grapes");
        choices.add("Limes");
        choices.add("Pears");

        simpleTextView = (TextView) findViewById(R.id.simpleTextView); //get the id for TextView

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        try {
            //Allow direct USB connection of Phidgets
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
                com.phidget22.usb.Manager.Initialize(this);

            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

            Net.addServer("", "192.168.56.1", 5661, "", 0);

            lcd0 = new LCD();
            lcd0.setChannel(0);
            lcd0.setDeviceSerialNumber(329998);
            lcd0.open(5000);
            lcd0.setBacklight(0.5);
            lcd0.setContrast(0.5);
            lcd0.setScreenSize(LCDScreenSize.DIMENSIONS_2X16);
            lcd0.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, "text");
            lcd0.flush();

        } catch (PhidgetException pe) {
            pe.printStackTrace();
        }
    }

    public static AttachListener onCh_Attach =
            new AttachListener() {
                @Override
                public void onAttach(AttachEvent e) {
                    Log.d("Attach Listener", e.toString());
                }
            };

    public static DetachListener onCh_Detach =
            new DetachListener() {
                @Override
                public void onDetach(DetachEvent e) {
                    Log.d("Detach Listener", e.toString());
                }
            };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        mSensorManager.registerListener((SensorEventListener)this, mSensor, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener((SensorEventListener)this, mSensor);
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {

        String output = "X= " + String.valueOf(event.values[0]) + "Y= " + String.valueOf(event.values[1])+ "Z= " + String.valueOf(event.values[2]);
        simpleTextView.setText(output);

        if ((Math.abs(event.values[0]) > threshold) || (Math.abs(event.values[1]) > threshold) || (Math.abs(event.values[2]) > threshold)) {

            String choice = choices.get(rand.nextInt(choices.size()));

            System.out.println(choice);

            try {
                lcd0.clear();
                lcd0.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, choice);
                lcd0.flush();
            } catch (PhidgetException e) {
                e.printStackTrace();
            }


        }



        // Update Screen
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {

            lcd0.close();

            Log.d("onDestroy: ", "Closed channels.");
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }
}
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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.phidget22.*;

import java.text.DecimalFormat;

public class MainActivity extends Activity implements SensorEventListener {

    VoltageRatioInput voltageRatioInput0;
    RCServo rcServo0;
    DigitalOutput digitalOutput0;

    SensorManager mSensorManager;
    Sensor mAccel;
    Sensor mGyro;
    long timestamp = 0;
    float angle = 0;
    float filteredAngle = 0;
    float[] accel = new float[3];
    float pitch = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;

    TextView lTextView, xTextView, yTextView, zTextView, aTextView, vTextView, pTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        xTextView = (TextView) findViewById(R.id.xTextView); //get the id for TextView
        yTextView = (TextView) findViewById(R.id.yTextView); //get the id for TextView
        zTextView = (TextView) findViewById(R.id.zTextView); //get the id for TextView
        aTextView = (TextView) findViewById(R.id.aTextView); //get the id for TextView
        vTextView = (TextView) findViewById(R.id.vTextView); //get the id for TextView
        pTextView = (TextView) findViewById(R.id.pTextView); //get the id for TextView

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        try {
            //Allow direct USB connection of Phidgets
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
                com.phidget22.usb.Manager.Initialize(this);

            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

//            Net.addServer("", "137.44.181.66", 5661, "", 0);
            Net.addServer("", "192.168.1.43", 5661, "", 0);

            //Create your Phidget channels
            voltageRatioInput0 = new VoltageRatioInput();
            rcServo0 = new RCServo();
            digitalOutput0 = new DigitalOutput();

            //Set addressing parameters to specify which channel to open (if any)
            voltageRatioInput0.setIsHubPortDevice(true);
            voltageRatioInput0.setDeviceSerialNumber(626673);
//            voltageRatioInput0.setChannel(0);
            voltageRatioInput0.setHubPort(0);
            //Set the sensor type to match the analog sensor you are using after opening the Phidget
//            voltageRatioInput0.setSensorType(VoltageRatioSensorType.PN_1128);
            rcServo0.setDeviceSerialNumber(14379);
            digitalOutput0.setIsHubPortDevice(true);
            digitalOutput0.setDeviceSerialNumber(626673);
            digitalOutput0.setHubPort(5);
//            digitalOutput0.setChannel(0);
//            digitalOutput0.setIsRemote(true);

            voltageRatioInput0.addAttachListener(onCh_Attach);
            voltageRatioInput0.addDetachListener(onCh_Detach);
//            voltageRatioInput0.addVoltageRatioChangeListener(onCh_VoltageRatioChange);
            voltageRatioInput0.addVoltageRatioChangeListener(new VoltageRatioInputVoltageRatioChangeListener() {
                public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent) {
                    VoltageRatioInputVoltageRatioChangeEventHandler handler = new VoltageRatioInputVoltageRatioChangeEventHandler(voltageRatioInput0, voltageRatioChangeEvent);
                    runOnUiThread(handler);
                }
            });

            rcServo0.addAttachListener(onCh_Attach);
            rcServo0.addDetachListener(onCh_Detach);
            rcServo0.addPositionChangeListener(onCh_PositionChange);
/*            rcServo0.addPositionChangeListener(new RCServoPositionChangeListener() {
                public void onPositionChange(RCServoPositionChangeEvent positionChangeEvent) {
                    RCServoPositionChangeEventHandler handler = new RCServoPositionChangeEventHandler(rcServo0, positionChangeEvent);
                    runOnUiThread(handler);
                }
            });*/

            digitalOutput0.addAttachListener(onCh_Attach);
            digitalOutput0.addDetachListener(onCh_Detach);

            voltageRatioInput0.open(5000);
            rcServo0.open(5000);
            digitalOutput0.open(5000);
        } catch (PhidgetException pe) {
            pe.printStackTrace();
        }
    }

/*    public VoltageRatioInputVoltageRatioChangeListener onCh_VoltageRatioChange =
            new VoltageRatioInputVoltageRatioChangeListener() {
                @Override
                public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent e) {
//                    Log.d("Voltage Ratio Value: ", String.valueOf(e.getVoltageRatio()));
                    try {
                        digitalOutput0.setDutyCycle(e.getVoltageRatio());
                    } catch (PhidgetException phidgetException) {
                        phidgetException.printStackTrace();
                    }
                }
            };*/

    class VoltageRatioInputVoltageRatioChangeEventHandler implements Runnable {
        Phidget ch;
        VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent;

        public VoltageRatioInputVoltageRatioChangeEventHandler(Phidget ch, VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent) {
            this.ch = ch;
            this.voltageRatioChangeEvent = voltageRatioChangeEvent;
        }

        public void run() {
            vTextView.setText("V_in = " + String.valueOf(String.format("%.2f",voltageRatioChangeEvent.getVoltageRatio())) + " V/V");
            try {
                digitalOutput0.setDutyCycle(voltageRatioChangeEvent.getVoltageRatio());
            } catch (PhidgetException phidgetException) {
                phidgetException.printStackTrace();
            }
        }
    }

/*    public RCServoPositionChangeListener onCh_PositionChange =
            new RCServoPositionChangeListener() {
                @Override
                public void onPositionChange(RCServoPositionChangeEvent e) {
                    Log.d("RCServo Position: ", String.valueOf(e.getPosition()));
                }
            };*/

    public RCServoPositionChangeListener onCh_PositionChange =
                new RCServoPositionChangeListener() {
        public void onPositionChange(RCServoPositionChangeEvent positionChangeEvent) {
            RCServoPositionChangeEventHandler handler = new RCServoPositionChangeEventHandler(rcServo0, positionChangeEvent);
            runOnUiThread(handler);
        }
    };

    class RCServoPositionChangeEventHandler implements Runnable {
        Phidget ch;
        RCServoPositionChangeEvent positionChangeEvent;

        public RCServoPositionChangeEventHandler(Phidget ch, RCServoPositionChangeEvent positionChangeEvent) {
            this.ch = ch;
            this.positionChangeEvent = positionChangeEvent;
        }

        public void run() {
            pTextView.setText("Servo position = " + String.valueOf(String.format("%.1f",positionChangeEvent.getPosition())) + " Degrees");
        }
    }

    public static AttachListener onCh_Attach =
            new AttachListener() {
                @Override
                public void onAttach(AttachEvent e) {
//                    Log.d("Attach Listener", e.toString());
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
        mSensorManager.registerListener((SensorEventListener)this, mAccel, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener((SensorEventListener)this, mGyro, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mSensorManager.registerListener((SensorEventListener)this, mAccel, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener((SensorEventListener)this, mGyro, SensorManager.SENSOR_DELAY_UI);
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            xTextView.setText("X = " + String.valueOf(String.format("%.3f",event.values[0])));
            yTextView.setText("Y = " + String.valueOf(String.format("%.3f",event.values[1])));
            zTextView.setText("Z = " + String.valueOf(String.format("%.3f",event.values[2])));
            aTextView.setText("A = " + String.valueOf(String.format("%.3f",event.values[3])));
//            try {
//                rcServo0.setTargetPosition(90 + event.values[1]*100);
//                rcServo0.setEngaged(true);
////                pTextView.setText("Servo position = " + String.valueOf(String.format("%.2f",rcServo0.getPosition())) + " Degrees");
//            } catch (PhidgetException phidgetException) {
//                phidgetException.printStackTrace();
//            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accel[0] = event.values[0];
            accel[1] = event.values[1];
            accel[2] = event.values[2];
            pitch = (float) Math.toDegrees(Math.atan2(accel[1], Math.sqrt(Math.pow(accel[2], 2) + Math.pow(accel[0], 2))));

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float xGyro = event.values[0];

            if (timestamp != 0 ) {
                final float dT = (event.timestamp - this.timestamp) * NS2S;
                angle += xGyro * dT;
                filteredAngle = (0.98f * (filteredAngle + (xGyro * dT))) + (0.02f * pitch);
                float normalisedAngle = (filteredAngle - (-90)) * (180) / (90 - (-90)) + 0;

//                try {
//                    rcServo0.setTargetPosition(filteredAngle);
//                    rcServo0.setEngaged(true);
                    pTextView.setText("Servo position = " + normalisedAngle + " Degrees");
//                } catch (PhidgetException phidgetException) {
//                    phidgetException.printStackTrace();
//                }
            }

            timestamp = event.timestamp;
        }
    }
        @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //Close your Phidgets once the program is done.
            voltageRatioInput0.close();
            rcServo0.close();
            digitalOutput0.close();
            Log.d("onDestroy: ", "Closed channels.");
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }
}
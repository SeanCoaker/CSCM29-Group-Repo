package com.example.multiphidgets;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;

import com.phidget22.*;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    VoltageRatioInput voltageRatioInput0;
    LCD lcd0;

    ArrayList<String> choices = new ArrayList<>();
    Random rand = new Random();

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

        try {
            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

            Net.addServer("", "192.168.50.168", 5661, "", 0);

            //Create your Phidget channels
            voltageRatioInput0 = new VoltageRatioInput();
            lcd0 = new LCD();

            //Set addressing parameters to specify which channel to open (if any)

            voltageRatioInput0.setChannel(0);
            voltageRatioInput0.setDeviceSerialNumber(29773);
            lcd0.setDeviceSerialNumber(29773);

            //Set the sensor type to match the analog sensor you are using after opening the Phidget
//            voltageRatioInput0.setSensorType(VoltageRatioSensorType.PN_1128);

            voltageRatioInput0.addAttachListener(onCh_Attach);
            voltageRatioInput0.addDetachListener(onCh_Detach);
            voltageRatioInput0.addVoltageRatioChangeListener(onCh_VoltageRatioChange);

            lcd0.addAttachListener(onCh_Attach);
            lcd0.addDetachListener(onCh_Detach);

            voltageRatioInput0.open(5000);
            lcd0.open(5000);

            voltageRatioInput0.setVoltageRatioChangeTrigger(0.1);
            lcd0.setBacklight(0.5);
            lcd0.setContrast(0.5);

        } catch (PhidgetException pe) {
            pe.printStackTrace();
        }
    }

    public VoltageRatioInputVoltageRatioChangeListener onCh_VoltageRatioChange =
            new VoltageRatioInputVoltageRatioChangeListener() {
                @Override
                public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent e) {
                    Log.d("Voltage Ratio Value: ", String.valueOf(e.getVoltageRatio()));
//                    Log.d("Voltage Ratio Value: ", e.toString());
                    String choice = choices.get(rand.nextInt(choices.size()));
                    Log.d("Choice: ", choice);

                    try {
                        lcd0.clear();
                        lcd0.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, choice);
                        lcd0.flush();
                    } catch (PhidgetException phidgetException) {
                        phidgetException.printStackTrace();
                    }
//                        Thread.sleep(100);
//                        rcServo0.setEngaged(false);
                }
            };

//    public static RCServoPositionChangeListener onCh_PositionChange =
//            new RCServoPositionChangeListener() {
//                @Override
//                public void onPositionChange(RCServoPositionChangeEvent e) {
//                    Log.d("RCServo Position: ", String.valueOf(e.getPosition()));
//                }
//            };

    public AttachListener onCh_Attach =
            new AttachListener() {
                @Override
                public void onAttach(AttachEvent e) {
                    Log.d("Attach Listener", e.toString());
                }
            };

    public DetachListener onCh_Detach =
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
    protected void onDestroy() {
        super.onDestroy();
        try {
            //Close your Phidgets once the program is done.
            voltageRatioInput0.close();
            lcd0.close();
            Log.d("onDestroy: ", "Closed channels.");
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }
}
package com.example.demoapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.demoapp.ui.main.MainFragment;
import com.phidget22.*;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private VoltageRatioInput voltageRatioInput0;
    private LCD lcd0;
    private Random rand = new Random();
    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private ArrayList<String> choices = choicesSingleton.getChoices();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance(this))
                    .commitNow();
        }

        try {
            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

            Net.addServer("", "192.168.1.203", 5661, "", 0);

            //Create your Phidget channels
            voltageRatioInput0 = new VoltageRatioInput();
            lcd0 = new LCD();

            //Set addressing parameters to specify which channel to open (if any)

            voltageRatioInput0.setChannel(0);
            voltageRatioInput0.setDeviceSerialNumber(29773);
            lcd0.setDeviceSerialNumber(29773);

            voltageRatioInput0.addAttachListener(onCh_Attach);
            voltageRatioInput0.addDetachListener(onCh_Detach);
            voltageRatioInput0.addVoltageRatioChangeListener(onCh_VoltageRatioChange);

            lcd0.addAttachListener(onCh_Attach);
            lcd0.addDetachListener(onCh_Detach);

            voltageRatioInput0.open(5000);
            lcd0.open(5000);

            voltageRatioInput0.setVoltageRatioChangeTrigger(0.35);
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

                    if (choices.size() > 0 && e.getVoltageRatio() > 0.7) {
                        Log.d("Voltage Ratio Value: ", String.valueOf(e.getVoltageRatio()));
                        String choice = choices.get(rand.nextInt(choices.size()));
                        Log.d("Choice: ", choice);

                        try {
                            lcd0.clear();
                            lcd0.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, choice);
                            lcd0.flush();
                        } catch (PhidgetException phidgetException) {
                            phidgetException.printStackTrace();
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                }
            };

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
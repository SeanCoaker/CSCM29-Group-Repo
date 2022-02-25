package com.example.demoapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.demoapp.ui.main.MainFragment;
import com.phidget22.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private VoltageRatioInput voltageRatioInput0;
    private LCD lcd0;
    private Random rand = new Random();
    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private ArrayList<String> choices = choicesSingleton.getChoices();

    Spatial spatial0; // Accelerometer
    int currentSideUp = 0; // Current dice side facing up - (1-6)
    int error = 30; // Maximum angle error in angle calculation
    int maxChar = 16; // Screen size in number of characters
    int maxRolls = 18; // Maximum number of rolls in auto roll

    ArrayList<String> diceSides = new ArrayList<>(6); // Screen output text

    // Used to determine the current mode the dice is in
    public enum OperatingMode {
        INACTIVE,
        SIXSIDENORMAL,
        SIXSIDEEXTENDED,
    }

    OperatingMode operatingMode; // Current dice mode
    int randomChoiceIndex; // Stores the random choice made

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        init();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance(this))
                    .commitNow();
        }

        try {

            //Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

            Net.addServer("", "172.25.160.1", 5661, "", 0);

            //Create your Phidget channels

            spatial0 = new Spatial();

            spatial0.addSpatialDataListener(new SpatialSpatialDataListener() {
                public void onSpatialData(SpatialSpatialDataEvent e) {

                    //System.out.println("Acceleration: \t"+ e.getAcceleration()[0]+ "  |  "+ e.getAcceleration()[1]+ "  |  "+ e.getAcceleration()[2]);
                    //System.out.println("AngularRate: \t"+ e.getAngularRate()[0]+ "  |  "+ e.getAngularRate()[1]+ "  |  "+ e.getAngularRate()[2]);
                    //System.out.println("MagneticField: \t"+ e.getMagneticField()[0]+ "  |  "+ e.getMagneticField()[1]+ "  |  "+ e.getMagneticField()[2]);
                    //System.out.println("Timestamp: " + e.getTimestamp());
                    //System.out.println("----------");

                    double xAngle = calculateAngleX(e.getAcceleration()[0],e.getAcceleration()[1],e.getAcceleration()[2]);
                    double yAngle = calculateAngleY(e.getAcceleration()[0],e.getAcceleration()[1],e.getAcceleration()[2]);

                    System.out.println("X Angle:" + xAngle);
                    System.out.println("Y Angle:" + yAngle);
                    System.out.println("----------");
                    updateSideUp(xAngle,yAngle);
                    System.out.println("Top: " + currentSideUp);
                    System.out.println("----------");
                    System.out.println("Selected Choice: " + choices.get(randomChoiceIndex));
                    System.out.println("----------");


                    switch(operatingMode) {
                        case INACTIVE:
                            break;
                        case SIXSIDENORMAL:
                            updateDiceSixNormal();
                            break;
                        case SIXSIDEEXTENDED:
                            updateDiceSixExtended();

                    }

                    printDice();

                    updateDice();

                }
            });

            spatial0.open(5000);

        } catch (PhidgetException pe) {
            pe.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        MenuItem item = menu.findItem(R.id.autoRollSwitchMenu);
        item.setActionView(R.layout.switch_layout);

        SwitchCompat autoRollSwitch = item.getActionView().findViewById(R.id.autoRollSwitch);

        autoRollSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                System.out.println("Switch ON");
            } else {
                System.out.println("Switch OFF");
            }
        });
        return true;
    }


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
            spatial0.close();
            Log.d("onDestroy: ", "Closed channels.");
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public double calculateAngleX(double x,double y,double z) {

        //return (Math.atan(y / Math.sqrt(pow(x, 2) + pow(z, 2))) * 180 / Math.PI);
        return Math.atan2(y , z) * 57.3;
    }

    public double calculateAngleY(double x,double y,double z) {

        //return (Math.atan(-1 * x / Math.sqrt(pow(y, 2) + pow(z, 2))) * 180 / Math.PI);
        return Math.atan2((- x) , Math.sqrt(y * y + z * z)) * 57.3;
    }

    public void updateSideUp(double aX, double aY) {

        if ( ((aX >= (0-error))&&(aX <= (0+error))) && ((aY >= (0-error))&&(aY <= (0+error))) ) {

            currentSideUp = 1;

        } else if ( ((aX >= (-90-error))&&(aX <= (-90+error))) && ((aY >= (0-error))&&(aY <= (0+error))) ) {

            currentSideUp = 2;

        } else if ( ((aX >= (-180-error))&&(aX <= (-180+error))) && ((aY >= (-90-error))&&(aY <= (-90+error))) ) {

            currentSideUp = 3;

        } else if ( ((aX >= (180-error))&&(aX <= (180+error))) && ((aY >= (0-error))&&(aY <= (0+error))) ) {

            currentSideUp = 4;

        } else if ( ((aX >= (180-error))&&(aX <= (180+error))) && ((aY >= (90-error))&&(aY <= (90+error))) ) {

            currentSideUp = 5;

        } else if ( ((aX >= (90-error))&&(aX <= (90+error))) && ((aY >= (0-error))&&(aY <= (0+error))) ) {

            currentSideUp = 6;

        } else {

        }

    }

    public String addPadding(String string, int maxChar) {

        return String.format("%-" + maxChar  + "s", String.format("%" + (string.length() + (maxChar - string.length()) / 2) + "s", string));

    }

    public String getSide(int side) {

        return diceSides.get(side-1);
    }

    public void printDice() {

        char[] arr = new char[maxChar];
        Arrays.fill(arr, ' ');
        String space = new String(arr);

        System.out.println("");
        System.out.println(" " + space + "|" + addPadding(getSide(6),maxChar) + "|" + space + " " + space + " ");
        System.out.println("|" + addPadding(getSide(3),maxChar) + "|" + addPadding(getSide(1),maxChar) + "|" + addPadding(getSide(5),maxChar) + "|" + addPadding(getSide(4),maxChar) + "|");
        System.out.println(" " + space + "|" + addPadding(getSide(2),maxChar) + "|" + space + " " + space + " ");
        System.out.println("");

    }

    public void setRandomChoice() {

        randomChoiceIndex = (int)(Math.random() * ((choices.size()) + 1));

    }

    public void setDiceSide(int choiceIndex, int sideNum) {

        diceSides.add(sideNum - 1,choices.get(choiceIndex));

    }

    public void setDiceSides() {

        for (int i=0; i < 6; i++) {

            diceSides.add(i,choices.get(i));

        }

    }

    public void setDiceSides(ArrayList<Integer> indexes) {

        for (int i=0; i < indexes.size(); i++) {

            diceSides.add(i,choices.get(i));

        }

    }

    public void updateDiceSixNormal () {

        setDiceSides();
        updateDice();


    }

    public void updateDiceSixExtended () {

        ArrayList fillingIndexes = new ArrayList<>();

        while (fillingIndexes.size() < 6) {

            int tempchoice = 0 + (int)(Math.random() * ((5) + 1));

            if (tempchoice != randomChoiceIndex) {

                fillingIndexes.add(tempchoice);
            }
        }

        setDiceSides(fillingIndexes);

        setDiceSide(randomChoiceIndex,currentSideUp);

        updateDice();

    }

    public void updateDice() {

        // Update Screens

    }

    public void autoRoll() {

        int sideToStopOn = (int)(Math.random() * ((5) + 1));

        // Generate a factor of 6 within the max rolls - could be 5 depending on whats selected - ie 1 doesnt need moving

        // Loop For that amount - activating the respective servo

    }

    public void init() {

        diceSides.add(addPadding("One", maxChar));
        diceSides.add(addPadding("Two", maxChar));
        diceSides.add(addPadding("Three", maxChar));
        diceSides.add(addPadding("Four", maxChar));
        diceSides.add(addPadding("Five", maxChar));
        diceSides.add(addPadding("Six", maxChar));

        operatingMode = OperatingMode.INACTIVE;

        if (choices.isEmpty()) return;
        setRandomChoice();

    }

    public void setOperatingMode(OperatingMode op) {
        this.operatingMode = op;
    }
}
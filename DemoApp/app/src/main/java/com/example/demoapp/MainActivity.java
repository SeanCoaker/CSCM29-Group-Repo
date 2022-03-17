package com.example.demoapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.demoapp.ui.main.MainFragment;
import com.phidget22.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    /*
     / Phidgets Devices
    */

    VoltageRatioInput voltageRatioInput0;

    RCServo rcServo0;
    RCServo rcServo1;
    RCServo rcServo2;
    RCServo rcServo3;

    LCD lcd0;
    LCD lcd1;
    LCD lcd2;

    Spatial spatial0; // Accelerometer

    RCServo servos[];

    /*
     / App Global Variables
    */

    private Random rand = new Random();
    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private ArrayList<String> choices = choicesSingleton.getChoices();

    int currentSideUp = 1; // Current dice side facing up - (1-6)
    int lastSideUp = 1;
    int error = 30; // Maximum angle error in angle calculation
    int maxChar = 20; // Screen size in number of characters
    int maxRolls = 5; // Maximum number of rolls in auto roll

    Boolean screenOn = true;

    ArrayList<String> diceSides = new ArrayList<>(6); // Screen output text

    Boolean autoRollEn = false;

    // Used to determine the current mode the dice is in
    public enum OperatingMode {
        INACTIVE,
        SIXSIDENORMAL,
        SIXSIDEEXTENDED,
    }

    OperatingMode operatingMode = OperatingMode.INACTIVE; // Current dice mode
    OperatingMode lastOperatingMode = OperatingMode.INACTIVE;

    int randomChoiceIndex = 0; // Stores the random choice made


    /*
     / On Create
    */
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

            Net.addServer("", "172.21.144.1", 5661, "", 0);

            //Create your Phidget channels

            spatial0 = new Spatial();

            rcServo0 = new RCServo();
            rcServo1 = new RCServo();
            rcServo2 = new RCServo();
            rcServo3 = new RCServo();

            rcServo0.setChannel(0);
            rcServo1.setChannel(1);
            rcServo2.setChannel(2);
            rcServo3.setChannel(3);

            lcd0 = new LCD();
            lcd0.setChannel(0);
            lcd0.setDeviceSerialNumber(30683);
            lcd0.open(5000);
            lcd0.setBacklight(0.5);
            lcd0.setContrast(0.5);
            lcd0.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-3",maxChar));
            lcd0.flush();

            lcd1 = new LCD();
            lcd1.setChannel(0);
            lcd1.setDeviceSerialNumber(30679);
            lcd1.open(5000);
            lcd1.setBacklight(0.5);
            lcd1.setContrast(0.5);
            lcd1.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-1",maxChar));
            lcd1.flush();

            lcd2 = new LCD();
            lcd2.setChannel(0);
            lcd2.setDeviceSerialNumber(29773);
            lcd2.open(5000);
            lcd2.setBacklight(0.5);
            lcd2.setContrast(0.5);
            lcd2.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-5",maxChar));
            lcd2.flush();

            spatial0.addSpatialDataListener(new SpatialSpatialDataListener() {
                public void onSpatialData(SpatialSpatialDataEvent e) {

                    //System.out.println("Acceleration: \t"+ e.getAcceleration()[0]+ "  |  "+ e.getAcceleration()[1]+ "  |  "+ e.getAcceleration()[2]);
                    //System.out.println("AngularRate: \t"+ e.getAngularRate()[0]+ "  |  "+ e.getAngularRate()[1]+ "  |  "+ e.getAngularRate()[2]);
                    //System.out.println("MagneticField: \t"+ e.getMagneticField()[0]+ "  |  "+ e.getMagneticField()[1]+ "  |  "+ e.getMagneticField()[2]);
                    //System.out.println("Timestamp: " + e.getTimestamp());
                    //System.out.println("----------");

                    double xAngle = calculateAngleX(e.getAcceleration()[0],e.getAcceleration()[1],e.getAcceleration()[2]);
                    double yAngle = calculateAngleY(e.getAcceleration()[0],e.getAcceleration()[1],e.getAcceleration()[2]);

                    //System.out.println("X Angle:" + xAngle);
                    //System.out.println("Y Angle:" + yAngle);

                    updateSideUp(xAngle,yAngle);

                    if (autoRollEn == false) {

                        System.out.println("-----------------------------------");
                        System.out.println("Current Top: " + currentSideUp);
                        System.out.println("-----------------------------------");
                        System.out.println("Operating Mode: " + operatingMode);
                        System.out.println("-----------------------------------");
                        if (!choices.isEmpty()){

                            System.out.println("Current Random Choice: " + choices.get(randomChoiceIndex));

                        }

                    }

                    switch(operatingMode) {
                        case INACTIVE:
                            try {
                                turnOffScreens();
                            } catch (PhidgetException phidgetException) {
                                phidgetException.printStackTrace();
                            }
                            break;
                        case SIXSIDENORMAL:
                            try {
                                lastOperatingMode = OperatingMode.SIXSIDENORMAL;
                                turnOnScreens();
                                updateDiceSixNormal();
                            } catch (PhidgetException phidgetException) {
                                phidgetException.printStackTrace();
                            }
                            break;
                        case SIXSIDEEXTENDED:
                            try {
                                lastOperatingMode = OperatingMode.SIXSIDEEXTENDED;
                                turnOnScreens();
                                updateDiceSixExtended();
                            } catch (PhidgetException phidgetException) {
                                phidgetException.printStackTrace();
                            }

                    }

                    if (autoRollEn == false) {
                        printDice();
                    }

                }
            });

            spatial0.open(5000);

            rcServo0.open(5000);
            rcServo1.open(5000);
            rcServo2.open(5000);
            rcServo3.open(5000);

            servos = new RCServo[4];
            servos[0] = rcServo0;
            servos[1] = rcServo1;
            servos[2] = rcServo2;
            servos[3] = rcServo3;

            screenOn = true;

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

                System.out.println("-----------------------------------");
                System.out.println("------------Switch ON--------------");
                System.out.println("-----------------------------------");

                try {
                    autoRoll();
                } catch (PhidgetException | InterruptedException e) {
                    e.printStackTrace();
                }

            } else {

                System.out.println("-----------------------------------");
                System.out.println("-----------Switch OFF--------------");
                System.out.println("-----------------------------------");
            }
        });
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {

            spatial0.close();

            lcd0.close();
            lcd1.close();
            lcd2.close();

            rcServo0.close();
            rcServo1.close();
            rcServo2.close();
            rcServo3.close();


            Log.d("onDestroy: ", "Closed channels.");

        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        System.out.println("-----------------------------------");
        System.out.println("--------onResume Activated---------");
        System.out.println("-----------------------------------");

        System.out.println("Last Operating Mode: " + lastOperatingMode);

        operatingMode = lastOperatingMode;

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        System.out.println("-----------------------------------");
        System.out.println("--------onRestart Activated--------");
        System.out.println("-----------------------------------");

        System.out.println("Last Operating Mode: " + lastOperatingMode);

        operatingMode = lastOperatingMode;

    }

    @Override
    protected void onPause() {
        super.onPause();

        System.out.println("-----------------------------------");
        System.out.println("---------onPause Activated---------");
        System.out.println("-----------------------------------");

        operatingMode = OperatingMode.INACTIVE;

        System.out.println("Last Operating Mode: " + lastOperatingMode);

    }
    @Override
    protected void onStop() {
        super.onStop();

        System.out.println("-----------------------------------");
        System.out.println("---------onStop Activated----------");
        System.out.println("-----------------------------------");

        operatingMode = OperatingMode.INACTIVE;

        System.out.println("Last Operating Mode: " + lastOperatingMode);

    }



    public double calculateAngleX(double x,double y,double z) {

        return Math.atan2(y , z) * 57.3;
    }

    public double calculateAngleY(double x,double y,double z) {

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

        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println("");
        System.out.println(" " + space + "|" + addPadding(getSide(6),maxChar) + "|" + space + " " + space + " ");
        System.out.println("|" + addPadding(getSide(3),maxChar) + "|" + addPadding(getSide(1),maxChar) + "|" + addPadding(getSide(5),maxChar) + "|" + addPadding(getSide(4),maxChar) + "|");
        System.out.println(" " + space + "|" + addPadding(getSide(2),maxChar) + "|" + space + " " + space + " ");
        System.out.println("");
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println("");

    }

    public void setRandomChoice() {

        randomChoiceIndex = (int)(Math.random() * ((choices.size()-1) + 1));

    }

    public void setDiceSide(int choiceIndex, int sideNum) {

        diceSides.set(sideNum - 1,choices.get(choiceIndex));

    }

    public void setDiceSides() {

        for (int i=0; i < 6; i++) {

            diceSides.set(i,choices.get(i));

        }

    }

    public void setDiceSides(ArrayList<Integer> indexes) {

        for (int i=0; i < indexes.size(); i++) {

            diceSides.set(i,choices.get(indexes.get(i)));

        }

    }

    public void updateDiceSixNormal () throws PhidgetException {

        setDiceSides();

        if (lastSideUp != currentSideUp) {

            updateDice();

        }

        lastSideUp = currentSideUp;


    }

    public void updateDiceSixExtended () throws PhidgetException {

        ArrayList fillingIndexes = new ArrayList<>(6);

        while (fillingIndexes.size() < 6) {

            int tempChoice = 0 + (int)(Math.random() * ((choices.size()-1 - 0) + 1));


            if (tempChoice != randomChoiceIndex && !fillingIndexes.contains(tempChoice)) {

                fillingIndexes.add(tempChoice);
            }
        }

        setDiceSides(fillingIndexes);

        setDiceSide(randomChoiceIndex,currentSideUp);

        updateDice();

        if (lastSideUp != currentSideUp) {

            System.out.println("-----------------------------------");
            System.out.println(choices.get(randomChoiceIndex));
            System.out.println("-----------------------------------");
            System.out.println("");

            updateDice();

        }

        lastSideUp = currentSideUp;

    }

    public void updateDice() throws PhidgetException {
        if (!autoRollEn) {

            System.out.println("-----------------------------------");
            System.out.println("----------Screens Update-----------");
            System.out.println("-----------------------------------");
            System.out.println("");
        }

        lcd0.clear();
        lcd0.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(diceSides.get(2),maxChar));
        lcd0.flush();

        lcd1.clear();
        lcd1.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(diceSides.get(0),maxChar));
        lcd1.flush();

        lcd2.clear();
        lcd2.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(diceSides.get(4),maxChar));
        lcd2.flush();

    }

    public void turnOffScreens() throws PhidgetException {

        if (screenOn == true) {

            lcd0.setBacklight(0);
            lcd1.setBacklight(0);
            lcd2.setBacklight(0);

            screenOn = false;

        }

    }

    public void turnOnScreens() throws PhidgetException {

        if (screenOn == false) {

            lcd0.setBacklight(0.5);
            lcd1.setBacklight(0.5);
            lcd2.setBacklight(0.5);

            screenOn = true;
        }

    }



    public void autoRoll() throws PhidgetException, InterruptedException {

        System.out.println("-----------------------------------");
        System.out.println("---------AutoRoll Activated--------");
        System.out.println("-----------------------------------");
        System.out.println("");

        autoRollEn = true;

        int sideToStopOn = 1 + (int)(Math.random() * ((maxRolls -1) + 1));

        System.out.println("-----------------------------------");
        System.out.println("Number of Rolls: " + sideToStopOn);
        System.out.println("-----------------------------------");
        System.out.println("");

        if (sideToStopOn != 1)  {

            int currentSide = 1;

            for (int i=1; i <= sideToStopOn; i++) {

                System.out.println("-----------------------------------");
                System.out.println("Current Side To Flip: " + currentSide);
                System.out.println("-----------------------------------");
                System.out.println("");


                servos[currentSide-1].setTargetPosition(0);
                servos[currentSide-1].setEngaged(true);
                Thread.sleep(1000);

                servos[currentSide-1].setTargetPosition(180);
                servos[currentSide-1].setEngaged(true);
                Thread.sleep(1000);


                servos[currentSide-1].setTargetPosition(0);
                servos[currentSide-1].setEngaged(true);
                Thread.sleep(1000);

                // Restart dice roll after all faces have been seen
                if (currentSide == 4) {
                    currentSide = 1;
                } else {
                    currentSide++;
                }

            }

        }

        System.out.println("-----------------------------------");
        System.out.println("---------AutoRoll Completed--------");
        System.out.println("-----------------------------------");
        System.out.println("Number of Rolls: " + sideToStopOn);
        System.out.println("-----------------------------------");
        System.out.println("Current Side : " + currentSideUp);
        System.out.println("-----------------------------------");
        System.out.println("");

        autoRollEn = false;

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
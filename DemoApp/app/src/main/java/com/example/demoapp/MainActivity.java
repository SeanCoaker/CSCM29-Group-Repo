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

    private Random rand = new Random();
    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private ArrayList<String> choices = choicesSingleton.getChoices();

    RCServo rcServo0;
    RCServo rcServo1;
    RCServo rcServo2;
    RCServo rcServo3;

    LCD lcd0;
    LCD lcd1;
    LCD lcd2;

    Spatial spatial0; // Accelerometer
    int currentSideUp = 0; // Current dice side facing up - (1-6)
    int error = 30; // Maximum angle error in angle calculation
    int maxChar = 16; // Screen size in number of characters

    int maxRolls = 5; // Maximum number of rolls in auto roll
    int movingSides = 4;

    ArrayList<String> diceSides = new ArrayList<>(6); // Screen output text
    RCServo servos[];
    Boolean autoRollEn = false;

    // Used to determine the current mode the dice is in
    public enum OperatingMode {
        INACTIVE,
        SIXSIDENORMAL,
        SIXSIDEEXTENDED,
    }

    OperatingMode operatingMode; // Current dice mode
    int randomChoiceIndex = 0; // Stores the random choice made

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

            lcd0 = new LCD();
            lcd1 = new LCD();
            lcd2 = new LCD();

            lcd0.setDeviceSerialNumber(30679);
            lcd1.setDeviceSerialNumber(30683);
            lcd0.setDeviceSerialNumber(29773);

            RCServo rcServo0 = new RCServo();
            RCServo rcServo1 = new RCServo();
            RCServo rcServo2 = new RCServo();
            RCServo rcServo3 = new RCServo();

            rcServo0.setChannel(0);
            rcServo1.setChannel(1);
            rcServo2.setChannel(2);
            rcServo3.setChannel(3);

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

                    if (autoRollEn == false) {

                        System.out.println("----------");
                        updateSideUp(xAngle,yAngle);
                        System.out.println("Top: " + currentSideUp);
                        System.out.println("----------");
                        System.out.println("----------");
                        System.out.println(operatingMode);
                        System.out.println(randomChoiceIndex);

                    }

                    switch(operatingMode) {
                        case INACTIVE:
                            break;
                        case SIXSIDENORMAL:
                            try {
                                updateDiceSixNormal();
                            } catch (PhidgetException phidgetException) {
                                phidgetException.printStackTrace();
                            }
                            break;
                        case SIXSIDEEXTENDED:
                            try {
                                updateDiceSixExtended();
                            } catch (PhidgetException phidgetException) {
                                phidgetException.printStackTrace();
                            }

                    }

                    if (autoRollEn == false) {
                        printDice();
                    }

                    // updateDice
                    //set data interval

                }
            });

            spatial0.open(5000);

            rcServo0.open(5000);
            rcServo1.open(5000);
            rcServo2.open(5000);
            rcServo3.open(5000);

            lcd0.open(5000);
            lcd1.open(5000);
            lcd2.open(5000);

            lcd0.setBacklight(0.5);
            lcd0.setContrast(0.5);

            lcd1.setBacklight(0.5);
            lcd1.setContrast(0.5);

            lcd2.setBacklight(0.5);
            lcd2.setContrast(0.5);

            servos = new RCServo[4];
            servos[0] = rcServo0;
            servos[1] = rcServo1;
            servos[2] = rcServo2;
            servos[3] = rcServo3;

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

                try {
                    autoRoll();
                } catch (PhidgetException | InterruptedException e) {
                    e.printStackTrace();
                }

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

            diceSides.set(i,choices.get(i));

        }

    }

    public void setDiceSides(ArrayList<Integer> indexes) {

        for (int i=0; i < indexes.size(); i++) {

            diceSides.set(i,choices.get(i));

        }

    }

    public void updateDiceSixNormal () throws PhidgetException {

        setDiceSides();
        updateDice();


    }

    public void updateDiceSixExtended () throws PhidgetException {

        ArrayList fillingIndexes = new ArrayList<>();

        while (fillingIndexes.size() < 6) {

            int tempchoice = 0 + (int)(Math.random() * ((5) + 1));

            if (tempchoice != randomChoiceIndex) {

                fillingIndexes.add(tempchoice);
            }
        }

        System.out.println(Arrays.toString(fillingIndexes.toArray()));

        setDiceSides(fillingIndexes);

        setDiceSide(randomChoiceIndex,currentSideUp);

        updateDice();

        System.out.println(choices.get(randomChoiceIndex));

    }

    public void updateDice() throws PhidgetException {

        lcd0.clear();
        lcd0.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, diceSides.get(0));
        lcd0.flush();

        lcd1.clear();
        lcd1.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, diceSides.get(1));
        lcd1.flush();

        lcd2.clear();
        lcd2.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, diceSides.get(2));
        lcd2.flush();

    }

    public void autoRoll() throws PhidgetException, InterruptedException {

        autoRollEn = true;

        int sideToStopOn = 1 + (int)(Math.random() * ((maxRolls -1) + 1));

        System.out.println("Roll Number" + sideToStopOn);

        if (sideToStopOn != 1)  {

            int currentSide = 1;

            for (int i=1; i <= sideToStopOn; i++) {

                servos[currentSide-1].setTargetPosition(0);
                servos[currentSide-1].setEngaged(true);


                Thread.sleep(1000);

                servos[currentSide-1].setTargetPosition(180);
                servos[currentSide-1].setEngaged(true);

                Thread.sleep(1000);

                servos[currentSide-1].setTargetPosition(0);
                servos[currentSide-1].setEngaged(true);

                Thread.sleep(1000);

                if (currentSide == 4) {

                    currentSide = 1;

                } else {

                    currentSide++;
                }


            }



        }

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
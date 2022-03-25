/**
 * Imports
 */

package com.example.demoapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.demoapp.ui.main.MainFragment;
import com.phidget22.*;

import java.util.ArrayList;
import java.util.Arrays;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * Main Activity Class
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Fragment childFragment;
    private MainFragment mainFragment;

    /**
     * Phidget Devices
     */

    RCServo rcServoSide1;
    RCServo rcServoSide2;
    RCServo rcServoSide3;
    RCServo servos[];

    LCD lcdSide1;
    LCD lcdSide2;
    LCD lcdSide3;
    LCD lcdSide4;
    LCD lcdSide5;
    LCD lcdSide6;

    Spatial spatial0; // Accelerometer inside dice
    Spatial spatial1; // External Accelerometer

    /**
     * App Global Variables
     */

    private ChoicesSingleton choicesSingleton = ChoicesSingleton.getInstance();
    private ArrayList<String> choices = choicesSingleton.getChoices();

    private int currentSideUp = 1; // Current dice side facing up - (1-6)
    private int lastSideUp = 1; // Side facing up prior to current side up
    private int error = 30; // Maximum angle error in angle calculation
    private int maxChar = 16; // Screen size in number of characters
    private int maxRolls = 5; // Maximum number of rolls in auto roll
    private int accelerometerThreshold = 15; // Threshold value for accelerometer required to detect shake
    private int numOfSidesActive = 3; // Used for limiting number of servos in auto roll

    Boolean isScreensOn = true; // If screens are currently on
    Boolean isAutoRollEngaged = false; // If auto roll has been activated

    ArrayList<String> diceSides = new ArrayList<>(6); // Screen output texts

    // Used to determine the current mode the dice is in
    public enum OperatingMode {
        INACTIVE,
        SIXSIDENORMAL,
        SIXSIDEEXTENDED,
    }

    OperatingMode operatingMode = OperatingMode.INACTIVE; // Current dice operating mode
    OperatingMode lastOperatingMode = OperatingMode.INACTIVE; // Previous dice operating mode

    int randomChoiceIndex = 0; // Stores the random choice of option

    LCDScreenSize screenSize = LCDScreenSize.DIMENSIONS_2X16; // Screen size for all screens

    SensorManager mSensorManager; // Android sensor manager
    Sensor mSensor; // Android accelerometer sensor

    long autoRollCoolDown;
    long coolDownTime = 10000;
    int notificationLength = 3000;

    Boolean isRolled = false;
    int externalAccThreshold = 5;


    /**
     * On Create
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        init(); // Initialise the dice

        if (savedInstanceState == null) {
            childFragment = MainFragment.newInstance(this);
            mainFragment = (MainFragment) childFragment;

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, childFragment)
                    .commitNow();
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // Initialise sensor manager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // Initialise accelerometer

        try {

            // Enable server discovery to list remote Phidgets
            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
            Net.addServer("", "172.26.32.1", 5661, "", 0);

            /**
             * Instantiate Phidgets
             */

            // Spatial Phidget Setup
            spatial0 = new Spatial();
            spatial0.setDeviceSerialNumber(619527);
            spatial1 = new Spatial();
            spatial1.setDeviceSerialNumber(620776);


            // Servos Setup
            rcServoSide1 = new RCServo();
            rcServoSide2 = new RCServo();
            rcServoSide3 = new RCServo();

            rcServoSide1.setChannel(1);
            rcServoSide2.setChannel(2);
            rcServoSide3.setChannel(3);


            // LCD Side 3 Setup
            lcdSide3 = new LCD();
            lcdSide3.setChannel(0);
            lcdSide3.setDeviceSerialNumber(331245);
            lcdSide3.open(5000);
            lcdSide3.setBacklight(0.5);
            lcdSide3.setContrast(0.5);
            lcdSide3.setScreenSize(screenSize);
            lcdSide3.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-3",maxChar));
            lcdSide3.flush();


            // LCD Side 4 Setup
            lcdSide4 = new LCD();
            lcdSide4.setChannel(1);
            lcdSide4.setDeviceSerialNumber(331245);
            lcdSide4.open(5000);
            lcdSide4.setBacklight(0.5);
            lcdSide4.setContrast(0.5);
            lcdSide4.setScreenSize(screenSize);
            lcdSide4.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-4",maxChar));
            lcdSide4.flush();

            // LCD Side 2 Setup
            lcdSide2 = new LCD();
            lcdSide2.setChannel(0);
            lcdSide2.setDeviceSerialNumber(329830);
            lcdSide2.open(5000);
            lcdSide2.setBacklight(0.5);
            lcdSide2.setContrast(0.5);
            lcdSide2.setScreenSize(screenSize);
            lcdSide2.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-2",maxChar));
            lcdSide2.flush();

            // LCD Side 5 Setup
            lcdSide5 = new LCD();
            lcdSide5.setChannel(1);
            lcdSide5.setDeviceSerialNumber(329830);
            lcdSide5.open(5000);
            lcdSide5.setBacklight(0.5);
            lcdSide5.setContrast(0.5);
            lcdSide5.setScreenSize(screenSize);
            lcdSide5.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-5",maxChar));
            lcdSide5.flush();

            // LCD Side 1 Setup
            lcdSide1 = new LCD();
            lcdSide1.setChannel(0);
            lcdSide1.setDeviceSerialNumber(329998);
            lcdSide1.open(5000);
            lcdSide1.setBacklight(0.5);
            lcdSide1.setContrast(0.5);
            lcdSide1.setScreenSize(screenSize);
            lcdSide1.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-1",maxChar));
            lcdSide1.flush();


            // LCD Side 6 Setup
            lcdSide6 = new LCD();
            lcdSide6.setChannel(1);
            lcdSide6.setDeviceSerialNumber(329998);
            lcdSide6.open(5000);
            lcdSide6.setBacklight(0.5);
            lcdSide6.setContrast(0.5);
            lcdSide6.setScreenSize(screenSize);
            lcdSide6.writeText(com.phidget22.LCDFont.DIMENSIONS_5X8, 0, 0, addPadding("SIDE-6",maxChar));
            lcdSide6.flush();

            /**
             * Spatial Phidget Listener
             */
            spatial0.addSpatialDataListener(new SpatialSpatialDataListener() {
                public void onSpatialData(SpatialSpatialDataEvent e) {

                    double xAngle = calculateAngleX(e.getAcceleration()[0],e.getAcceleration()[1],e.getAcceleration()[2]);
                    double yAngle = calculateAngleY(e.getAcceleration()[0],e.getAcceleration()[1],e.getAcceleration()[2]);

                    updateSideUp(xAngle,yAngle);

                    if (isAutoRollEngaged == false) {

                        System.out.println("-----------------------------------");
                        System.out.println("Current Top: " + currentSideUp);
                        System.out.println("-----------------------------------");
                        System.out.println("Operating Mode: " + operatingMode);
                        System.out.println("-----------------------------------");
                        if (choices.size() > randomChoiceIndex+1){

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
                                setRandomChoice();
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

                    if (isAutoRollEngaged == false) {
                        printDice();
                    }

                }
            });

            spatial1.addSpatialDataListener(new SpatialSpatialDataListener() {
                public void onSpatialData(SpatialSpatialDataEvent e) {

                    if ((Math.abs(e.getAcceleration()[0]) > externalAccThreshold) || (Math.abs(e.getAcceleration()[1]) > externalAccThreshold) || (Math.abs(e.getAcceleration()[2]) > externalAccThreshold)) {

                        System.out.println("-----------------------------------");
                        System.out.println("-----------Roll Detected-----------");
                        System.out.println("-----------------------------------");

                        isRolled = true;

                    }

                }
            });


            spatial0.open(5000);
            spatial1.open(5000);


            rcServoSide1.open(5000);
            rcServoSide2.open(5000);
            rcServoSide3.open(5000);

            servos = new RCServo[6];
            servos[0] = rcServoSide1;
            servos[1] = rcServoSide2;
            servos[2] = rcServoSide3;

            isScreensOn = true;

        } catch (PhidgetException pe) {
            pe.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        MenuItem resetItem = menu.findItem(R.id.btnResetMenu);
        resetItem.setActionView(R.layout.reset_layout);
        MenuItem checkItem = menu.findItem(R.id.btnCheckMenu);
        checkItem.setActionView(R.layout.check_layout);

        AppCompatImageButton resetButton = resetItem.getActionView().findViewById(R.id.btnReset);
        AppCompatImageButton checkButton = checkItem.getActionView().findViewById(R.id.btnCheck);

        checkButton.setOnClickListener((button) -> {
            if (!mainFragment.isCardSelected()) return;
            System.out.println("CHECK ANSWER: " + choices.get(mainFragment.getSelectedIndex()));
            try {
                checkAnswer(mainFragment.getSelectedIndex());
            } catch (PhidgetException e) {
                e.printStackTrace();
            }
        });

        resetButton.setOnClickListener((button) -> {
            System.out.println("RESET");
            isRolled = false;
            mainFragment.clearRecycler();
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

            lcdSide1.close();
            lcdSide2.close();
            lcdSide3.close();
            lcdSide4.close();
            lcdSide5.close();
            lcdSide6.close();

            rcServoSide1.close();
            rcServoSide2.close();
            rcServoSide3.close();


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

        mSensorManager.registerListener((SensorEventListener)this, mSensor, SensorManager.SENSOR_DELAY_UI);

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

        mSensorManager.unregisterListener((SensorEventListener)this, mSensor);

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


    /**
     * Calculate x angle from accelerometer
     */
    public double calculateAngleX(double x,double y,double z) {

        return Math.atan2(y , z) * 57.3;
    }


    /**
     * Calculate y angle from accelerometer
     */
    public double calculateAngleY(double x,double y,double z) {

        return Math.atan2((- x) , Math.sqrt(y * y + z * z)) * 57.3;
    }


    /**
     * Calculate current side up using tilt angles from accelerometer data
     */
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


    /**
     * Adds padding to left and right side of a string to centre text on a screen
     */
    public String addPadding(String string, int maxChar) {

        return String.format("%-" + maxChar  + "s", String.format("%" + (string.length() + (maxChar - string.length()) / 2) + "s", string));

    }


    /**
     * Return side value
     */
    public String getSide(int side) {

        return diceSides.get(side-1);
    }

    /**
     * Prints the current state of text displayed on the dice
     */
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

    /**
     * Sets the random choice
     */
    public void setRandomChoice() {

        randomChoiceIndex = (int)(Math.random() * ((choices.size()-1) + 1));

    }

    /**
     * Sets side text
     */
    public void setDiceSide(int choiceIndex, int sideNum) {

        diceSides.set(sideNum - 1,choices.get(choiceIndex));

    }

    /**
     * Sets all sides text
     */
    public void setDiceSides() {

        for (int i=0; i < 6; i++) {

            diceSides.set(i,choices.get(i));

        }

    }

    /**
     * Sets all sides text
     */
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

        if (isRolled) {

            setDiceSide(randomChoiceIndex,currentSideUp);

            lastSideUp = currentSideUp;

            updateDice();

        } else {

            updateDice();
            lastSideUp = currentSideUp;
        }

    }

    public void updateDice() throws PhidgetException {
        if (!isAutoRollEngaged) {

            System.out.println("-----------------------------------");
            System.out.println("----------Screens Update-----------");
            System.out.println("-----------------------------------");
            System.out.println("");
        }

        lcdSide1.clear();
        lcdSide1.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(getSide(1), maxChar));
        lcdSide1.flush();

        lcdSide2.clear();
        lcdSide2.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(getSide(2), maxChar));
        lcdSide2.flush();

        lcdSide3.clear();
        lcdSide3.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(getSide(3), maxChar));
        lcdSide3.flush();

        lcdSide4.clear();
        lcdSide4.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(getSide(4), maxChar));
        lcdSide4.flush();

        lcdSide5.clear();
        lcdSide5.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(getSide(5), maxChar));
        lcdSide5.flush();

        lcdSide6.clear();
        lcdSide6.writeText(LCDFont.DIMENSIONS_5X8, 0, 0, addPadding(getSide(6), maxChar));
        lcdSide6.flush();

    }

    public void turnOffScreens() throws PhidgetException {

        if (isScreensOn == true) {

            // Turn screens off
            lcdSide1.setBacklight(0);
            lcdSide2.setBacklight(0);
            lcdSide3.setBacklight(0);
            lcdSide4.setBacklight(0);
            lcdSide5.setBacklight(0);
            lcdSide6.setBacklight(0);

            isScreensOn = false;

        }

    }

    public void turnOnScreens() throws PhidgetException {

        if (isScreensOn == false) {

            lcdSide1.setBacklight(0.5);
            lcdSide2.setBacklight(0.5);
            lcdSide3.setBacklight(0.5);
            lcdSide4.setBacklight(0.5);
            lcdSide5.setBacklight(0.5);
            lcdSide6.setBacklight(0.5);

            isScreensOn = true;
        }

    }


    public void autoRoll() throws PhidgetException, InterruptedException {

        isAutoRollEngaged = true;

        System.out.println("-----------------------------------");
        System.out.println("---------AutoRoll Activated--------");
        System.out.println("-----------------------------------");
        System.out.println("");


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

                servos[currentSide-1].setTargetPosition(90);
                servos[currentSide-1].setEngaged(true);
                Thread.sleep(1000);

                servos[currentSide-1].setTargetPosition(0);
                servos[currentSide-1].setEngaged(true);
                Thread.sleep(1000);

                servos[currentSide-1].setTargetPosition(90);
                servos[currentSide-1].setEngaged(true);
                Thread.sleep(1000);

                // Restart dice roll after all faces have been seen
                if (currentSide == numOfSidesActive) {
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

        autoRollCoolDown = System.currentTimeMillis();

        isAutoRollEngaged = false;

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


    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public void onSensorChanged(SensorEvent event) {

        if ((Math.abs(event.values[0]) > accelerometerThreshold) || (Math.abs(event.values[1]) > accelerometerThreshold) || (Math.abs(event.values[2]) > accelerometerThreshold)) {

            System.out.println("-----------------------------------");
            System.out.println("-----------Shake Detected----------");
            System.out.println("-----------------------------------");

            try {
                if (!isAutoRollEngaged && (System.currentTimeMillis() - autoRollCoolDown) > coolDownTime) {
                    autoRoll();
                }

            } catch (PhidgetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

    public void checkAnswer(int selectedChoiceIndex) throws PhidgetException {

        ArrayList<String> currentDiceSides = (ArrayList<String>) diceSides.clone();
        System.out.println(currentDiceSides);

        if (choices.get(currentSideUp-1).equals(choices.get(selectedChoiceIndex))) {

            diceSides.set(currentSideUp-1, "Correct");
            updateDice();
            try {
                Thread.sleep(notificationLength);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            diceSides = (ArrayList<String>) currentDiceSides.clone();
            updateDice();

        } else {

            diceSides.set(currentSideUp-1, "Incorrect");
            updateDice();
            try {
                Thread.sleep(notificationLength);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            diceSides = (ArrayList<String>) currentDiceSides.clone();
            updateDice();


        }

    }


    public void setOperatingMode(OperatingMode op) {
        this.operatingMode = op;
    }
}
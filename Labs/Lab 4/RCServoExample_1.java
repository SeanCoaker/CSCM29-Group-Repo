package com.example.PhidgetRCServoExample;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.phidget22.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class RCServoExample extends Activity {

	RCServo rcServo0;
	Button engagedButton;
    SeekBar targetPositionBar;

	VoltageRatioInput voltageRatioInput0;

	Toast errToast;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Hide device information and settings until one is attached
		LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData);
		settingsAndData.setVisibility(LinearLayout.GONE);

		//set button functionality
		engagedButton = (Button) findViewById(R.id.engagedButton);
		engagedButton.setOnClickListener(new engagedChangeListener());

		targetPositionBar = (SeekBar) findViewById(R.id.targetPositionBar);
		targetPositionBar.setOnSeekBarChangeListener(new targetPositionChangeListener());
		((TextView)findViewById(R.id.positionTxt)).setText("");

        try
        {
			rcServo0 = new RCServo();

        	//Allow direct USB connection of Phidgets
			if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST))
                com.phidget22.usb.Manager.Initialize(this);

			//Enable server discovery to list remote Phidgets
			this.getSystemService(Context.NSD_SERVICE);
			Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

			//Add a specific network server to communicate with Phidgets remotely
			Net.addServer("", "172.26.32.1", 5661, "", 0);
			//Set addressing parameters to specify which channel to open (if any)
			rcServo0.setDeviceSerialNumber(20986);

			rcServo0.addAttachListener(new AttachListener() {
				public void onAttach(final AttachEvent attachEvent) {
				    AttachEventHandler handler = new AttachEventHandler(rcServo0);
                    synchronized(handler)
					{
						runOnUiThread(handler);
						try {
							handler.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});

			rcServo0.addDetachListener(new DetachListener() {
				public void onDetach(final DetachEvent detachEvent) {
                    DetachEventHandler handler = new DetachEventHandler(rcServo0);
                    synchronized(handler)
					{
						runOnUiThread(handler);
						try {
							handler.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});

			rcServo0.addErrorListener(new ErrorListener() {
				public void onError(final ErrorEvent errorEvent) {
					ErrorEventHandler handler = new ErrorEventHandler(rcServo0, errorEvent);
					runOnUiThread(handler);
				}
			});

			rcServo0.addPositionChangeListener(new RCServoPositionChangeListener() {
				public void onPositionChange(RCServoPositionChangeEvent positionChangeEvent) {
                    RCServoPositionChangeEventHandler handler = new RCServoPositionChangeEventHandler(rcServo0, positionChangeEvent);
                    runOnUiThread(handler);
                }
			});

			rcServo0.open();

			voltageRatioInput0 = new VoltageRatioInput();

			//Set addressing parameters to specify which channel to open (if any)
			voltageRatioInput0.setIsHubPortDevice(true);
			voltageRatioInput0.setDeviceSerialNumber(619527);
//			voltageRatioInput0.setChannel(0);
			voltageRatioInput0.setHubPort(2);


			voltageRatioInput0.addAttachListener(new AttachListener() {
				public void onAttach(final AttachEvent attachEvent) {
					AttachEventHandler2 handler = new AttachEventHandler2(voltageRatioInput0);
					runOnUiThread(handler);
				}
			});

			voltageRatioInput0.addDetachListener(new DetachListener() {
				public void onDetach(final DetachEvent detachEvent) {
					DetachEventHandler2 handler = new DetachEventHandler2(voltageRatioInput0);
					runOnUiThread(handler);

				}
			});

			voltageRatioInput0.addVoltageRatioChangeListener(new VoltageRatioInputVoltageRatioChangeListener() {
				public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent) {
					VoltageRatioInputVoltageRatioChangeEventHandler handler = new VoltageRatioInputVoltageRatioChangeEventHandler(voltageRatioInput0, voltageRatioChangeEvent);
					runOnUiThread(handler);
				}
			});

			voltageRatioInput0.open();

		} catch (PhidgetException pe) {
	        pe.printStackTrace();
		}

    }

	private class engagedChangeListener implements Button.OnClickListener {
		public void onClick(View v) {
			try {
				if(engagedButton.getText() == "Engage") {
					rcServo0.setEngaged(true);
					engagedButton.setText("Disengage");
				}
				else {
					rcServo0.setEngaged(false);
					engagedButton.setText("Engage");
				}
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}
	}

    private class targetPositionChangeListener implements SeekBar.OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
			if(fromUser) {
				try {
					TextView targetPositionTxt = (TextView) findViewById(R.id.targetPositionTxt);
					double targetPosition = Math.round(((double) progress / seekBar.getMax()) *
							(rcServo0.getMaxPosition() - rcServo0.getMinPosition()) + rcServo0.getMinPosition());
					targetPositionTxt.setText(String.valueOf(targetPosition));
					rcServo0.setTargetPosition(targetPosition);
				} catch (PhidgetException e) {
					e.printStackTrace();
				}
			}
        }

        public void onStartTrackingTouch(SeekBar seekBar) {}

        public void onStopTrackingTouch(SeekBar seekBar) {}
    }

    class AttachEventHandler implements Runnable { 
    	Phidget ch;

		public AttachEventHandler(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData);
			settingsAndData.setVisibility(LinearLayout.VISIBLE);

			TextView attachedTxt = (TextView) findViewById(R.id.attachedTxt);

			attachedTxt.setText("Attached");
			try {
				TextView nameTxt = (TextView) findViewById(R.id.nameTxt);
				TextView serialTxt = (TextView) findViewById(R.id.serialTxt);
				TextView versionTxt = (TextView) findViewById(R.id.versionTxt);
				TextView channelTxt = (TextView) findViewById(R.id.channelTxt);
				TextView hubPortTxt = (TextView) findViewById(R.id.hubPortTxt);
				TextView labelTxt = (TextView) findViewById(R.id.labelTxt);

				nameTxt.setText(ch.getDeviceName());
				serialTxt.setText(Integer.toString(ch.getDeviceSerialNumber()));
				versionTxt.setText(Integer.toString(ch.getDeviceVersion()));
				channelTxt.setText(Integer.toString(ch.getChannel()));
				hubPortTxt.setText(Integer.toString(ch.getHubPort()));
				labelTxt.setText(ch.getDeviceLabel());

                SeekBar targetPositionBar = (SeekBar) findViewById(R.id.targetPositionBar);
                targetPositionBar.setProgress(targetPositionBar.getMax()/2);

                double targetPosition = (((RCServo)ch).getMaxPosition() - ((RCServo)ch).getMinPosition())/2
						+ ((RCServo)ch).getMinPosition();

				TextView targetPositionTxt = (TextView) findViewById(R.id.targetPositionTxt);
				targetPositionTxt.setText(String.valueOf(targetPosition));

				((RCServo)ch).setTargetPosition(targetPosition);

				engagedButton.setText("Engage");
			} catch (PhidgetException e) {
				e.printStackTrace();
			}

			//notify that we're done
			synchronized(this)
			{
				this.notify();
			}
		}
    }
    
    class DetachEventHandler implements Runnable {
    	Phidget ch;
    	
    	public DetachEventHandler(Phidget ch) {
    		this.ch = ch;
    	}
    	
		public void run() {
			LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData);

			settingsAndData.setVisibility(LinearLayout.GONE);

			TextView attachedTxt = (TextView) findViewById(R.id.attachedTxt);
			attachedTxt.setText("Detached");

			TextView nameTxt = (TextView) findViewById(R.id.nameTxt);
			TextView serialTxt = (TextView) findViewById(R.id.serialTxt);
			TextView versionTxt = (TextView) findViewById(R.id.versionTxt);
			TextView channelTxt = (TextView) findViewById(R.id.channelTxt);
			TextView hubPortTxt = (TextView) findViewById(R.id.hubPortTxt);
			TextView labelTxt = (TextView) findViewById(R.id.labelTxt);

			nameTxt.setText(R.string.unknown_val);
			serialTxt.setText(R.string.unknown_val);
			versionTxt.setText(R.string.unknown_val);
			channelTxt.setText(R.string.unknown_val);
			hubPortTxt.setText(R.string.unknown_val);
			labelTxt.setText(R.string.unknown_val);

			//notify that we're done
			synchronized(this)
			{
				this.notify();
			}
		}
    }

	class ErrorEventHandler implements Runnable {
		Phidget ch;
		ErrorEvent errorEvent;

		public ErrorEventHandler(Phidget ch, ErrorEvent errorEvent) {
			this.ch = ch;
			this.errorEvent = errorEvent;
		}

		public void run() {
			 if (errToast == null)
				 errToast = Toast.makeText(getApplicationContext(), errorEvent.getDescription(), Toast.LENGTH_SHORT);

			 //replace the previous toast message if a new error occurs
			 errToast.setText(errorEvent.getDescription());
			 errToast.show();
        }
	}

	class RCServoPositionChangeEventHandler implements Runnable {
		Phidget ch;
		RCServoPositionChangeEvent positionChangeEvent;

		public RCServoPositionChangeEventHandler(Phidget ch, RCServoPositionChangeEvent positionChangeEvent) {
			this.ch = ch;
			this.positionChangeEvent = positionChangeEvent;
		}

		public void run() {
			DecimalFormat numberFormat = new DecimalFormat("#.##");
			TextView positionTxt = (TextView)findViewById(R.id.positionTxt);
			positionTxt.setText(numberFormat.format(positionChangeEvent.getPosition()));
		}
	}

	class AttachEventHandler2 implements Runnable {
		Phidget ch;

		public AttachEventHandler2(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData2);
			settingsAndData.setVisibility(LinearLayout.VISIBLE);

			TextView attachedTxt = (TextView) findViewById(R.id.attachedTxt2);

			attachedTxt.setText("Attached");
			try {
				TextView nameTxt = (TextView) findViewById(R.id.nameTxt2);
				TextView serialTxt = (TextView) findViewById(R.id.serialTxt2);
				TextView versionTxt = (TextView) findViewById(R.id.versionTxt2);
				TextView channelTxt = (TextView) findViewById(R.id.channelTxt2);
				TextView hubPortTxt = (TextView) findViewById(R.id.hubPortTxt2);
				TextView labelTxt = (TextView) findViewById(R.id.labelTxt2);

				nameTxt.setText(ch.getDeviceName());
				serialTxt.setText(Integer.toString(ch.getDeviceSerialNumber()));
				versionTxt.setText(Integer.toString(ch.getDeviceVersion()));
				channelTxt.setText(Integer.toString(ch.getChannel()));
				hubPortTxt.setText(Integer.toString(ch.getHubPort()));
				labelTxt.setText(ch.getDeviceLabel());
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}
	}

	class DetachEventHandler2 implements Runnable {
		Phidget ch;

		public DetachEventHandler2(Phidget ch) {
			this.ch = ch;
		}

		public void run() {
			LinearLayout settingsAndData = (LinearLayout) findViewById(R.id.settingsAndData2);

			settingsAndData.setVisibility(LinearLayout.GONE);

			TextView attachedTxt = (TextView) findViewById(R.id.attachedTxt2);
			attachedTxt.setText("Detached");

			TextView nameTxt = (TextView) findViewById(R.id.nameTxt2);
			TextView serialTxt = (TextView) findViewById(R.id.serialTxt2);
			TextView versionTxt = (TextView) findViewById(R.id.versionTxt2);
			TextView channelTxt = (TextView) findViewById(R.id.channelTxt2);
			TextView hubPortTxt = (TextView) findViewById(R.id.hubPortTxt2);
			TextView labelTxt = (TextView) findViewById(R.id.labelTxt2);

			nameTxt.setText(R.string.unknown_val);
			serialTxt.setText(R.string.unknown_val);
			versionTxt.setText(R.string.unknown_val);
			channelTxt.setText(R.string.unknown_val);
			hubPortTxt.setText(R.string.unknown_val);
			labelTxt.setText(R.string.unknown_val);
		}
	}

	class VoltageRatioInputVoltageRatioChangeEventHandler implements Runnable {
		Phidget ch;
		VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent;

		public VoltageRatioInputVoltageRatioChangeEventHandler(Phidget ch, VoltageRatioInputVoltageRatioChangeEvent voltageRatioChangeEvent) {
			this.ch = ch;
			this.voltageRatioChangeEvent = voltageRatioChangeEvent;
		}

		public void run() {
			TextView voltageRatioTxt = (TextView)findViewById(R.id.voltageRatioTxt);

			voltageRatioTxt.setText(String.valueOf(voltageRatioChangeEvent.getVoltageRatio()));
			try {
				System.out.println(voltageRatioChangeEvent.getVoltageRatio());
				double targetPosition = voltageRatioChangeEvent.getVoltageRatio()*180;
				rcServo0.setTargetPosition(targetPosition);
				SeekBar targetPositionBar = (SeekBar) findViewById(R.id.targetPositionBar);
				targetPositionBar.setProgress((int) (targetPosition/180*targetPositionBar.getMax()));
				TextView targetPositionTxt = (TextView) findViewById(R.id.targetPositionTxt);
				targetPositionTxt.setText(String.format("%.1f", targetPosition));
			} catch (PhidgetException e) {
				e.printStackTrace();
			}

		}
	}

	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	try {
			rcServo0.close();

		} catch (PhidgetException e) {
			e.printStackTrace();
		}
    }

}


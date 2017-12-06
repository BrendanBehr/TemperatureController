package com.example.babeh_000.temperaturecontroller;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ControlActivity extends AppCompatActivity {


    private static TextView mActualTemperatureText;
    private static TextView mDesiredTemperatureText;
    private static TextView mActualTemperatureNum;
    private static EditText mDesiredTemperatureNum;
    private Button mSetTemperatureButton;

    // This tag is used for debug messages
    private static final String TAG = ControlActivity.class.getSimpleName();

    private static int temperature;
    private static int actualTemp = 0;

    private static String mDeviceAddress;
    private static CapSenseTemperatureControllerService mTemperatureController;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object, initialize the service, and connect.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mTemperatureController = ((CapSenseTemperatureControllerService.LocalBinder) service).getService();
            if (!mTemperatureController.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the car database upon successful start-up initialization.
            mTemperatureController.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mTemperatureController = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // Assign the various layout objects to the appropriate variables
        mActualTemperatureText = (TextView) findViewById(R.id.actual_temperature);
        mDesiredTemperatureText = (TextView) findViewById(R.id.desired_temperature);
        mActualTemperatureNum = (TextView) findViewById(R.id.ActualTemperatureValue);
        mDesiredTemperatureNum = (EditText) findViewById(R.id.DesiredTemperatureValue);
        mSetTemperatureButton= (Button) findViewById(R.id.SetTemeratureBtn);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(ScanActivity.EXTRAS_BLE_ADDRESS);

        // Bind to the BLE service
        Log.i(TAG, "Binding Service");
        Intent TemperatureControllerServiceIntent = new Intent(this, CapSenseTemperatureControllerService.class);
        bindService(TemperatureControllerServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        /* This will be called when the right motor enable switch is changed */
        mSetTemperatureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(view.getId() == R.id.SetTemeratureBtn) {
                    String temp = mDesiredTemperatureNum.getText().toString();
                    int value = Integer.parseInt(temp);
                    temperature = value;
                };
                mTemperatureController.setTemperature(temperature);
            }
        });

        //mActualTemperatureNum.setText(mTemperatureController.getTempValue());

    } /* End of onCreate method */

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mTemperatureUpdateReceiver, makeTemperatureUpdateIntentFilter());
        if (mTemperatureController != null) {
            final boolean result = mTemperatureController.connect(mDeviceAddress);
            Log.i(TAG, "Connect request result=" + result);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mTemperatureUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mTemperatureController = null;
    }

    /**
     * Handle broadcasts from the Car service object. The events are:
     * ACTION_CONNECTED: connected to the car.
     * ACTION_DISCONNECTED: disconnected from the car.
     * ACTION_DATA_AVAILABLE: received data from the car.  This can be a result of a read
     * or notify operation.
     */
    private final BroadcastReceiver mTemperatureUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case CapSenseTemperatureControllerService.ACTION_CONNECTED:
                    // No need to do anything here. Service discovery is started by the service.
                    break;
                case CapSenseTemperatureControllerService.ACTION_DISCONNECTED:
                    mTemperatureController.close();
                    break;
                case CapSenseTemperatureControllerService.ACTION_DATA_AVAILABLE:
                    // This is called after a Notify completes
                    mActualTemperatureText.setText("Actual Temperature");
                    int value = mTemperatureController.getTempValue();
                    mActualTemperatureNum.setText(value);
                    mDesiredTemperatureText.setText("Desired Temperature");
                    break;
            }
        }
    };

    /**
     * This sets up the filter for broadcasts that we want to be notified of.
     * This needs to match the broadcast receiver cases.
     *
     * @return intentFilter
     */
    private static IntentFilter makeTemperatureUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CapSenseTemperatureControllerService.ACTION_CONNECTED);
        intentFilter.addAction(CapSenseTemperatureControllerService.ACTION_DISCONNECTED);
        intentFilter.addAction(CapSenseTemperatureControllerService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}

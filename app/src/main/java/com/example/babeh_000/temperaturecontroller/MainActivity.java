package com.example.babeh_000.temperaturecontroller;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private static TextView mDesiredTemperature;
    private static TextView mActualTemperature;
    private static Button mSetTemperatureButton;

    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static CapSenseTemperatureControllerService mCapSenseTemperatureControllerService;

    private static final int REQUEST_ENABLE_BLE = 1;

    private static final int PERMISSION_RREQUST_COARSE_LOCATION = 1;

    private static boolean CapSenseNotifyState = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mCapSenseTemperatureControllerService = ((CapSenseTemperatureControllerService.LocalBinder) service).getService();
            mServiceConnected = true;
            mCapSenseTemperatureControllerService.initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mCapSenseTemperatureControllerService = null;
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDesiredTemperature = (TextView) findViewById(R.id.desired_temperature);
        mActualTemperature = (TextView) findViewById(R.id.actual_temperature);

        mSetTemperatureButton = (Button) findViewById(R.id.SetTemeratureBtn);

        mServiceConnected = false;
        mConnectState = false;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant lovation access so this app can  detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_RREQUST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        //Set Temp Here

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RREQUST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to grant permission");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    });
                    builder.show();
                }
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(CapSenseTemperatureControllerService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(CapSenseTemperatureControllerService.ACTION_CONNECTED);
        filter.addAction(CapSenseTemperatureControllerService.ACTION_DISCONNECTED);
        filter.addAction(CapSenseTemperatureControllerService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(CapSenseTemperatureControllerService.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReciever, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();;
        unregisterReceiver(mBleUpdateReciever);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCapSenseTemperatureControllerService.close();;
        unbindService(mServiceConnecion);
        mCapSenseTemperatureControllerService = null;
        mServiceConnected = false;
    }

    public void startBluetooth(View view) {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceInent = new Intent(this, CapSenseTemperatureControllerService.class);
        bindService(gattServiceIntent, mServiceConnecion, BIND_AUTO_CREATE);

        start_button.setEnabled(false);
        search_button.setEnabled(true);
        Log.d(TAG, "Bluetooth is Enabled");


    }

    public void searchBluetooth(View view) {
        if(mServiceConnected) {
            mCapSenseTemperatureControllerService.scan();
        }
    }

    public void connectBluetooth(View view) {
        mCapSenseTemperatureControllerService.connect();
    }

    public void discoverServices(View view) {
        mCapSenseTemperatureControllerService.discoverServices();
    }

    public void Disconnect(View view) {
        mCapSenseTemperatureControllerService.disconnect();
    }

    private final BroadcastReceiver mBleUpdateReciever = new BroadcastReceiver(){
        @Override
        public void onRecieve(Context context, Intent intent) {
            final String action intent.getAction();
            switch (action) {
                case CapSenseTemperatureControllerService.ACTION_BLESCAN_CALLBACK:
                    search_button.setEnabled(false);
                    connect_button.setEnabled(true);
                    break;
                case CapSenseTemperatureControllerService.ACTION_CONNECTED:
                    if(!mConnectState) {
                        connect_button.setEnabled(false);
                        discover_button.setEnabled(true);
                        discover_button.setEnabled(true);
                        mConnectState = true;
                        Log.d(TAG, "Connected to Device");
                    }
                    break;
                case CapSenseTemperatureControllerService.ACTION_DISCONNECTED:
                    disconnect_button.setEnabled(false);
                    disconnect_button.setEnabled(false);
                    search_button.setEnabled(true);

                    //Turn off Temperature control fields here .setEnable() and .setChecked()

                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;
                case CapSenseTemperatureControllerService.ACTION_SERVICES_DISCOVERED:
                    disconnect_button.setEnabled(false);

                    //Enable Temperature control fields here .setEnable() only


                    Log.d(TAG, "Discovered");
                    break;
                case CapSenseTemperatureControllerService.ACTION_DATA_RECEIVED:
                    //Change from slider to Temperature value
//                    String CapSensePos = mCapSenseTemperatureControllerService.getCapSenseValue();
//                    if(CapSensePos.equals("-1")){
//                        if(!CapSenseNotifyState) {
//                            mCapsenseValue.setText(R.string.NotifyOff);
//                        } else {
//                            mCapsenseValue.setText(R.string.NoTouch);
//                        }
//                    } else {
//                        mCapsenseValue.setText(CapSensePos);
//                    }
                default:
                    break;
            }
        }
    };
}

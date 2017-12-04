package com.example.babeh_000.temperaturecontroller;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CapSenseTemperatureControllerService extends Service {
    private final static  String TAG = CapSenseTemperatureControllerService.class.getSimpleName();

    //Bluetooth objects that we need to interact with
    private static BluetoothManager mBluetoothManager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothLeScanner mLEScanner;
    private static BluetoothDevice mLeDevice;
    private static BluetoothGatt mBluetoothGatt;

    //Bluetooth characteristics we need to read/write
    private static BluetoothGattCharacteristic mLedCharacteristic;
    private static BluetoothGattCharacteristic mCapsenseCharacteristic;
    private static BluetoothGattDescriptor mCapSenseCccd;

    //UUID for the service and characteristics that the custom CapSenseLED service uses
    private final static String baseUUID = "6AC07F87-E95C-4B65-8550-909F0D46D4E";
    private final static String capsenseTempereatureControlServiceUUID = baseUUID + "0";
    private final static String TemperatureControlCharacteristicUUID = baseUUID + "1";
    private final static String capsenseCharachteristicUUID = baseUUID + "2";
    private final static String CccdUUID = "6AC0A889-E95C-4B65-8550-909F0D46D4EB";

    //Vars used for the temperature controller
    private static int mTemperatureRead = 0;
    private static int mTemperatureWrite = 0;
    private static String mCapSenseValue = "-1";

    //Actions used during broadcasts to the main activity
//    public final static String ACTION_BLESCAN_CALLBACK =
//            "com.example.babeh_000.temperaturecontroller.ACTION_BLESCAN_CALLBACK";
    public final static String ACTION_CONNECTED =
            "com.example.babeh_000.temperaturecontroller.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "com.example.babeh_000.temperaturecontroller.ACTION_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.babeh_000.temperaturecontroller.ACTION_DATA_AVAILABLE";
//    public final static String ACTION_SERVICES_DISCOVERED =
//            "com.example.babeh_000.temperaturecontroller.ACTION_SERVICES_DISCOVERED";
//    public final static String ACTION_DATA_RECEIVED =
//            "com.example.babeh_000.temperaturecontroller.ACTION_DATA_RECEIVED";


    public CapSenseTemperatureControllerService() {
    }

    public class LocalBinder extends Binder {
        CapSenseTemperatureControllerService getService() {
            return CapSenseTemperatureControllerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        disconnect();
        close();
        return super.onUnbind(intent);
    }

    public final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        //BluetoothManager
        if(mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager == null){
                Log.e(TAG, "Unable to initialize a BluetoothManager");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Log.e(TAG, "Unable to obtan a BluetoothAdapter");
            return false;
        }

        return true;
    }

    public void scan() {
        //Scan for decices and look for the one with the service we want
        UUID capSenseTemperatureControllerService = UUID.fromString(capsenseTempereatureControlServiceUUID);
        UUID[] capSenseTemperatureControllerServiceArray = {capSenseTemperatureControllerService};

        //Use old scan method for versions older than lollipop
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.startLeScan(capSenseTemperatureControllerServiceArray, mLeScanCallback);
        }
        else {
            ScanSettings settings;
            List<ScanFilter> filters;
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
            filters = new ArrayList<>();
            //We sill scan just for the CAR's UUID
            ParcelUuid PUuid = new ParcelUuid(capSenseTemperatureControllerService);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(PUuid).build();
            filters.add(filter);
            mLEScanner.startScan(filters, settings, mScanCallBack);

        }

    }

    public boolean connect() {
        if(mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        if(mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        mBluetoothGatt = mLeDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection");
        return true;

    }

    public void discoverServices() {
        if(mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "Bluetooth not initialized");
            return;
        }

        mBluetoothGatt.discoverServices();
    }

    public void disconnect() {
        if(mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG,"BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.disconnect();
    }

    public void close() {
        if(mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    //Used to read the current temperature
    public void readTemperatureControllerCharacteristic() {

    }

    //Used to set the desired temperature
    public void writeTemperatureControllerCharacteristic() {

    }

    public String getCapSenseValue(){
        return mCapSenseValue;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mLeDevice = device;

                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    broadcastUpdate(ACTION_BLESCAN_CALLBACK);
                }
            };

    private final ScanCallback mScanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDevice = result.getDevice();
            mLEScanner.stopScan(mScanCallBack);
            broadcastUpdate(ACTION_BLESCAN_CALLBACK);
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_CONNECTED);
                Log.i(TAG, "Connected to Gatt server");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected feom GATT server");
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService mService = gatt.getService(UUID.fromString(capsenseTempereatureControlServiceUUID));
            mLedCharacteristic = mService.getCharacteristic(UUID.fromString(TemperatureControlCharacteristicUUID));
            mCapsenseCharacteristic = mService.getCharacteristic(UUID.fromString(capsenseCharachteristicUUID));
            mCapSenseCccd = mCapsenseCharacteristic.getDescriptor(UUID.fromString(CccdUUID));
            readTemperatureControllerCharacteristic();
//            broadcastUpdate(ACTION_SERVICES_DISCOVERED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status){
            if(status == BluetoothGatt.GATT_SUCCESS) {


                String uuid = characteristic.getUuid().toString();

                if (uuid.equals(TemperatureControlCharacteristicUUID)) {
                    final byte[] data = characteristic.getValue();

                    mTemperatureRead = data[0];
                }

                broadcastUpdate(ACTION_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String uuid = characteristic.getUuid().toString();

            if(uuid.equals(capsenseCharachteristicUUID)){
                mCapSenseValue= characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0).toString();

            }

            broadcastUpdate(ACTION_DATA_RECEIVED);
        }
    };

    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
}


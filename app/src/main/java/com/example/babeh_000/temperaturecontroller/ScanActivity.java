package com.example.babeh_000.temperaturecontroller;


/**
 * Created by babeh_000 on 12/4/2017.
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.M)
public class ScanActivity extends AppCompatActivity {
    private final static String TAG = ScanActivity.class.getSimpleName();

    public static final String EXTRAS_BLE_ADDRESS = "BLE_ADDRESS";

    private static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothLeScanner mLEScanner;
    private static boolean mScanning;
    private static Handler mHandler;

    private static final int REQUEST_ENAGLE_BLE = 1;

    private static final long SCAN_TIMEOUT = 10000;

    private static SwipeRefreshLayout mSwipeRefreshLayout;

    ListView BleDeviceList;

    List<BluetoothDevice> mBluetoothDevice;
    List<String> mBleName;

    ArrayAdapter<String> mBleArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        BleDeviceList = (ListView) findViewById(R.id.BlelistItems);

        mHandler = new Handler();

        if(!getPackageManager().hasSystemFeature(PackageManager
        .FEATURE_BLUETOOTH_LE)) {
           Toast.makeText(this, R.string.no_ble, Toast.LENGTH_SHORT).show();
           finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_SHORT.show());
            finish();
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(this.checkSelfPermission(Manifest
            .permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs lovation access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss (DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
            }
        }
    }

}

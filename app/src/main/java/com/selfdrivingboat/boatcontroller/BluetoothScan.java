package com.selfdrivingboat.boatcontroller;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;


public class BluetoothScan {
    public static final int SCAN_FEATURE_ERROR = 0x00;
    public static final int SCAN_ADAPTER_ERROR = 0x01;
    public static final int SCAN_NEED_ENADLE = 0x02;
    public static final int SCAN_BEGIN_SCAN = 0x03;
    public static final int AUTO_ENABLE_FAILURE = 0x04;

    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothScanCallBack mBluetoothScanCallBack;

    public void startScan(boolean autoEnable, BluetoothScanCallBack callBack) {
        mBluetoothScanCallBack = callBack;
        if (!isBluetoothSupport(autoEnable)) {
            return;
        }
        if (mBluetoothAdapter != null) {
            final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            Log.e("BluetoothScan", "mBluetoothAdapter is null.");
        }
    }

    public void stopScan() {

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothAdapter != null) {
            bluetoothLeScanner.stopScan(mLeScanCallback);
        } else {
            Log.e("BluetoothScan","mBluetoothAdapter is null.");
        }
    }

    private static boolean isBluetoothSupport(Boolean autoEnable) {
        Log.d("siddharthks", "isBluetoothSupport: ");
        Context c = MyApplication.context();
        if (!MyApplication.context().getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mBluetoothScanCallBack.onLeScanInitFailure(SCAN_FEATURE_ERROR);
            return false;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) MyApplication.context().
                getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (autoEnable) {
                    if (mBluetoothAdapter.enable()) {
                        mBluetoothScanCallBack.onLeScanInitSuccess(SCAN_BEGIN_SCAN);
                        return true;
                    } else {
                        mBluetoothScanCallBack.onLeScanInitSuccess(AUTO_ENABLE_FAILURE);
                        return false;
                    }
                } else {
                    mBluetoothScanCallBack.onLeScanInitSuccess(SCAN_NEED_ENADLE);
                    return false;
                }
            } else {
                mBluetoothScanCallBack.onLeScanInitSuccess(SCAN_BEGIN_SCAN);
                return true;
            }
        } else {
            mBluetoothScanCallBack.onLeScanInitFailure(SCAN_ADAPTER_ERROR);
            return false;
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (mBluetoothScanCallBack != null) {
                mBluetoothScanCallBack.onLeScanResult(result.getDevice(), result.getRssi(), result.getScanRecord());
            } else {
                Log.e("BluetoothScan","mBluetoothScanCallBack is null.");
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public interface BluetoothScanCallBack {
        void onLeScanInitFailure(int failureCode);
        void onLeScanInitSuccess(int successCode);
        void onLeScanResult(BluetoothDevice device, int rssi, ScanRecord scanRecord);
    }
}


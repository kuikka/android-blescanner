package com.example.blescanner;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class BLEDeviceManager extends Service {
	private final String ZONE = "Service";
	private final String TAG = this.getClass().getSimpleName(); // "BLEDeviceManager";

	public final static String ACTION_DEVICE_DISCOVERED = "com.example.blescanner.DEVICE_DISCOVERED";
	public final static String ACTION_DEVICE_CONNECTED = "com.example.blescanner.DEVICE_CONNECTED";
	public final static String ACTION_DEVICE_PENDING = "com.example.blescanner.DEVICE_PENDING";
	public final static String ACTION_DEVICE_DISCONNECTED = "com.example.blescanner.DEVICE_DISCONNECTED";
	public final static String ACTION_DEVICE_INVALID = "com.example.blescanner.DEVICE_INVALID";
	public static final String ACTION_DEVICE_UPDATED = "com.example.blescanner.DEVICE_RSSI_UPDATED";
	public static final String ACTION_SERVICES_DISCOVERED = "com.example.blescanner.SERVICES_DISCOVERED";
	public final static String EXTRA_DEVICE_ID = "com.example.blescanner.EXTRA_DATA";

	private final IBinder mBinder = new LocalBinder();
	private boolean mBluetoothEnabled;
	private BluetoothAdapter mAdapter;
	BLEScanDataFilter mFilter;

	public class LocalBinder extends Binder {
		public BLEDeviceManager getService() throws BluetoothNotEnabled {
			LogManager.logD(ZONE, TAG, "getService() called");
			if (mBluetoothEnabled)
				return BLEDeviceManager.this;
			else
				throw new BluetoothNotEnabled();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		LogManager.logD(ZONE, TAG, "onCreate");
		super.onCreate();
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mAdapter = bluetoothManager.getAdapter();

		if (mAdapter == null) {
			mBluetoothEnabled = false;
		} else {
			mBluetoothEnabled = true;
		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		LogManager.logD(ZONE, TAG, "onDestroy");
		super.onDestroy();
		stopScanning();
		stopReScanTimer();
		mReScanTimer.cancel();
	}

	private void broadcastUpdate(String action, BLEDevice device) {
		Intent intent = new Intent(action);
		intent.putExtra(EXTRA_DEVICE_ID, device.getDeviceId());
		sendBroadcast(intent);
	}

	LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			String deviceAddress = device.getAddress(); // also deviceId
			LogManager.logD(ZONE, TAG, "found device " + deviceAddress);

			if (!mDevices.containsKey(deviceAddress)) {
				// Parse scan record, create new BLEDevice etc.
				BLEScanRecord sr = BLEScanRecordParser.parse(scanRecord);
				sr.rssi = rssi;
				BLEDevice ble = new BLEDevice(device, sr);
				mDevices.put(deviceAddress, ble);
				if (mFilter == null) {
					broadcastUpdate(ACTION_DEVICE_DISCOVERED, ble);
				} else {
					if (mFilter.match(scanRecord)) {
						broadcastUpdate(ACTION_DEVICE_DISCOVERED, ble);
					}
				}
			} else {
				BLEDevice ble = mDevices.get(deviceAddress);
				ble.setRssi(rssi);
				broadcastUpdate(ACTION_DEVICE_UPDATED, ble);
			}
		}
	};

	public boolean startScanning() {
		LogManager.logD(ZONE, TAG, "startScanning");
		if (mAdapter.startLeScan(mScanCallback) == true) {
			startReScanTimer();
			return true;
		}
		return false;
	}

	public boolean startScanning(BLEScanDataFilter filter) {
		mFilter = filter;
		return startScanning();
	}

	public void stopScanning() {
		stopReScanTimer();
		mAdapter.stopLeScan(mScanCallback);
		mFilter = null;
		mDevices.clear();
	}

	private HashMap<String, BLEDevice> mDevices = new HashMap<String, BLEDevice>();

	public BLEDevice getDevice(String deviceId) {
		return mDevices.get(deviceId);
	}

	private Timer mReScanTimer = new Timer();
	private TimerTask mReScanTimerTask;

	private void startReScanTimer() {
		LogManager.logD(ZONE, TAG, "startReScanTimer ");

		mReScanTimerTask = new TimerTask() {

			@Override
			public void run() {
				LogManager.logD(ZONE, TAG, "restarting scan ");

				mAdapter.stopLeScan(mScanCallback);
				mAdapter.startLeScan(mScanCallback);
			}

		};
		mReScanTimer.schedule(mReScanTimerTask, 0L, 2000L);
	}

	private void stopReScanTimer() {
		if (mReScanTimerTask != null)
			mReScanTimerTask.cancel();
		mReScanTimer.purge();
	}
}

package com.example.blescanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import com.example.blescanner.GATTOperation.OperationType;

public class BLEDevice {

	private BluetoothDevice mDevice;
	private BLEScanRecord mScanRecord;
	private BluetoothGatt mGatt;
	private List<IBLEDeviceListener> mListeners = new ArrayList<IBLEDeviceListener>();

	private final String ZONE = "Service";
	private final String TAG = this.getClass().getSimpleName(); // "BLEDeviceManager";
	protected List<BluetoothGattService> mServices;

	class MyGattCallback extends BluetoothGattCallback {
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {

		}

		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			LogManager.logD(ZONE, TAG, "onCharacteristicRead");

			completeCurrentOperation(status, characteristic);

			LogManager.logD(ZONE, TAG, String.format(
					"read characteristic %s value=%s",
					characteristic.getUuid(), characteristic.getValue()));
		}

		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {

			completeCurrentOperation(status, characteristic);
		}

		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			LogManager.logD(ZONE, TAG, String.format(
					"onConnectionStateChange status=%d, newState=%d", status,
					newState));

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				if (gatt.discoverServices() == false) {
					LogManager.logD(ZONE, TAG,
							String.format("discoverServices failed"));
				}
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				setState(ConnectionState.DISCONNECTED);
			}

		}

		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			LogManager.logD(ZONE, TAG, "onDescriptorRead");

			completeCurrentOperation(status, descriptor);

			LogManager.logD(
					ZONE,
					TAG,
					String.format("read descriptor %s value=%s",
							descriptor.getUuid(), descriptor.getValue()));
		}

		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {

			completeCurrentOperation(status, descriptor);
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

		}

		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

		}

		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			LogManager.logD(ZONE, TAG, "onServicesDiscovered status=" + status);

			setState(ConnectionState.CONNECTED);
			List<BluetoothGattService> services = gatt.getServices();
			mServices = services;

			for (BluetoothGattService s : services) {
				LogManager.logD(ZONE, TAG, "Discovered service " + s.getUuid());
				for (BluetoothGattCharacteristic c : s.getCharacteristics()) {

					GATTOperation op = new GATTOperation(
							OperationType.READ_CHARACTERISTIC, c,
							new GATTCharacteristicListener() {

								@Override
								public void read(int status,
										BluetoothGattCharacteristic c) {
									LogManager.logD(
											ZONE,
											TAG,
											"  Discovered characteristic "
													+ c.getUuid());
									LogManager.logD(ZONE, TAG,
											"  Value = " + c.getValue());

									for (BluetoothGattDescriptor d : c
											.getDescriptors()) {
										LogManager.logD(ZONE, TAG,
												"    Discovered descriptor "
														+ d.getUuid());

									}
								}
							});
					queue(op);
				}
			}
		}
	};

	public BLEDevice(BluetoothDevice device) {
		mDevice = device;
	}

	public BLEDevice(BluetoothDevice device, BLEScanRecord sr) {
		this(device);
		mScanRecord = sr;
	}

	public String getDeviceId() {
		return mDevice.getAddress();
	}

	public String getName() {
		return mScanRecord.name;
	}

	public int getRssi() {
		return mScanRecord.rssi;
	}

	public void setRssi(int rssi) {
		mScanRecord.rssi = rssi;
	}

	public boolean connect(Context context) {
		LogManager.logD(ZONE, TAG, "connect");

		mGatt = mDevice.connectGatt(context, false, new MyGattCallback());
		if (mGatt == null)
			return false;
		return true;
	}

	public BLEScanRecord getScanRecord() {
		return mScanRecord;
	}

	protected Queue<GATTOperation> mOperations = new ConcurrentLinkedQueue<GATTOperation>();
	protected Handler mGattHandler = new Handler();
	protected GATTOperation mCurrentOperation;

	public void queue(final GATTOperation op) {
		mGattHandler.post(new Runnable() {
			@Override
			public void run() {
				mOperations.add(op);
				if (mCurrentOperation == null)
					handleOperation();
			}
		});
	}

	protected static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");

	protected void handleOperation() {
		GATTOperation op = mOperations.poll();

		if (op == null)
			return;

		if (mCurrentOperation != null) {
			LogManager.logE(ZONE, TAG, "Operation not null!");
			return;
		}

		BluetoothGattCharacteristic c = op.getCharacteristic();
		BluetoothGattDescriptor d = op.getDescriptor();

		switch (op.getType()) {
		case READ_CHARACTERISTIC:
			mCurrentOperation = op;
			mGatt.readCharacteristic(c);
			break;
		case WRITE_CHARACTERISTIC:
			mCurrentOperation = op;
			mGatt.writeCharacteristic(c);
			break;
		case READ_DESCRIPTOR:
			mCurrentOperation = op;
			mGatt.readDescriptor(d);
			break;
		case WRITE_DESCRIPTOR:
			mCurrentOperation = op;
			mGatt.writeDescriptor(d);
			break;
		case NOTIFY_START:
			BluetoothGattDescriptor ntfDescriptor = c
					.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
			if (ntfDescriptor != null) {
				mGatt.setCharacteristicNotification(c, true);
				GATTOperation decsriptorWriteOp = new GATTOperation(
						OperationType.WRITE_DESCRIPTOR, ntfDescriptor);
				queue(decsriptorWriteOp);
			}
			break;
		case NOTIFY_END:
			break;

		}

		if (mCurrentOperation == null) {
			processNextOperation();
		}
	}

	protected void processNextOperation() {
		mGattHandler.post(new Runnable() {

			@Override
			public void run() {
				handleOperation();
			}
		});
	}

	protected void completeCurrentOperation(final int status,
			final BluetoothGattCharacteristic c) {
		mGattHandler.post(new Runnable() {

			@Override
			public void run() {
				mCurrentOperation.complete(status, c);
				mCurrentOperation = null;
				processNextOperation();
			}
		});
	}

	protected void completeCurrentOperation(final int status,
			final BluetoothGattDescriptor d) {
		mGattHandler.post(new Runnable() {

			@Override
			public void run() {
				mCurrentOperation.complete(status, d);
				mCurrentOperation = null;
				processNextOperation();
			}
		});
	}

	public void addListener(IBLEDeviceListener listener) {
		mListeners.add(listener);
	}

	public enum ConnectionState {
		CONNECTED, DISCONNECTED
	};

	protected void setState(ConnectionState state) {
		for (IBLEDeviceListener l : mListeners) {
			l.onConnectionStateChange(this, state);
		}
	}
}

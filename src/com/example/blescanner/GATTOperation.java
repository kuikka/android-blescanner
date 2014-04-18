package com.example.blescanner;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class GATTOperation {
	public enum OperationType {
		READ_CHARACTERISTIC, WRITE_CHARACTERISTIC, READ_DESCRIPTOR, WRITE_DESCRIPTOR, NOTIFY_START, NOTIFY_END,
	}

	protected OperationType mType;
	protected BluetoothGattCharacteristic mCharacteristic;
	protected BluetoothGattDescriptor mDescriptor;
	protected GATTCharacteristicListener mCharListener;
	protected IGATTDescriptorListener mDescListener;

	public GATTOperation(OperationType type,
			BluetoothGattCharacteristic characteristic,
			GATTCharacteristicListener listener) {
		mType = type;
		mCharacteristic = characteristic;
		mCharListener = listener;
	}

	public GATTOperation(OperationType type,
			BluetoothGattCharacteristic characteristic) {
		mType = type;
		mCharacteristic = characteristic;
	}

	public GATTOperation(OperationType type,
			BluetoothGattDescriptor descriptor, IGATTDescriptorListener listener) {
		mType = type;
		mDescriptor = descriptor;
		mDescListener = listener;
	}

	public GATTOperation(OperationType type, BluetoothGattDescriptor descriptor) {
		mType = type;
		mDescriptor = descriptor;
	}

	public OperationType getType() {
		return mType;
	}

	public BluetoothGattDescriptor getDescriptor() {
		return mDescriptor;
	}

	public BluetoothGattCharacteristic getCharacteristic() {
		return mCharacteristic;
	}

	public void complete(int status, BluetoothGattCharacteristic c) {
		if (mType == OperationType.READ_CHARACTERISTIC)
			mCharListener.read(status, c);
		else if (mType == OperationType.WRITE_CHARACTERISTIC)
			mCharListener.written(status, c);
	}

	public void complete(int status, BluetoothGattDescriptor d) {
		if (mType == OperationType.READ_CHARACTERISTIC)
			mDescListener.read(status, d);
		else if (mType == OperationType.WRITE_CHARACTERISTIC)
			mDescListener.written(status, d);
	}
}

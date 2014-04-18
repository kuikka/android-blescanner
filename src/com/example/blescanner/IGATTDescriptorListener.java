package com.example.blescanner;

import android.bluetooth.BluetoothGattDescriptor;

public interface IGATTDescriptorListener {
	void read(int status, BluetoothGattDescriptor d);

	void written(int status, BluetoothGattDescriptor d);
}

package com.example.blescanner;

import com.example.blescanner.BLEDevice.ConnectionState;

public interface IBLEDeviceListener {
	void onConnectionStateChange(BLEDevice device, ConnectionState state);
}

package com.example.blescanner;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdvertisementDataManager {
	class DeviceStatus {

		public int rssi;
		public BLEScanRecord scanRecord;

		public DeviceStatus(BLEDevice device) {
			rssi = device.getRssi();
			scanRecord = device.getScanRecord();
		}

	};

	HashMap<String, ArrayList<DeviceStatus>> mData = new HashMap<String, ArrayList<DeviceStatus>>();

	public void updateDevice(BLEDevice device) {
		ArrayList<DeviceStatus> list = mData.get(device);
		if (list == null) {
			list = new ArrayList<DeviceStatus>();
			mData.put(device.getDeviceId(), list);
		}
		list.add(new DeviceStatus(device));
	}

	void clear() {
		mData.clear();
	}

	JSONArray getData() throws JSONException {
		JSONArray json = new JSONArray();

		for (String id : mData.keySet()) {
			JSONObject d = new JSONObject();
			int sum = 0, count = 0, average = 0;

			d.put("id", id);

			for (DeviceStatus status : mData.get(id)) {
				sum += status.rssi;
				count++;
				d.put("name", status.scanRecord.name);
			}
			if (count > 0) {
				average = sum / count;
			} else {
				average = -127;
			}
			d.put("rssi", average);
			json.put(d);
		}

		return json;
	}
}

package com.example.blescanner;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final String ZONE = "Application";
	private final String TAG = this.getClass().getSimpleName(); // "BLEService";

	private static final int REQUEST_CONNECT_DEVICE = 0;
	private static final int REQUEST_ENABLE_BT = 1;

	private BLEDeviceManager mDeviceManager;

	private static IntentFilter makeDeviceManagerIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BLEDeviceManager.ACTION_DEVICE_DISCOVERED);
		intentFilter.addAction(BLEDeviceManager.ACTION_DEVICE_PENDING);
		intentFilter.addAction(BLEDeviceManager.ACTION_DEVICE_INVALID);
		intentFilter.addAction(BLEDeviceManager.ACTION_DEVICE_UPDATED);
		return intentFilter;
	}

	private final void removeDevice(BLEDevice device) {
		Iterator<BLEDevice> i = mDeviceList.iterator();
		while (i.hasNext()) {
			BLEDevice d = i.next();
			if (d.getDeviceId().equals(device.getDeviceId())) {
				i.remove();
			}
		}
	}

	private final void addDevice(BLEDevice device) {
		mDeviceListAdapter.add(device);
		Collections.sort(mDeviceList, new Comparator<BLEDevice>() {

			@Override
			public int compare(BLEDevice lhs, BLEDevice rhs) {
				if (lhs.getRssi() < rhs.getRssi())
					return 1;
				else if (lhs.getRssi() > rhs.getRssi())
					return -1;
				else
					return 0;
			}

		});
		mDeviceListAdapter.notifyDataSetChanged();
	}

	private AdvertisementDataManager mAdvManager = new AdvertisementDataManager();

	private final BroadcastReceiver mDeviceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String deviceId = intent.getExtras().getString(
					BLEDeviceManager.EXTRA_DEVICE_ID);

			if (action.equals(BLEDeviceManager.ACTION_DEVICE_DISCOVERED)) {
				BLEDevice device = mDeviceManager.getDevice(deviceId);

				LogManager.logD(ZONE, TAG, "device discovered " + deviceId);

				if (device == null) {
					LogManager.logE(ZONE, TAG,
							"Discovered Device doesn't exist");
					return;
				}
				removeDevice(device);
				addDevice(device);

				device.connect(getApplicationContext());
				mAdvManager.updateDevice(device);

			} else if (action.equals(BLEDeviceManager.ACTION_DEVICE_INVALID)) {
				for (BLEDevice device : mDeviceList) {
					if (device.getDeviceId() == deviceId) {
						removeDeviceFromList(device);
						return;
					}
				}
			} else if (action.equals(BLEDeviceManager.ACTION_DEVICE_UPDATED)) {
				BLEDevice device = mDeviceManager.getDevice(deviceId);

				removeDevice(device);
				addDevice(device);
				mAdvManager.updateDevice(device);
			}
		}

	};

	private void removeDeviceFromList(final BLEDevice device) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mDeviceListAdapter.remove(device);
			}
		});
	}

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LogManager.logD(ZONE, TAG, "Bound to the BLEDeviceManager");

			// If Bluetooth isn't enabled, ask for permission to use it
			try {
				mDeviceManager = ((BLEDeviceManager.LocalBinder) service)
						.getService();
			} catch (BluetoothNotEnabled e) {
				unbindService(mServiceConnection);

				Intent enableBTIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
				return;
			}
			LogManager.logD(ZONE, TAG, "Starting scanning");
			mDeviceManager.startScanning();
			startUpdateTimer();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			LogManager.logD(ZONE, TAG, "Binding to the BLEDeviceManager lost");
			mDeviceManager = null;
		}
	};

	private List<BLEDevice> mDeviceList = new ArrayList<BLEDevice>();
	private ArrayAdapter<BLEDevice> mDeviceListAdapter;

	OnItemClickListener mListClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
			LogManager.logD(ZONE, TAG,
					String.format("Clicked on device, pos %d", pos));

			mDeviceManager.stopScanning();
		}
	};

	private void setupUI() {
		setContentView(R.layout.activity_main);

		mDeviceListAdapter = new ArrayAdapter<BLEDevice>(this,
				android.R.layout.simple_list_item_1, mDeviceList) {

			@Override
			public View getView(int position, View convertView,
					android.view.ViewGroup parent) {
				View row;
				if (convertView == null) {
					row = getLayoutInflater().inflate(
							android.R.layout.simple_list_item_1, null);
				} else {
					row = convertView;
				}

				BLEDevice device = getItem(position);
				String text = String.format(Locale.US, "<%s> - (%s) [%d dB]",
						device.getDeviceId(), device.getName(),
						device.getRssi());

				TextView tv = (TextView) row.findViewById(android.R.id.text1);
				tv.setTextColor(Color.BLACK);
				tv.setText(text);
				return row;
			}
		};

		ListView view = (ListView) findViewById(R.id.listView1);
		view.setAdapter(mDeviceListAdapter);
		view.setOnItemClickListener(mListClickListener);
	}

	private PowerManager.WakeLock mWakeLock = null;

	@SuppressWarnings("deprecation")
	private void createWakeLock() {
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		LogManager.logE(ZONE, TAG, "onCreate");
		super.onCreate(savedInstanceState);

		setupUI();

		// Before we can interact with the device we need to bind to the
		// BLEDeviceManager service.
		Intent deviceManagerIntent = new Intent(this, BLEDeviceManager.class);
		bindService(deviceManagerIntent, mServiceConnection, BIND_AUTO_CREATE);

		createWakeLock();

	}

	@Override
	protected void onResume() {
		LogManager.logD(ZONE, TAG, "onResume");
		super.onResume();

		// Force a redraw of our device list
		mDeviceListAdapter.notifyDataSetChanged();

		registerReceiver(mDeviceReceiver, makeDeviceManagerIntentFilter());
		mWakeLock.acquire();
	}

	@Override
	protected void onPause() {
		LogManager.logD(ZONE, TAG, "onPause");
		super.onPause();
		mDeviceManager.stopScanning();
		unbindService(mServiceConnection);
		unregisterReceiver(mDeviceReceiver);
		stopUpdateTimer();
		if (mServerRequest != null) {
			mServerRequest.cancelRequest();
			mServerRequest = null;
		}
		mWakeLock.release();
		LogManager.logD(ZONE, TAG, "onPause end");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private Timer mUpdateTimer = null;

	private String getDeviceID() {
		return Settings.Secure.getString(getContentResolver(),
				Settings.Secure.ANDROID_ID);

	}

	JSONObject createAdvertisementData() {
		JSONObject data = new JSONObject();
		try {
			data.put("deviceId", getDeviceID());
			data.put("time", System.currentTimeMillis());
			data.put("data", mAdvManager.getData());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return data;
	}

	private HttpRequestBase createHttpRequest(String url, JSONObject data) {
		HttpPost request = new HttpPost(url);
		request.setHeader("Content-Type", "application/json");
		StringEntity putData;
		try {
			putData = new StringEntity(data.toString());
		} catch (UnsupportedEncodingException e) {
			LogManager.logE(ZONE, TAG, "Error creating StringEntity", e);
			return null;
		}
		request.setEntity(putData);
		return request;
	}

	private DoHttpRequest mServerRequest = null;

	private void sendUpdateToServer(String url) {
		LogManager.logD(ZONE, TAG, "sendUpdateToServer http mServerRequest="
				+ mServerRequest);
		if (mServerRequest == null) {
			JSONObject data = createAdvertisementData();
			mAdvManager.clear();
			HttpRequestBase request = createHttpRequest(url, data);

			if (request != null) {
				mServerRequest = new DoHttpRequest() {
					@Override
					protected void onPostExecute(HttpResponse result) {
						mServerRequest = null;
					}
				};
				mServerRequest.execute(request);

			}
		}
	}

	private void startUpdateTimer() {
		if (mUpdateTimer != null) {
			mUpdateTimer.cancel();
			mUpdateTimer = null;
		}
		mUpdateTimer = new Timer();
		mUpdateTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				sendUpdateToServer("http://192.168.1.98:8080/postData");
			}

		}, 1000L, 1000L);
	}

	private void stopUpdateTimer() {
		if (mUpdateTimer != null) {
			mUpdateTimer.cancel();
			mUpdateTimer = null;
		}
	}

	/**
	 * Replies from started activities come here
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != Activity.RESULT_OK) {
			LogManager.logW(ZONE, TAG,
					"Activity did not return a valid result.");
			return;
		}

		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			Intent deviceManagerIntent = new Intent(this,
					BLEDeviceManager.class);
			bindService(deviceManagerIntent, mServiceConnection,
					BIND_AUTO_CREATE);
			break;
		}
	}
}

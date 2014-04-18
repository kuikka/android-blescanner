package com.example.blescanner;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.util.Log;

public class LogManager {
	private static StringBuilder createMessage(CharSequence zone,
			CharSequence msg) {
		StringBuilder sb = new StringBuilder(zone);
		sb.append(": ");
		sb.append(msg);
		return sb;
	}

	private static StringBuilder createMessage(CharSequence zone,
			CharSequence msg, Exception e) {
		StringBuilder sb = createMessage(zone, msg);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		sb.append(sw);
		return sb;
	}

	public static void logD(CharSequence ZONE, String TAG, CharSequence msg) {
		Log.d(TAG, createMessage(ZONE, msg).toString());
	}

	public static void logE(String ZONE, String TAG, CharSequence msg) {
		Log.e(TAG, createMessage(ZONE, msg).toString());
	}

	public static void logE(String ZONE, String TAG, CharSequence msg,
			Exception e) {
		Log.e(TAG, createMessage(ZONE, msg, e).toString());
	}

	public static void logW(String ZONE, String TAG, CharSequence msg) {
		Log.w(TAG, createMessage(ZONE, msg).toString());
	}
}

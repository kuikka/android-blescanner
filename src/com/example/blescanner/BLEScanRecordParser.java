package com.example.blescanner;

import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class BLEScanRecordParser {

	private static final byte DATA_TYPE_FLAGS = 0x01;

	private static final byte DATA_TYPE_UUID16_MORE = 0x02;
	private static final byte DATA_TYPE_UUID16 = 0x03;
	private static final byte DATA_TYPE_UUID32_MORE = 0x04;
	private static final byte DATA_TYPE_UUID32 = 0x05;
	private static final byte DATA_TYPE_UUID128_MORE = 0x06;
	private static final byte DATA_TYPE_UUID128 = 0x07;

	private static final byte DATA_TYPE_NAME_SHORTENED = 0x08;
	private static final byte DATA_TYPE_NAME = 0x09;

	private static final byte DATA_TYPE_TX_POWER_LEVEL = 0x0A;

	private static final byte DATA_TYPE_DEVICE_CLASS = 0x0D;
	private static final byte DATA_TYPE_PAIRING_HASH = 0x0E;
	private static final byte DATA_TYPE_PAIRING_RANDOMIZER = 0x0F;
	private static final byte DATA_TYPE_TK = 0x10;
	private static final byte DATA_TYPE_SM_OOB_FLAGS = 0x11;

	private static final byte DATA_TYPE_SLAVE_CONNECTION_INTERVAL = 0x12;
	private static final byte DATA_TYPE_SERVICE_UUID16 = 0x14;
	private static final byte DATA_TYPE_SERVICE_UUID128 = 0x15;
	private static final byte DATA_TYPE_SERVICE_DATA = 0x16;

	private static final byte DATA_TYPE_MANUF = (byte) 0xFF;
	private static final CharSequence ZONE = "Service";
	private static final String TAG = "BLEScanRecordParser";

	public static BLEScanRecord parse(byte[] scanRecord) {
		BLEScanRecord sr = new BLEScanRecord();
		int parseIndex = 0;
		int dataLeft = scanRecord.length;
		ByteBuffer bb;

		while (true) {
			try {
				if (dataLeft < 3) {
					return sr;
				}
				int length = scanRecord[parseIndex] + 1; // Account for the
															// length field
															// length
				bb = ByteBuffer.wrap(scanRecord, parseIndex, length);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				boolean done = parse(sr, bb);
				if (done)
					return sr;

				dataLeft -= length;
				parseIndex += length;

			} catch (IndexOutOfBoundsException e) {
				return sr;
			} catch (BufferUnderflowException e) {
				return sr;
			}
		}
	}

	private static boolean parse(BLEScanRecord sr, ByteBuffer bb)
			throws BufferUnderflowException {

		int dataLength;
		byte dataType;

		dataLength = (bb.get() & 0xFF) - 1;
		dataType = bb.get();

		if (dataType == 0)
			return true;

		switch (dataType) {
		case DATA_TYPE_FLAGS:
			sr.flags = bb.get();
			LogManager.logD(ZONE, TAG,
					String.format("Got flags 0x%02x", sr.flags));
			break;
		case DATA_TYPE_UUID16:
			for (int i = 0; i < dataLength / 2; i++) {
				short val = bb.getShort();
				LogManager
						.logD(ZONE, TAG, String.format("Got UUUID16 %x", val));
				// UUID uuid = new UUID(mostSigBits, leastSigBits),
				// leastSigBits)
			}
			break;
		case DATA_TYPE_NAME:
			byte[] nameData = new byte[dataLength];

			for (int i = 0; i < dataLength; i++) {
				nameData[i] = bb.get();
			}
			String name;
			try {
				name = new String(nameData, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			sr.name = name;
			LogManager.logD(ZONE, TAG, String.format("Got NAME %s", name));
			break;
		case DATA_TYPE_UUID128_MORE:
		case DATA_TYPE_UUID128:
			while (bb.remaining() >= 16) {
				long lsb = bb.getLong();
				long msb = bb.getLong();
				UUID uuid = new UUID(msb, lsb);
				sr.uuids.add(uuid);
				LogManager.logD(ZONE, TAG,
						String.format("Got UUID = %s", uuid.toString()));
			}
			break;
		case DATA_TYPE_TX_POWER_LEVEL:
			byte txPower = bb.get();
			LogManager.logD(ZONE, TAG,
					String.format("Got txPower = %d", txPower));

			break;
		case DATA_TYPE_MANUF:
			short oid = bb.getShort();
			byte[] manufacturerData = new byte[dataLength - 2];
			sr.manufacturerData.put(oid, manufacturerData);
			LogManager.logD(ZONE, TAG,
					String.format("Got Manufacturer oid = %x", oid));
			break;
		case DATA_TYPE_TK:
			break;
		case DATA_TYPE_SERVICE_DATA:
			short serviceData = bb.getShort();
			LogManager.logD(ZONE, TAG,
					String.format("Got service data 0x%04x", serviceData));
			break;
		}
		return false;
	}
}

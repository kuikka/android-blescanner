package com.example.blescanner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BLEScanRecord {

	public byte flags;
	/**
	 * Device name
	 */
	public String name;
	/**
	 * Service UUIDs broadcast
	 */
	public Set<UUID> uuids = new HashSet<UUID>();
	/**
	 * Manufacturer data, per OID
	 */
	public HashMap<Short, byte[]> manufacturerData = new HashMap<Short, byte[]>();

	/*
	 * Received RSSI
	 */
	protected int rssi;

}

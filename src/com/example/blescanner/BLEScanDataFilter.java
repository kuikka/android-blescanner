package com.example.blescanner;

public interface BLEScanDataFilter {
	public boolean match(byte[] scanData);
}

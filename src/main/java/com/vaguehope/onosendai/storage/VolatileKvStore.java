package com.vaguehope.onosendai.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VolatileKvStore implements KvStore {

	private final Map<String, String> m = new ConcurrentHashMap<String, String>();

	@Override
	public void storeValue (final String key, final String value) {
		if (key == null) throw new IllegalArgumentException("Can not store against null key.");
		if (value != null) {
			this.m.put(key, value);
		}
		else {
			this.m.remove(key);
		}
	}

	@Override
	public void deleteValue (final String key) {
		this.m.remove(key);
	}

	@Override
	public String getValue (final String key) {
		return this.m.get(key);
	}

}

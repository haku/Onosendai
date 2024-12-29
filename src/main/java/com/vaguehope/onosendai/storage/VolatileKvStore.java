package com.vaguehope.onosendai.storage;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
	public void deleteValuesStartingWith (final String prefix) {
		for (final Iterator<Entry<String, String>> ittr = this.m.entrySet().iterator(); ittr.hasNext();) {
			if (ittr.next().getKey().startsWith(prefix)) ittr.remove();
		}
	}

	@Override
	public String getValue (final String key) {
		return this.m.get(key);
	}

}

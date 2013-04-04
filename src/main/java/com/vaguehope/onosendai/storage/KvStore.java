package com.vaguehope.onosendai.storage;

public interface KvStore {

	void storeValue (String key, String value);
	String getValue (String key);

}

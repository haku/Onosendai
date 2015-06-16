package com.vaguehope.onosendai.storage;

public interface KvStore {

	void storeValue (String key, String value);
	void deleteValue (String key);
	String getValue (String key);

}

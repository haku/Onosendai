package com.vaguehope.onosendai.model;

public enum MetaType {

	MEDIA("M"),
	HASHTAG("H"),
	MENTION("N");

	private final String key;

	private MetaType (final String key) {
		this.key = key;
	}

	public String getKey () {
		return this.key;
	}

	public static MetaType parseKey (final String key) {
		for (MetaType m : values()) { // TODO if this list gets long this may be slow?
			if (m.getKey().equals(key)) return m;
		}
		return null;
	}

}

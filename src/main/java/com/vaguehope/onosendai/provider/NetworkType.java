package com.vaguehope.onosendai.provider;

public enum NetworkType {
	TWITTER("twitter"),
	MASTODON("mastodon"),
	FACEBOOK("facebook");

	private final String name; // NOSONAR not a singular field.

	private NetworkType (final String name) {
		this.name = name;
	}

	public static NetworkType parse (final String s) {
		for (NetworkType type : values()) {
			if (s.equalsIgnoreCase(type.name)) return type;
		}
		return null;
	}

}

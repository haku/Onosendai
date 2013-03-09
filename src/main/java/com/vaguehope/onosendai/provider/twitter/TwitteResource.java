package com.vaguehope.onosendai.provider.twitter;

import java.util.Locale;

public enum TwitteResource {
	TIMELINE,
	MENTIONS,
	ME;

	public static TwitteResource parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}
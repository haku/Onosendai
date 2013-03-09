package com.vaguehope.onosendai.provider.twitter;

import java.util.Locale;

public enum TwitterFeedType {
	TIMELINE,
	MENTIONS,
	ME,
	LIST;

	public static TwitterFeedType parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}
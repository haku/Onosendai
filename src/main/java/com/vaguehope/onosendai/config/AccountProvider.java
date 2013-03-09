package com.vaguehope.onosendai.config;

import java.util.Locale;

public enum AccountProvider {
	TWITTER;

	public static AccountProvider parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}
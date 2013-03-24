package com.vaguehope.onosendai.config;

import java.util.Locale;

public enum AccountProvider {
	TWITTER("Twitter"),
	SUCCESSWHALE("SuccessWhale");

	private final String humanName;

	private AccountProvider (final String humanName) {
		this.humanName = humanName;
	}

	public String toHumanString () {
		return this.humanName;
	}

	public static AccountProvider parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}
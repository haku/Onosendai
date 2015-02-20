package com.vaguehope.onosendai.config;

import java.util.Locale;

import com.vaguehope.onosendai.util.Titleable;

public enum AccountProvider implements Titleable {
	TWITTER("Twitter"), //ES
	SUCCESSWHALE("SuccessWhale"), //ES
	INSTAPAPER("Instapaper"), //ES
	BUFFER("Buffer"), //ES
	HOSAKA("Hosaka"); //ES

	private final String humanName;

	private AccountProvider (final String humanName) {
		this.humanName = humanName;
	}

	@Override
	public String getUiTitle () {
		return this.humanName;
	}

	public static AccountProvider parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}
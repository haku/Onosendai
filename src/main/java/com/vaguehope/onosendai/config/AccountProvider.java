package com.vaguehope.onosendai.config;

import java.util.Locale;

import com.vaguehope.onosendai.util.Titleable;

public enum AccountProvider implements Titleable {
	TWITTER("Twitter"), //ES
	/**
	 * consumerKey = instanceName
	 * accessToken = accessToken
	 */
	MASTODON("Mastodon"), //ES
	/**
	 * accessToken = username
	 * accessSecret = passwd
	 */
	SUCCESSWHALE("SuccessWhale"), //ES
	/**
	 * accessToken = username
	 * accessSecret = passwd
	 */
	INSTAPAPER("Instapaper"), //ES
	/**
	 * accessToken = accessToken
	 */
	BUFFER("Buffer"), //ES
	/**
	 * accessToken = username
	 * accessSecret = passwd
	 */
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
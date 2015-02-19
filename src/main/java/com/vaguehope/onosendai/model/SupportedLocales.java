package com.vaguehope.onosendai.model;

import com.vaguehope.onosendai.util.Titleable;

public enum SupportedLocales implements Titleable {
	DEFAULT("", "Default"),
	EN_CUTE("en_CU", "Cute English");

	private final String value;
	private final String title;

	private SupportedLocales (final String value, final String title) {
		this.value = value;
		this.title = title;
	}

	public String getValue () {
		return this.value;
	}

	@Override
	public String getUiTitle () {
		return this.title;
	}

	public static CharSequence[] prefEntries () {
		final SupportedLocales[] pis = SupportedLocales.values();
		final CharSequence[] ret = new CharSequence[pis.length];
		for (int i = 0; i < pis.length; i++) {
			ret[i] = pis[i].getUiTitle();
		}
		return ret;
	}

	public static CharSequence[] prefEntryValues () {
		final SupportedLocales[] pis = SupportedLocales.values();
		final CharSequence[] ret = new CharSequence[pis.length];
		for (int i = 0; i < pis.length; i++) {
			ret[i] = pis[i].getValue();
		}
		return ret;
	}

}

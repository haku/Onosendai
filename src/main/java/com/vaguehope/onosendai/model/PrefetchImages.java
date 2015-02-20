package com.vaguehope.onosendai.model;

import com.vaguehope.onosendai.util.Titleable;

public enum PrefetchImages implements Titleable {
	NO("no", "No"), //ES
	WIFI_ONLY("wifi_only", "WiFi Only"), //ES
	ALWAYS("always", "Always"); //ES

	private final String value;
	private final String title;

	private PrefetchImages (final String value, final String title) {
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
		final PrefetchImages[] pis = PrefetchImages.values();
		final CharSequence[] ret = new CharSequence[pis.length];
		for (int i = 0; i < pis.length; i++) {
			ret[i] = pis[i].getUiTitle();
		}
		return ret;
	}

	public static CharSequence[] prefEntryValues () {
		final PrefetchImages[] pis = PrefetchImages.values();
		final CharSequence[] ret = new CharSequence[pis.length];
		for (int i = 0; i < pis.length; i++) {
			ret[i] = pis[i].getValue();
		}
		return ret;
	}

	public static PrefetchImages parseValue (final String value) {
		if (value == null) return null;
		final PrefetchImages[] pis = PrefetchImages.values();
		for (int i = 0; i < pis.length; i++) {
			if (value.equals(pis[i].getValue())) return pis[i];
		}
		return null;
	}

}

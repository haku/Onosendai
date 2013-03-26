package com.vaguehope.onosendai.config;

import java.util.Locale;

public enum InternalColumnType {

	LATER();

	public boolean matchesColumn(final Column col) {
		return name().equalsIgnoreCase(col.getResource());
	}

	public static InternalColumnType parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}

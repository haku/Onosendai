package com.vaguehope.onosendai.config;

import java.util.Locale;

import com.vaguehope.onosendai.util.Titleable;

public enum InternalColumnType implements Titleable {

	LATER("Reading List"); //ES

	private final String title;

	private InternalColumnType(final String title) {
		this.title = title;
	}

	@Override
	public String getUiTitle () {
		return this.title;
	}

	public boolean matchesColumn(final Column col) {
		return name().equalsIgnoreCase(col.getResource());
	}

	public static InternalColumnType fromColumn(final Column col) {
		for (final InternalColumnType t : InternalColumnType.values()) {
			if (t.matchesColumn(col)) return t;
		}
		return null;
	}

	public static InternalColumnType parse (final String s) {
		return valueOf(s.toUpperCase(Locale.UK));
	}

}

package com.vaguehope.onosendai.config;

import java.util.Collection;
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
		for (final ColumnFeed cf : col.getFeeds()) {
			if (matchesFeed(cf)) return true;
		}
		return false;
	}

	public boolean matchesFeed (final ColumnFeed cf) {
		return name().equalsIgnoreCase(cf.getResource());
	}

	public ColumnFeed findInFeeds(final Collection<ColumnFeed> feeds) {
		if (feeds == null) return null;
		for (final ColumnFeed cf : feeds) {
			if (matchesFeed(cf)) return cf;
		}
		return null;
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

package com.vaguehope.onosendai.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {

	private static final int MINUTES_IN_HOUR = 60;

	private TimeParser () {
		throw new AssertionError();
	}

	private static final Pattern DURATION_PATTERN = Pattern.compile("^(?:([\\d]+)hours?)?\\s*(?:([\\d]+)mins?)?$", Pattern.CASE_INSENSITIVE);

	/**
	 * @return duration in minutes or less than 0 if invalid.
	 */
	public static int parseDuration (final String s) {
		if (s == null || s.isEmpty()) return 0;
		final String t = s.trim();
		if (t.isEmpty()) return 0;
		Matcher m = DURATION_PATTERN.matcher(t);
		if (m.matches()) {
			int hours = parseInt(m.group(1));
			int mins = parseInt(m.group(2));
			return (hours * MINUTES_IN_HOUR) + mins;
		}
		return -1;
	}

	private static int parseInt (final String s) {
		if (s == null) return 0;
		return Integer.parseInt(s);
	}

}

package com.vaguehope.onosendai.util;

public final class StringHelper {

	private static final String ELIPSE = "...";

	private StringHelper () {
		throw new AssertionError();
	}

	public static boolean isEmpty (final String s) {
		return s == null || s.isEmpty();
	}

	public static String maxLength(final String s, final int len) {
		if (s.length() < len) return s;
		return s.substring(0, len - ELIPSE.length()) + ELIPSE;
	}

}

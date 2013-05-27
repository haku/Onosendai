package com.vaguehope.onosendai.util;

public final class StringHelper {

	private StringHelper () {
		throw new AssertionError();
	}

	public static boolean isEmpty (final String s) {
		return s == null || s.isEmpty();
	}

}

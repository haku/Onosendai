package com.vaguehope.onosendai.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringHelper {

	public static final Pattern URL_PATTERN = Pattern.compile("\\(?\\b(https?://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]");

	public static final Pattern MENTIONS_PATTERN = Pattern.compile("\\B@([a-zA-Z0-9_]{1,15})");

	private static final String ELIPSE = "...";

	private StringHelper () {
		throw new AssertionError();
	}

	public static boolean isEmpty (final String s) {
		return s == null || s.isEmpty();
	}

	public static boolean notEmpty (final String s) {
		return s != null && !s.isEmpty();
	}

	public static String maxLength(final String s, final int len) {
		if (s.length() < len) return s;
		return s.substring(0, len - ELIPSE.length()) + ELIPSE;
	}

	public static String maxLengthEnd(final String s, final int len) {
		if (s.length() <= len) return s;
		return ELIPSE + s.substring(s.length() - (len - ELIPSE.length()));
	}

	public static boolean caseInsensitiveStartsWith(final String s, final String lookFor) {
		if (s == null) return lookFor == null;
		if (lookFor == null) return false;
		return s.toLowerCase(Locale.UK).startsWith(lookFor.toLowerCase(Locale.UK));
	}

	public static boolean safeContainsIgnoreCase (final String s, final String lookFor) {
		if (s == null) return lookFor == null;
		if (lookFor == null) return false;
		return s.toLowerCase(Locale.UK).contains(lookFor.toLowerCase(Locale.UK));
	}

	public static String firstLine(final String s) {
		if (s == null) return s;
		final int x = s.indexOf('\n');
		return x >= 0 ? s.substring(0, x) : s;
	}

	public static String replaceOnce (final String str, final String from, final String to) {
		if (isEmpty(str) || isEmpty(from) || to == null) return str;
		final int x = str.indexOf(from);
		if (x < 0) return str;
		return new StringBuilder(str.length() + to.length() - from.length())
				.append(str.substring(0, x))
				.append(to)
				.append(str.substring(x + from.length()))
				.toString();
	}

	public static String addSuffexIfCaseInsensitiveMissing (final String str, final String suffex) {
		if (str == null) return null;
		if (suffex == null) throw new IllegalArgumentException("Suffex must not be null.");
		if (str.toLowerCase(Locale.ENGLISH).endsWith(suffex.toLowerCase(Locale.ENGLISH))) return str;
		return str + suffex;
	}

	public static List<String> extractPattern (final Pattern pattern, final String input) {
		if (input == null || input.isEmpty()) return Collections.emptyList();
		List<String> occurances = null;
		final Matcher m = pattern.matcher(input);
		while (m.find()) {
			final String g = m.group(1);
			if (occurances == null) occurances = new ArrayList<String>();
			occurances.add(g);
		}
		return occurances != null ? occurances : Collections.<String>emptyList();
	}

}

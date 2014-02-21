package com.vaguehope.onosendai.util;

public final class ExcpetionHelper {

	private ExcpetionHelper () {
		throw new AssertionError();
	}

	public static String causeTrace (final Throwable t) {
		return causeTrace(t, "\n");
	}

	public static String causeTrace (final Throwable t, final String sep) {
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		Throwable c = t;
		while (c != null) {
			if (!first) sb.append(sep).append("caused by: ");
			sb.append(String.valueOf(c));
			c = c.getCause();
			first = false;
		}
		return sb.toString();
	}

}

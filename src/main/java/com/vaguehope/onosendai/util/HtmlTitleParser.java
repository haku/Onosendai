package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Html;
import android.text.Spanned;

import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;

public enum HtmlTitleParser implements HttpStreamHandler<Spanned> {
	INSTANCE;

	private static final String CHARSET = "charset=";
	private static final int MAX_SEARCH_LENGTH = 10 * 1024; // If the </title> is more than this into the page then... well dam.
	private static final Pattern TITLE_REGEX = Pattern.compile("^.*<title>(.*)</title>.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@Override
	public void onError (final Exception e) {/* Unused. */}

	@Override
	public Spanned handleStream (final URLConnection connection, final InputStream is, final int contentLength) throws IOException {
		final String charset = parseCharset(connection.getHeaderField("Content-Type"));
		final Matcher m = TITLE_REGEX.matcher(IoHelper.toString(is, MAX_SEARCH_LENGTH, charset));
		if (m.matches()) return Html.fromHtml(m.group(1));
		return null;
	}

	// e.g. Content-Type: text/html; charset=ISO-8859-1
	private static String parseCharset (final String contentType) {
		if (contentType == null) return null;
		for (String part : contentType.split(";")) {
			if (part != null) {
				part = part.trim();
				if (part.startsWith(CHARSET)) {
					return part.substring(CHARSET.length()).trim();
				}
			}
		}
		return null;
	}

}

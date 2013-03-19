package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaguehope.onosendai.model.Tweet;

public final class PayloadUtils {

	// http://www.regular-expressions.info/unicode.html

	private static final Pattern URL_PATTERN = Pattern.compile("\\(?\\b(https?://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]");
	private static final Pattern HASHTAG_PATTERN = Pattern.compile(
			"\\B([#|\uFF03][a-z0-9_\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u3040-\\u309F\\u30A0-\\u30FF]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern USERS_PATTERN = Pattern.compile("\\B@([a-zA-Z0-9_]{1,15})");


	private PayloadUtils () {
		throw new AssertionError();
	}

	public static PayloadList extractPayload (final Tweet tweet) {
		List<Payload> ret = new ArrayList<Payload>();
		extractUrls(tweet.getBody(), ret);
		extractHashTags(tweet.getBody(), ret);
		// TODO @usernames
		return new PayloadList(ret);
	}

	private static void extractUrls (final String text, final List<Payload> result) {
		if (text == null || text.isEmpty()) return;
		Matcher m = URL_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			if (g.startsWith("(") && g.endsWith(")")) g = g.substring(1, g.length() - 1);
			result.add(new LinkPayload(g));
		}
	}

	private static void extractHashTags (final String text, final List<Payload> result) {
		if (text == null || text.isEmpty()) return;
		Matcher m = HASHTAG_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			result.add(new HashTagPayload(g));
		}
	}

}

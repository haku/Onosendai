package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaguehope.onosendai.model.Tweet;

public final class PayloadUtils {

	private static final String URL_REGEX = "\\(?\\b(https?://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
	private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

	private PayloadUtils () {
		throw new AssertionError();
	}

	public static PayloadList extractPayload (final Tweet tweet) {
		List<Payload> ret = new ArrayList<Payload>();
		extractUrls(tweet.getBody(), ret);
		return new PayloadList(ret);
	}

	private static void extractUrls (final String text, final List<Payload> result) {
		Matcher m = URL_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			if (g.startsWith("(") && g.endsWith(")")) g = g.substring(1, g.length() - 1);
			result.add(new LinkPayload(g));
		}
	}

}

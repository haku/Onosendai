package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.LogWrapper;

public final class PayloadUtils {

	// http://www.regular-expressions.info/unicode.html

	private static final Pattern URL_PATTERN = Pattern.compile("\\(?\\b(https?://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]");
	private static final Pattern HASHTAG_PATTERN = Pattern.compile(
			"\\B([#|\uFF03][a-z0-9_\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u3040-\\u309F\\u30A0-\\u30FF]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern MENTIONS_PATTERN = Pattern.compile("\\B@([a-zA-Z0-9_]{1,15})");

	private static final LogWrapper LOG = new LogWrapper("PU");

	private PayloadUtils () {
		throw new AssertionError();
	}

	public static PayloadList extractPayload (final Tweet tweet) {
		Set<Payload> set = new LinkedHashSet<Payload>();
		convertMeta(tweet, set);
		extractUrls(tweet, set);
		extractHashTags(tweet, set);
		extractMentions(tweet, set);
		List<Payload> sorted = new ArrayList<Payload>(set);
		Collections.sort(sorted, Payload.TYPE_TITLE_COMP);
		return new PayloadList(sorted);
	}

	private static void convertMeta (final Tweet tweet, final Set<Payload> ret) {
		try {
			List<Meta> metas = tweet.parseMeta();
			if (metas == null) return;
			for (Meta meta : metas) {
				Payload payload = metaToPayload(meta);
				if (payload != null) {
					ret.add(payload);
				}
				else {
					LOG.e("Unknown meta type: %s", meta.getType());
				}
			}
		}
		catch (JSONException e) {
			LOG.e("Failed to parse tweet meta: %s", e.toString());
		}
	}

	private static Payload metaToPayload (final Meta meta) {
		switch (meta.getType()) {
			case MEDIA:
				return new MediaPayload(meta);
			case HASHTAG:
				return new HashTagPayload(meta);
			case MENTION:
				return new MentionPayload(meta);
			default:
				return null;
		}
	}

	private static void extractUrls (final Tweet tweet, final Set<Payload> ret) {
		String text = tweet.getBody();
		if (text == null || text.isEmpty()) return;
		Matcher m = URL_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			if (g.startsWith("(") && g.endsWith(")")) g = g.substring(1, g.length() - 1);
			ret.add(new LinkPayload(g));
		}
	}

	private static void extractHashTags (final Tweet tweet, final Set<Payload> set) {
		String text = tweet.getBody();
		if (text == null || text.isEmpty()) return;
		Matcher m = HASHTAG_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			set.add(new HashTagPayload(g));
		}
	}

	private static void extractMentions (final Tweet tweet, final Set<Payload> set) {
		set.add(new MentionPayload('@' + tweet.getUsername()));
		String text = tweet.getBody();
		if (text == null || text.isEmpty()) return;
		Matcher m = MENTIONS_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			set.add(new MentionPayload(g));
		}
	}

}

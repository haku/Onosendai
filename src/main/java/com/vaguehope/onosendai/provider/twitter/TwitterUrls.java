package com.vaguehope.onosendai.provider.twitter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.StringHelper;

public final class TwitterUrls {

	private static final String TWEET_URL_TEMPLATE = "https://twitter.com/%s/status/%s";
	private static final String HASHTAG_URL_TEMPLATE = "https://twitter.com/search?q=%s";
	private static final Pattern TWEET_URL_PATTERN = Pattern.compile("^https?://(?:mobile\\.)?twitter.com/([^/]+)/status/([0-9]+)[^/]*$");
	private static final String PROFILE_URL_TEMPLATE = "https://twitter.com/%s";

	private TwitterUrls () {
		throw new AssertionError();
	}

	public static String tweet (final Tweet tweet) {
		return String.format(TWEET_URL_TEMPLATE,
				StringHelper.firstLine(tweet.getUsername()),
				tweet.getSid());
	}

	public static String hashtag (final String hashtag) {
		return String.format(HASHTAG_URL_TEMPLATE, Uri.encode(hashtag));
	}

	public static String readTweetSidFromUrl (final String url) {
		final Matcher m = TWEET_URL_PATTERN.matcher(url);
		if (m.matches()) return m.group(2);
		return null;
	}

	public static String profileUrl (final String username) {
		return String.format(PROFILE_URL_TEMPLATE, username);
	}

}

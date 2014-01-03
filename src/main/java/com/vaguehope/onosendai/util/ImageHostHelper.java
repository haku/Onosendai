package com.vaguehope.onosendai.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageHostHelper {

	private static final Pattern INSTAGRAM_URL = Pattern.compile("^http://instagram.com/p/(.+)/$");
	private static final Pattern TWITPIC_URL = Pattern.compile("^http://twitpic.com/(.+)$");
	private static final Pattern IMGUR_URL = Pattern.compile("^http://(?:i\\.)?imgur.com/(.+?)(?:\\..+)?$");
	private static final Pattern YFROG_URL = Pattern.compile("^http://yfrog.com/(.+)$");

	private ImageHostHelper () {
		throw new AssertionError();
	}

	public static String thumbUrl (final String linkUrl) {
		{
			final Matcher m = INSTAGRAM_URL.matcher(linkUrl);
			if (m.matches()) return linkUrl + "media/";
		}

		{
			final Matcher m = TWITPIC_URL.matcher(linkUrl);
			if (m.matches()) return "http://twitpic.com/show/thumb/" + m.group(1) + ".jpg";
		}

		{ // https://api.imgur.com/models/image
			final Matcher m = IMGUR_URL.matcher(linkUrl);
			if (m.matches()) {
				final String imgId = m.group(1);
				if (imgId.startsWith("a/") || imgId.startsWith("gallery/")) return null;
				return "http://i.imgur.com/" + imgId + "l.jpg";
			}
		}

		{
			final Matcher m = YFROG_URL.matcher(linkUrl);
			if (m.matches()) return "http://yfrog.com/" + m.group(1) + ":small";
		}

		return null;
	}

}

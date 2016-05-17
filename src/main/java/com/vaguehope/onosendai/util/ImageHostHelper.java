package com.vaguehope.onosendai.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageHostHelper {

	private static final int MAX_THUMBS = 5;

	private static final Pattern INSTAGRAM_URL = Pattern.compile("^https?://(?:www\\.)?instagram.com/p/([^/]+)/?$");
	private static final Pattern TWITPIC_URL = Pattern.compile("^https?://twitpic.com/(.+)$");
	private static final Pattern IMGUR_URL = Pattern.compile("^https?://(?:i\\.)?imgur.com/(.+?)(?:\\..+)?$");
	private static final Pattern YFROG_URL = Pattern.compile("^https?://yfrog.com/(.+)$");
	private static final Pattern TWIPPLE_URL = Pattern.compile("^https?://p.twipple.jp/(.+)$");

	private ImageHostHelper () {
		throw new AssertionError();
	}

	public static List<String> thumbUrl (final String linkUrl, final boolean hdMedia) {
		{ // http://instagram.com/developer/embedding
			final Matcher m = INSTAGRAM_URL.matcher(linkUrl);
			if (m.matches()) {
				final String thumbUrl = "https://instagram.com/p/" + m.group(1) + "/media/?size=" + (hdMedia ? "l" : "m");
				return Collections.singletonList(thumbUrl);
			}
		}

		{ // http://dev.twitpic.com/docs/thumbnails/
			final Matcher m = TWITPIC_URL.matcher(linkUrl);
			if (m.matches()) {
				final String thumbUrl = "https://twitpic.com/show/thumb/" + m.group(1) + ".jpg";
				return Collections.singletonList(thumbUrl);
			}
		}

		{ // https://api.imgur.com/models/image
			final Matcher m = IMGUR_URL.matcher(linkUrl);
			if (m.matches()) {
				String imgIds = m.group(1);

				// Assumes paths with single slash are galleries.
				final int firstSlash = imgIds.indexOf('/');
				if (firstSlash >= 0) {
					final int lastSlash = imgIds.lastIndexOf('/');
					if (lastSlash > firstSlash) { // i.e. not the same.
						imgIds = imgIds.substring(lastSlash + 1);
					}
					else {
						return null;
					}
				}

				if (imgIds.startsWith("a/") || imgIds.startsWith("gallery/")) return null;

				final List<String> ret = new ArrayList<String>(1);
				for (final String imgId : imgIds.split(",")) {
					final String thumbUrl = "https://i.imgur.com/" + imgId + (hdMedia ? "h" : "l") + ".jpg";
					ret.add(thumbUrl);
					if (ret.size() >= MAX_THUMBS) break;
				}
				return ret;
			}
		}

		{ // http://twitter.yfrog.com/page/api#a5
			final Matcher m = YFROG_URL.matcher(linkUrl);
			if (m.matches()) {
				final String thumbUrl = "http://yfrog.com/" + m.group(1) + (hdMedia ? ":medium" : ":small");
				return Collections.singletonList(thumbUrl);
			}
		}

		{ // http://p.twipple.jp/wiki/API_Thumbnail
			final Matcher m = TWIPPLE_URL.matcher(linkUrl);
			if (m.matches()) {
				final String thumbUrl = "http://p.twipple.jp/show/large/" + m.group(1);
				return Collections.singletonList(thumbUrl);
			}
		}

		return null;
	}

}

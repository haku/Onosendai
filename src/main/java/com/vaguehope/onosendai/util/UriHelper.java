package com.vaguehope.onosendai.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import android.net.Uri;

public final class UriHelper {

	public static CharSequence uriFileName (final Uri item) {
		if (item == null) return null;

		final List<String> parts = item.getPathSegments();
		if (parts.size() < 1) return null;

		return parts.get(parts.size() - 1);
	}

	public static CharSequence uriFileName (final URI uri) {
		return uriFileName(uriToUri(uri));
	}

	public static CharSequence uriFileName (final URL url) {
		try {
			return uriFileName(url.toURI());
		}
		catch (final URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static Uri uriToUri (final URI uri) {
		return new Uri.Builder().scheme(uri.getScheme())
				.encodedAuthority(uri.getRawAuthority())
				.encodedPath(uri.getRawPath())
				.query(uri.getRawQuery())
				.fragment(uri.getRawFragment())
				.build();
	}

}

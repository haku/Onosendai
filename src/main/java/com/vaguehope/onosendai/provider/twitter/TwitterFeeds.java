package com.vaguehope.onosendai.provider.twitter;

import java.util.Locale;

public final class TwitterFeeds {

	static final String PREFIX_LISTS = "lists/";
	static final String PREFIX_FAVORITES = "favorites/";
	static final String PREFIX_SEARCH = "search/";

	private TwitterFeeds () {
		throw new AssertionError();
	}

	public static TwitterFeed parse (final String resource) {
		if (resource.startsWith(PREFIX_LISTS)) {
			final String slug = resource.substring(PREFIX_LISTS.length());
			return new ListFeed(slug);
		}
		else if (resource.startsWith(PREFIX_SEARCH)) {
			final String term = resource.substring(PREFIX_SEARCH.length());
			return new SearchFeed(term);
		}
		else if (resource.startsWith(PREFIX_FAVORITES)) {
			final String screenName = resource.substring(PREFIX_FAVORITES.length());
			return new FavoritesFeed(screenName);
		}
		return MainFeeds.valueOf(resource.toUpperCase(Locale.UK));
	}

}

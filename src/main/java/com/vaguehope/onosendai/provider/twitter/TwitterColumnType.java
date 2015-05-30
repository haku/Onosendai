package com.vaguehope.onosendai.provider.twitter;

import com.vaguehope.onosendai.util.Titleable;

public enum TwitterColumnType implements Titleable {

	TIMELINE("Home Timeline"), //ES
	MENTIONS("Mentions"), //ES
	ME("Me"), //ES
	FAVORITES("My Favorites"), //ES
	LIST("My List..."), //ES
	ANOTHERS_LIST("Another's List..."), //ES
	SEARCH("Search..."); //ES

	private final String title;

	private TwitterColumnType (final String title) {
		this.title = title;
	}

	@Override
	public String getUiTitle () {
		return this.title;
	}

	public String getResource() {
		switch (this) {
			case TIMELINE:
				return MainFeeds.TIMELINE.name();
			case MENTIONS:
				return MainFeeds.MENTIONS.name();
			case ME:
				return MainFeeds.ME.name();
			case FAVORITES:
				return MainFeeds.FAVORITES.name();
			case LIST:
			case ANOTHERS_LIST:
				return TwitterFeeds.PREFIX_LISTS;
			case SEARCH:
				return TwitterFeeds.PREFIX_SEARCH;
			default:
				return null;
		}
	}

}

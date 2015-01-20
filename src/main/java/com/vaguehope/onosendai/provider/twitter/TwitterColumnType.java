package com.vaguehope.onosendai.provider.twitter;

import com.vaguehope.onosendai.util.Titleable;

public enum TwitterColumnType implements Titleable {

	TIMELINE("Home Timeline"),
	MENTIONS("Mentions"),
	LIST("My List..."),
	ANOTHERS_LIST("Another's List..."),
	SEARCH("Search...");

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

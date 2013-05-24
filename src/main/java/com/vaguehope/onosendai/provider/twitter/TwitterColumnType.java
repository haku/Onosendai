package com.vaguehope.onosendai.provider.twitter;

public enum TwitterColumnType {

	TIMELINE("Home Timeline"),
	MENTIONS("Mentions"),
	LIST("List..."),
	SEARCH("Search...");

	private final String title;

	private TwitterColumnType (final String title) {
		this.title = title;
	}

	public String getTitle () {
		return this.title;
	}

	public String getResource() {
		switch (this) {
			case TIMELINE:
				return MainFeeds.TIMELINE.name();
			case MENTIONS:
				return MainFeeds.MENTIONS.name();
			case LIST:
				return TwitterFeeds.PREFIX_LISTS;
			case SEARCH:
				return TwitterFeeds.PREFIX_SEARCH;
			default:
				return null;
		}
	}

}

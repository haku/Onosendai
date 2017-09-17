package com.vaguehope.onosendai.provider.mastodon;

import com.vaguehope.onosendai.util.Titleable;

public enum MastodonColumnType implements Titleable {

	TIMELINE("Home Timeline", "TIMELINE"), //ES
	ME("Me", "ME"), //ES
	FAVORITES("My Favorites", "FAVORITES"); //ES

	private final String title;
	private final String resoure;

	private MastodonColumnType (final String title, final String resoure) {
		this.title = title;
		this.resoure = resoure;
	}

	@Override
	public String getUiTitle () {
		return this.title;
	}

	public String getResource() {
		return this.resoure;
	}

	public static MastodonColumnType parseResource (final String resource) {
		for (MastodonColumnType type : values()) {
			if (type.getResource().equalsIgnoreCase(resource)) return type;
		}
		return null;
	}

}

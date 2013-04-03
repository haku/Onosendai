package com.vaguehope.onosendai.provider.successwhale;

import com.vaguehope.onosendai.provider.NetworkType;

public enum ItemAction {

	RETWEET(NetworkType.TWITTER, "retweet"),
	FAVORITE(NetworkType.TWITTER, "favorite"),
	UNFAVORITE(NetworkType.TWITTER, "unfavorite"),
	LIKE(NetworkType.FACEBOOK, "like"),
	UNLIKE(NetworkType.FACEBOOK, "unlike");

	private final NetworkType networkType;
	private final String action;

	private ItemAction (final NetworkType networkType, final String action) {
		this.networkType = networkType;
		this.action = action;
	}

	public NetworkType getNetworkType () {
		return this.networkType;
	}

	public String getAction () {
		return this.action;
	}

}

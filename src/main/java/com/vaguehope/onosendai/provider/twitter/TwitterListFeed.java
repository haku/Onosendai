package com.vaguehope.onosendai.provider.twitter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterListFeed implements TwitterFeed {

	private final String slug;

	public TwitterListFeed (final String slug) {
		this.slug = slug;
	}

	@Override
	public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
		return t.getUserListStatuses(t.getId(), this.slug, paging);
	}

	@Override
	public int recommendedFetchCount () {
		return 50;
	}

}

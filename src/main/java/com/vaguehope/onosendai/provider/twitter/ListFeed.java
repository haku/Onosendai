package com.vaguehope.onosendai.provider.twitter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.TweetList;

class ListFeed implements FeedGetter {

	private final String slug;

	public ListFeed (final String slug) {
		this.slug = slug;
	}

	@Override
	public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
		return t.getUserListStatuses(t.getId(), this.slug, paging);
	}

	@Override
	public int recommendedFetchCount () {
		return C.TWITTER_LIST_MAX_FETCH;
	}

	@Override
	public TweetList getTweets (final Account account, final Twitter t, final long sinceId, final boolean hdMedia) throws TwitterException {
		return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId, hdMedia);
	}

	@Override
	public String toString () {
		return "list{" + this.slug + "}";
	}

}
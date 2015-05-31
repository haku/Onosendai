package com.vaguehope.onosendai.provider.twitter;

import java.util.Collection;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.StringHelper;

class FavoritesFeed implements FeedGetter {

	private final String screenName;

	public FavoritesFeed (final String screenName) {
		if (StringHelper.isEmpty(screenName)) throw new IllegalArgumentException("screenName must not be empty.");
		this.screenName = screenName;
	}

	@Override
	public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
		return t.getFavorites(this.screenName);
	}

	@Override
	public int recommendedFetchCount () {
		return C.TWITTER_ME_MAX_FETCH;
	}

	@Override
	public TweetList getTweets (final Account account, final Twitter t, final long sinceId, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId, hdMedia, extraMetas);
	}

	@Override
	public String toString () {
		return "Favorites{" + this.screenName + "}";
	}

}

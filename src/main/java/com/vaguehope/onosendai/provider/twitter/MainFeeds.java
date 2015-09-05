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

enum MainFeeds implements FeedGetter {
	TIMELINE(C.TWITTER_FETCH_COUNT_TIMELINE) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getHomeTimeline(paging);
		}
	},
	MENTIONS(C.TWITTER_FETCH_COUNT_MENTIONS) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getMentionsTimeline(paging);
		}
	},
	ME(C.TWITTER_FETCH_COUNT_ME) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getUserTimeline(paging);
		}
	},
	FAVORITES(C.TWITTER_FETCH_COUNT_FAVORITES) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getFavorites(paging);
		}
	};

	private final int recommendedFetchCount;

	private MainFeeds (final int recommendedFetchCount) {
		this.recommendedFetchCount = recommendedFetchCount;
	}

	@Override
	public int recommendedFetchCount () {
		return this.recommendedFetchCount;
	}

	@Override
	public abstract ResponseList<Status> getTweets (Twitter t, Paging paging) throws TwitterException;

	@Override
	public TweetList getTweets (final Account account, final Twitter t, final long sinceId, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId, hdMedia, extraMetas);
	}

}

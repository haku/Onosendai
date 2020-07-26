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
	TIMELINE(C.TWITTER_FETCH_COUNT_TIMELINE, false) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getHomeTimeline(paging);
		}
	},
	MENTIONS(C.TWITTER_FETCH_COUNT_MENTIONS, false) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getMentionsTimeline(paging);
		}
	},
	ME(C.TWITTER_FETCH_COUNT_ME, false) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getUserTimeline(paging);
		}
	},
	FAVORITES(C.TWITTER_FETCH_COUNT_FAVORITES, true) {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getFavorites(paging);
		}
	};

	private final int recommendedFetchCount;
	private final boolean ignoreSinceIdIfManual;

	private MainFeeds (final int recommendedFetchCount, final boolean ignoreSinceIdIfManual) {
		this.recommendedFetchCount = recommendedFetchCount;
		this.ignoreSinceIdIfManual = ignoreSinceIdIfManual;
	}

	@Override
	public int recommendedFetchCount () {
		return this.recommendedFetchCount;
	}

	@Override
	public abstract ResponseList<Status> getTweets (Twitter t, Paging paging) throws TwitterException;

	@Override
	public TweetList getTweets (final Account account, final Twitter t, final long sinceId, final boolean hdMedia, final boolean manual, final Collection<Meta> extraMetas) throws TwitterException {
		long sid = sinceId;
		if (this.ignoreSinceIdIfManual && manual) sid = 0;  // 0 disabled using sinceId.
		return TwitterUtils.fetchTwitterFeed(account, t, this, sid, hdMedia, extraMetas);
	}

}

package com.vaguehope.onosendai.provider.twitter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.TweetList;

enum MainFeeds implements FeedGetter {

	TIMELINE {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getHomeTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return C.TWITTER_TIMELINE_MAX_FETCH;
		}

		@Override
		public TweetList getTweets (final Account account, final Twitter t, final long sinceId) throws TwitterException {
			return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId);
		}
	},
	MENTIONS {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getMentionsTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return C.TWITTER_MENTIONS_MAX_FETCH;
		}

		@Override
		public TweetList getTweets (final Account account, final Twitter t, final long sinceId) throws TwitterException {
			return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId);
		}
	},
	ME {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getUserTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return C.TWITTER_ME_MAX_FETCH;
		}

		@Override
		public TweetList getTweets (final Account account, final Twitter t, final long sinceId) throws TwitterException {
			return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId);
		}
	};

	@Override
	public abstract ResponseList<Status> getTweets (Twitter t, Paging paging) throws TwitterException;

}
package com.vaguehope.onosendai.provider.twitter;

import java.util.Locale;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.C;

public enum TwitterFeeds implements TwitterFeed {
	TIMELINE {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getHomeTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return C.TWITTER_TIMELINE_MAX_FETCH;
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
	};

	private static final String PREFIX_LISTS = "lists/";

	@Override
	public abstract ResponseList<Status> getTweets (Twitter t, Paging paging) throws TwitterException;

	public static TwitterFeed parse (final String resource) {
		if (resource.startsWith(PREFIX_LISTS)) {
			String slug = resource.substring(PREFIX_LISTS.length());
			return new TwitterListFeed(slug);
		}
		return valueOf(resource.toUpperCase(Locale.UK));
	}
}

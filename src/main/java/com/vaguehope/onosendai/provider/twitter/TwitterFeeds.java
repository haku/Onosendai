package com.vaguehope.onosendai.provider.twitter;

import java.util.Locale;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public enum TwitterFeeds implements TwitterFeed {
	TIMELINE {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getHomeTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return 50;
		}
	},
	MENTIONS {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getMentionsTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return 15;
		}
	},
	ME {
		@Override
		public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
			return t.getUserTimeline(paging);
		}

		@Override
		public int recommendedFetchCount () {
			return 15;
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

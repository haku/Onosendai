package com.vaguehope.onosendai.provider.twitter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

interface FeedGetter extends TwitterFeed {

	ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException;

	int recommendedFetchCount ();

}
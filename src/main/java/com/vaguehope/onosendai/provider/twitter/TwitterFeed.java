package com.vaguehope.onosendai.provider.twitter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public interface TwitterFeed {
	
	String getName ();
	
	ResponseList<Status> getTweets (Twitter t, Paging paging) throws TwitterException;
	
}

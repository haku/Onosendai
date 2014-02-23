package com.vaguehope.onosendai.provider.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.TweetList;

public interface TwitterFeed {

	TweetList getTweets (Account account, Twitter t, final long sinceId, final boolean hdMedia) throws TwitterException;

}

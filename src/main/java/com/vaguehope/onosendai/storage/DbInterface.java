package com.vaguehope.onosendai.storage;

import java.util.List;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public interface DbInterface {

	void storeTweets(Column column, List<Tweet> tweets);
	void deleteTweet(Column column, Tweet tweet);
	List<Tweet> getTweets(int columnId, int numberOf);
	Tweet getTweetDetails(int columnId, Tweet tweet);
	Tweet getTweetDetails(int columnId, long tweetId);

	// FIXME action should be told what changed.
	void addTwUpdateListener (Runnable action);
	void removeTwUpdateListener (Runnable action);

	void storeScroll(int columnId, ScrollState state);
	ScrollState getScroll(int columnId);

}

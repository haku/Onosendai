package com.vaguehope.onosendai.storage;

import java.util.List;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public interface DbInterface extends KvStore {

	void storeTweets(Column column, List<Tweet> tweets);
	void deleteTweet(Column column, Tweet tweet);

	List<Tweet> getTweets(int columnId, int numberOf);
	List<Tweet> getTweets(int columnId, int numberOf, int[] excludeColumnIds);

	Tweet getTweetDetails(int columnId, Tweet tweet);
	Tweet getTweetDetails(int columnId, String tweetSid);
	Tweet getTweetDetails(String tweetSid);
	Tweet getTweetDetails(long tweetUid);

	void addTwUpdateListener (TwUpdateListener listener);
	void removeTwUpdateListener (TwUpdateListener listener);

	void storeScroll(int columnId, ScrollState state);
	ScrollState getScroll(int columnId);

	interface TwUpdateListener {
		void columnChanged(int columnId);
	}

}

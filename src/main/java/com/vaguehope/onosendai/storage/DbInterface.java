package com.vaguehope.onosendai.storage;

import java.util.List;
import java.util.Set;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public interface DbInterface extends KvStore {

	void storeTweets(Column column, List<Tweet> tweets);
	void deleteTweet(Column column, Tweet tweet);
	void deleteTweets(Column column);

	List<Tweet> getTweets(int columnId, int numberOf);
	List<Tweet> getTweets(int columnId, int numberOf, Set<Integer> excludeColumnIds);

	Tweet getTweetDetails(int columnId, Tweet tweet);
	Tweet getTweetDetails(int columnId, String tweetSid);
	Tweet getTweetDetails(String tweetSid);
	Tweet getTweetDetails(long tweetUid);

	int getScrollUpCount(Column column);
	int getScrollUpCount(int columnId, Set<Integer> excludeColumnIds, ScrollState scroll);

	void addTwUpdateListener (TwUpdateListener listener);
	void removeTwUpdateListener (TwUpdateListener listener);

	void storeScroll(int columnId, ScrollState state);
	ScrollState getScroll(int columnId);

	interface TwUpdateListener {
		void columnChanged(int columnId);
	}

}

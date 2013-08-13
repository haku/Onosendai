package com.vaguehope.onosendai.storage;

import java.util.List;
import java.util.Set;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public interface DbInterface extends KvStore {

	void storeTweets(Column column, List<Tweet> tweets);
	void deleteTweet(Column column, Tweet tweet);
	void deleteTweets(Column column);

	List<Tweet> getTweets(int columnId, int numberOf);
	List<Tweet> getTweets(int columnId, int numberOf, Set<Integer> excludeColumnIds);

	List<Tweet> findTweetsWithMeta (MetaType metaType, String data, final int numberOf);

	Tweet getTweetDetails(int columnId, Tweet tweet);
	Tweet getTweetDetails(int columnId, String tweetSid);
	Tweet getTweetDetails(String tweetSid);
	Tweet getTweetDetails(long tweetUid);

	int getUnreadCount(Column column);
	int getUnreadCount(int columnId, Set<Integer> excludeColumnIds, ScrollState scroll);
	int getScrollUpCount(Column column);
	int getScrollUpCount(int columnId, Set<Integer> excludeColumnIds, ScrollState scroll);

	void addTwUpdateListener (TwUpdateListener listener);
	void removeTwUpdateListener (TwUpdateListener listener);

	void storeScroll(int columnId, ScrollState state);
	ScrollState getScroll(int columnId);

	void notifyTwListenersColumnState (final int columnId, final ColumnState state);

	enum ColumnState {
		UPDATE_RUNNING,
		UPDATE_OVER;
	}

	interface TwUpdateListener {
		void columnChanged(int columnId);
		void columnStatus(int columnId, ColumnState state);
	}

	void addPostToOutput (OutboxTweet ot);
	void updateOutboxEntry (OutboxTweet ot);
	List<OutboxTweet> getOutboxEntries();
	List<OutboxTweet> getOutboxEntries(OutboxTweetStatus status);
	void deleteFromOutbox(OutboxTweet ot);

	void addOutboxListener (OutboxListener listener);
	void removeOutboxListener (OutboxListener listener);

	interface OutboxListener {
		void outboxChanged();
	}

	void vacuum ();

}

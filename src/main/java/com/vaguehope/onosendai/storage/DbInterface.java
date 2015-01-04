package com.vaguehope.onosendai.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public interface DbInterface extends KvStore {

	void storeTweets(Column column, List<Tweet> tweets);
	void storeTweets(int columnId, List<Tweet> tweets);
	void appendToTweet(Tweet tweet, Meta meta);
	void deleteTweet(Column column, Tweet tweet);
	void deleteTweets(Column column);

	List<Tweet> getTweets(int columnId, int numberOf);
	List<Tweet> getTweets(int columnId, int numberOf, Set<Integer> excludeColumnIds);

	Cursor getTweetsCursor(int columnId);
	Cursor getTweetsCursor(int columnId, boolean withInlineMediaOnly);
	Cursor getTweetsCursor(int columnId, Set<Integer> excludeColumnIds, boolean withInlineMediaOnly);

	List<Tweet> getTweetsSinceTime (final int columnId, final long earliestTime, final int numberOf);
	List<Tweet> getTweetsWithSid (String sid);
	List<Tweet> findTweetsWithMeta (MetaType metaType, String data, final int numberOf);
	List<Tweet> searchTweets(String searchTerm, List<Column> columns, int numberOf);

	Tweet getTweetDetails(int columnId, Tweet tweet);
	Tweet getTweetDetails(int columnId, String tweetSid);
	Tweet getTweetDetails(String tweetSid);
	Tweet getTweetDetails(long tweetUid);

	List<String> getUsernames(int numberOf);
	List<String> getUsernames(String prefix, int numberOf);
	List<String> getHashtags(String prefix, int numberOf);

	int getUnreadCount(Column column);
	int getUnreadCount(int columnId, Set<Integer> excludeColumnIds, ScrollState scroll);
	int getScrollUpCount(Column column);
	int getScrollUpCount(int columnId, Set<Integer> excludeColumnIds, boolean withInlineMediaOnly, ScrollState scroll);

	void addTwUpdateListener (TwUpdateListener listener);
	void removeTwUpdateListener (TwUpdateListener listener);

	void storeScroll(int columnId, ScrollState state);
	void storeUnreadTime(int columnId, long unreadTime);
	void mergeAndStoreScrolls(Map<Column, ScrollState> colToSs);
	ScrollState getScroll(int columnId);

	void notifyTwListenersColumnState (final int columnId, final ColumnState state);

	enum ColumnState {
		UPDATE_RUNNING,
		UPDATE_OVER;
	}

	interface TwUpdateListener {
		void columnChanged(int columnId);
		void columnStatus(int columnId, ColumnState state);
		void unreadChanged(int columnId);
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

	void housekeep ();

	long getTotalTweetsEverSeen ();
	double getTweetsPerHour (int columnId);

}

package com.vaguehope.onosendai.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.database.Cursor;
import android.support.v4.util.Pair;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public interface DbInterface extends KvStore {

	enum DiscardOrder {
		/**
		 * Delete oldest published time.
		 */
		FIRST_PUBLISHED,
		/**
		 * Delete first written to local DB.
		 */
		FIRST_DOWNLOADED;
	}

	void storeTweets(Column column, List<Tweet> tweets, DiscardOrder discardOrder);
	void storeTweets(int columnId, List<Tweet> tweets, DiscardOrder discardOrder);
	void appendToTweet(Tweet tweet, Meta meta);
	void deleteTweet(Column column, Tweet tweet);
	void deleteTweets(Column column);
	void updateTweetFiltered(List<Pair<Long, Boolean>> uidToFiltered);

	enum Selection {
		ALL,
		FILTERED;
	}

	List<Tweet> getTweets(int columnId, int numberOf, Selection selection);
	List<Tweet> getTweets(int columnId, int numberOf, Selection selection,
			Set<Integer> excludeColumnIds, Set<Integer> columnsHidingRetweets,
			boolean withInlineMediaOnly, boolean excludeRetweets, boolean excludeEditable);

	Cursor getTweetsCursor(int columnId, Selection selection);
	Cursor getTweetsCursor(int columnId, Selection selection,
			Set<Integer> excludeColumnIds, Set<Integer> columnsHidingRetweets,
			boolean withInlineMediaOnly, boolean excludeRetweets, boolean excludeEditable);

	List<Tweet> getTweetsSinceTime (final int columnId, final long earliestTime, final int numberOf);
	List<Tweet> getTweetsWithSid (String sid);
	List<Tweet> findTweetsWithMeta (MetaType metaType, String data, final int numberOf);
	List<Tweet> findTweetsWithMeta (final int columnId, MetaType metaType, String data, final int numberOf);
	List<Tweet> findTweetsWithAvatarUrl (String avatarUrl, final int numberOf);
	List<Tweet> searchTweets(String searchTerm, List<Column> columns, int numberOf);

	Tweet getTweetDetails(int columnId, Tweet tweet);
	Tweet getTweetDetails(int columnId, String tweetSid);
	Tweet getTweetDetails(String tweetSid);
	Tweet getTweetDetails(long tweetUid);

	List<Meta> getTweetMetas(long tweetUid);
	List<Meta> getTweetMetasOfType(long tweetUid, MetaType metaType);

	List<String> getUsernames(String prefix, int numberOf);
	List<String> getHashtags(String prefix, int numberOf);

	/**
	 * Excludes editable.
	 */
	int getUnreadCount(Column column, Set<Integer> columnsHidingRetweets);

	int getUnreadCount(int columnId, Selection selection,
			Set<Integer> excludeColumnIds, Set<Integer> columnsHidingRetweets,
			boolean withInlineMediaOnly, boolean excludeRetweets, boolean excludeEditable,
			ScrollState scroll);

	int getScrollUpCount(int columnId, Selection selection,
			Set<Integer> excludeColumnIds, Set<Integer> columnsHidingRetweets,
			boolean withInlineMediaOnly, boolean excludeRetweets, boolean excludeEditable,
			ScrollState scroll);

	void addTwUpdateListener (TwUpdateListener listener);
	void removeTwUpdateListener (TwUpdateListener listener);

	void storeScroll(int columnId, ScrollState state);
	void storeUnreadTime(int columnId, long unreadTime);
	void mergeAndStoreScrolls(Map<Column, ScrollState> colToSs, ScrollChangeType type);
	ScrollState getScroll(int columnId);

	void notifyTwListenersColumnState (final int columnId, final ColumnState state);
	/**
	 * Returns column IDs of saves requested.
	 */
	Set<Integer> requestStoreScrollNow();

	enum ColumnState {
		/**
		 * Always called before and instead of UPDATE_RUNNING.
		 */
		NOT_STARTED,
		UPDATE_RUNNING,
		/**
		 * Always once after each UPDATE_RUNNING event.
		 */
		UPDATE_OVER;
	}

	enum ScrollChangeType {
		UNREAD,
		UNREAD_AND_SCROLL
	}

	interface TwUpdateListener {
		void columnChanged(int columnId);
		void columnStatus(int columnId, ColumnState state);
		void unreadOrScrollChanged(int columnId, ScrollChangeType type);
		/**
		 * returns columnId or null.
		 */
		Integer requestStoreScrollStateNow();
		void scrollStored(int columnId);
	}

	long addPostToOutput (OutboxTweet ot);
	void updateOutboxEntry (OutboxTweet ot);
	OutboxTweet getOutboxEntry(long uid);
	List<OutboxTweet> getUnsentOutboxEntries();
	List<OutboxTweet> getAllOutboxEntries();
	/**
	 * Returned in ascending ID order.
	 */
	List<OutboxTweet> getOutboxEntries(OutboxTweetStatus status);
	void deleteFromOutbox(OutboxTweet ot);

	void addOutboxListener (OutboxListener listener);
	void removeOutboxListener (OutboxListener listener);

	interface OutboxListener {
		void outboxChanged();
	}

	void cacheString(CachedStringGroup group, String key, String value);
	String cachedString(CachedStringGroup group, String key);

	void housekeep ();

	long getTotalTweetsEverSeen ();
	TimeRange getColumnTimeRange (int columnId);
	/**
	 * Returns an ordered map of username => number of tweets.
	 */
	Map<String, Long> getColumnUserStats(int columnId);

}

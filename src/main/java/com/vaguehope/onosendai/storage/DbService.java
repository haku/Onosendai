package com.vaguehope.onosendai.storage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.util.Pair;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;

public class DbService extends Service implements DbInterface {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onCreate () {
		super.onCreate();
		dbStart();
	}

	@Override
	public void onDestroy () {
		dbStop();
		super.onDestroy();
	}

	@Override
	public IBinder onBind (final Intent arg0) {
		return this.mBinder;
	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public DbInterface getService () {
			return DbService.this;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data store.

	private DbAdapter dbAdaptor;

	private void dbStart () {
		this.dbAdaptor = new DbAdapter(getApplicationContext());
		this.dbAdaptor.open();
	}

	private void dbStop () {
		this.dbAdaptor.close();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB methods.

	@Override
	public void storeTweets (final Column column, final List<Tweet> tweets, final DiscardOrder discardOrder) {
		this.dbAdaptor.storeTweets(column, tweets, discardOrder);
	}

	@Override
	public void storeTweets (final int columnId, final List<Tweet> tweets, final DiscardOrder discardOrder) {
		this.dbAdaptor.storeTweets(columnId, tweets, discardOrder);
	}

	@Override
	public void appendToTweet (final Tweet tweet, final Meta meta) {
		this.dbAdaptor.appendToTweet(tweet, meta);
	}

	@Override
	public void deleteTweet (final Column column, final Tweet tweet) {
		this.dbAdaptor.deleteTweet(column, tweet);
	}

	@Override
	public void deleteTweets (final Column column) {
		this.dbAdaptor.deleteTweets(column);
	}

	@Override
	public void updateTweetFiltered (final List<Pair<Long, Boolean>> uidToFiltered) {
		this.dbAdaptor.updateTweetFiltered(uidToFiltered);
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf, final Selection selection) {
		return this.dbAdaptor.getTweets(columnId, numberOf, selection);
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf, final Selection selection,
			final Set<Integer> excludeColumnIds, final Set<Integer> columnsHidingRetweets,
			final boolean withInlineMediaOnly, final boolean excludeRetweets, final boolean excludeEditable) {
		return this.dbAdaptor.getTweets(columnId, numberOf, selection,
				excludeColumnIds, columnsHidingRetweets,
				withInlineMediaOnly, excludeRetweets, excludeEditable);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId, final Selection selection) {
		return this.dbAdaptor.getTweetsCursor(columnId, selection);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId, final Selection selection,
			final Set<Integer> excludeColumnIds, final Set<Integer> columnsHidingRetweets,
			final boolean withInlineMediaOnly, final boolean excludeRetweets, final boolean excludeEditable) {
		return this.dbAdaptor.getTweetsCursor(columnId, selection,
				excludeColumnIds, columnsHidingRetweets,
				withInlineMediaOnly, excludeRetweets, excludeEditable);
	}

	@Override
	public List<Tweet> getTweetsSinceTime (final int columnId, final long earliestTime, final int numberOf) {
		return this.dbAdaptor.getTweetsSinceTime(columnId, earliestTime, numberOf);
	}

	@Override
	public List<Tweet> getTweetsWithSid (final String sid) {
		return this.dbAdaptor.getTweetsWithSid(sid);
	}

	@Override
	public List<Tweet> findTweetsWithMeta (final MetaType metaType, final String data, final int numberOf) {
		return this.dbAdaptor.findTweetsWithMeta(metaType, data, numberOf);
	}

	@Override
	public List<Tweet> findTweetsWithMeta (final int columnId, final MetaType metaType, final String data, final int numberOf) {
		return this.dbAdaptor.findTweetsWithMeta(columnId, metaType, data, numberOf);
	}

	@Override
	public List<Tweet> findTweetsWithAvatarUrl (final String avatarUrl, final int numberOf) {
		return this.dbAdaptor.findTweetsWithAvatarUrl(avatarUrl, numberOf);
	}

	@Override
	public List<Tweet> searchTweets (final String searchTerm, final List<Column> columns, final int numberOf) {
		return this.dbAdaptor.searchTweets(searchTerm, columns, numberOf);
	}

	@Override
	public Tweet getTweetDetails (final int columnId, final Tweet tweet) {
		return this.dbAdaptor.getTweetDetails(columnId, tweet);
	}

	@Override
	public Tweet getTweetDetails (final int columnId, final String tweetSid) {
		return this.dbAdaptor.getTweetDetails(columnId, tweetSid);
	}

	@Override
	public Tweet getTweetDetails (final String tweetSid) {
		return this.dbAdaptor.getTweetDetails(tweetSid);
	}

	@Override
	public Tweet getTweetDetails (final long tweetUid) {
		return this.dbAdaptor.getTweetDetails(tweetUid);
	}

	@Override
	public List<Meta> getTweetMetas (final long tweetUid) {
		return this.dbAdaptor.getTweetMetas(tweetUid);
	}

	@Override
	public List<Meta> getTweetMetasOfType (final long tweetUid, final MetaType metaType) {
		return this.dbAdaptor.getTweetMetasOfType(tweetUid, metaType);
	}

	@Override
	public List<String> getUsernames (final String prefix, final int numberOf) {
		return this.dbAdaptor.getUsernames(prefix, numberOf);
	}

	@Override
	public List<String> getHashtags (final String prefix, final int numberOf) {
		return this.dbAdaptor.getHashtags(prefix, numberOf);
	}

	@Override
	public int getUnreadCount (final Column column, final Set<Integer> columnsHidingRetweets) {
		return this.dbAdaptor.getUnreadCount(column, columnsHidingRetweets);
	}

	@Override
	public int getUnreadCount(final int columnId, final Selection selection,
			final Set<Integer> excludeColumnIds, final Set<Integer> columnsHidingRetweets,
			final boolean withInlineMediaOnly, final boolean excludeRetweets, final boolean excludeEditable,
			final ScrollState scroll) {
		return this.dbAdaptor.getUnreadCount(columnId, selection,
				excludeColumnIds, columnsHidingRetweets,
				withInlineMediaOnly, excludeRetweets, excludeEditable,
				scroll);
	}

	@Override
	public int getScrollUpCount (final int columnId, final Selection selection,
			final Set<Integer> excludeColumnIds, final Set<Integer> columnsHidingRetweets,
			final boolean withInlineMediaOnly, final boolean excludeRetweets, final boolean excludeEditable,
			final ScrollState scroll) {
		return this.dbAdaptor.getScrollUpCount(columnId, selection,
				excludeColumnIds, columnsHidingRetweets,
				withInlineMediaOnly, excludeRetweets, excludeEditable,
				scroll);
	}

	@Override
	public void addTwUpdateListener (final TwUpdateListener listener) {
		this.dbAdaptor.addTwUpdateListener(listener);
	}

	@Override
	public void removeTwUpdateListener (final TwUpdateListener listener) {
		this.dbAdaptor.removeTwUpdateListener(listener);
	}

	@Override
	public void storeScroll (final int columnId, final ScrollState state) {
		this.dbAdaptor.storeScroll(columnId, state);
	}

	@Override
	public void storeUnreadTime (final int columnId, final long unreadTime) {
		this.dbAdaptor.storeUnreadTime(columnId, unreadTime);
	}

	@Override
	public void mergeAndStoreScrolls (final Map<Column, ScrollState> colToSs, final ScrollChangeType type) {
		this.dbAdaptor.mergeAndStoreScrolls(colToSs, type);
	}

	@Override
	public ScrollState getScroll (final int columnId) {
		return this.dbAdaptor.getScroll(columnId);
	}

	@Override
	public void notifyTwListenersColumnState (final int columnId, final ColumnState eventType) {
		this.dbAdaptor.notifyTwListenersColumnState(columnId, eventType);
	}

	@Override
	public Set<Integer> requestStoreScrollNow () {
		return this.dbAdaptor.requestStoreScrollNow();
	}

	@Override
	public long addPostToOutput (final OutboxTweet ot) {
		return this.dbAdaptor.addPostToOutput(ot);
	}

	@Override
	public void updateOutboxEntry (final OutboxTweet ot) {
		this.dbAdaptor.updateOutboxEntry(ot);
	}

	@Override
	public List<OutboxTweet> getUnsentOutboxEntries () {
		return this.dbAdaptor.getUnsentOutboxEntries();
	}

	@Override
	public List<OutboxTweet> getAllOutboxEntries () {
		return this.dbAdaptor.getAllOutboxEntries();
	}

	@Override
	public OutboxTweet getOutboxEntry (final long uid) {
		return this.dbAdaptor.getOutboxEntry(uid);
	}

	@Override
	public List<OutboxTweet> getOutboxEntries (final OutboxTweetStatus status) {
		return this.dbAdaptor.getOutboxEntries(status);
	}

	@Override
	public void deleteFromOutbox (final OutboxTweet ot) {
		this.dbAdaptor.deleteFromOutbox(ot);
	}

	@Override
	public void addOutboxListener (final OutboxListener listener) {
		this.dbAdaptor.addOutboxListener(listener);
	}

	@Override
	public void removeOutboxListener (final OutboxListener listener) {
		this.dbAdaptor.removeOutboxListener(listener);
	}

	@Override
	public String getValue (final String key) {
		return this.dbAdaptor.getValue(key);
	}

	@Override
	public void storeValue (final String key, final String value) {
		this.dbAdaptor.storeValue(key, value);
	}

	@Override
	public void deleteValue (final String key) {
		this.dbAdaptor.deleteValue(key);
	}

	@Override
	public void deleteValuesStartingWith (final String prefix) {
		this.dbAdaptor.deleteValuesStartingWith(prefix);
	}

	@Override
	public void cacheString (final CachedStringGroup group, final String key, final String value) {
		this.dbAdaptor.cacheString(group, key, value);
	}

	@Override
	public String cachedString (final CachedStringGroup group, final String key) {
		return this.dbAdaptor.cachedString(group, key);
	}

	@Override
	public void housekeep () {
		this.dbAdaptor.housekeep();
	}

	@Override
	public long getTotalTweetsEverSeen () {
		return this.dbAdaptor.getTotalTweetsEverSeen();
	}

	@Override
	public TimeRange getColumnTimeRange (final int columnId) {
		return this.dbAdaptor.getColumnTimeRange(columnId);
	}

	@Override
	public Map<String, Long> getColumnUserStats (final int columnId) {
		return this.dbAdaptor.getColumnUserStats(columnId);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

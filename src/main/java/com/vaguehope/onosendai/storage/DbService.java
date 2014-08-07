package com.vaguehope.onosendai.storage;

import java.util.List;
import java.util.Set;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;

import com.vaguehope.onosendai.config.Column;
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
	public void storeTweets (final Column column, final List<Tweet> tweets) {
		this.dbAdaptor.storeTweets(column, tweets);
	}

	@Override
	public void storeTweets (final int columnId, final List<Tweet> tweets) {
		this.dbAdaptor.storeTweets(columnId, tweets);
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
	public List<Tweet> getTweets (final int columnId, final int numberOf) {
		return this.dbAdaptor.getTweets(columnId, numberOf);
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf, final Set<Integer> excludeColumnIds) {
		return this.dbAdaptor.getTweets(columnId, numberOf, excludeColumnIds);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId) {
		return this.dbAdaptor.getTweetsCursor(columnId);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId, final boolean withInlineMediaOnly) {
		return this.dbAdaptor.getTweetsCursor(columnId, withInlineMediaOnly);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId, final Set<Integer> excludeColumnIds, final boolean withInlineMediaOnly) {
		return this.dbAdaptor.getTweetsCursor(columnId, excludeColumnIds, withInlineMediaOnly);
	}

	@Override
	public List<Tweet> getTweetsSinceTime (final int columnId, final long earliestTime, final int numberOf) {
		return this.dbAdaptor.getTweetsSinceTime(columnId, earliestTime, numberOf);
	}

	@Override
	public List<Tweet> findTweetsWithMeta (final MetaType metaType, final String data, final int numberOf) {
		return this.dbAdaptor.findTweetsWithMeta(metaType, data, numberOf);
	}

	@Override
	public List<Tweet> searchTweets (final String searchTerm, final int numberOf) {
		return this.dbAdaptor.searchTweets(searchTerm, numberOf);
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
	public List<String> getUsernames (final int numberOf) {
		return this.dbAdaptor.getUsernames(numberOf);
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
	public int getUnreadCount (final Column column) {
		return this.dbAdaptor.getUnreadCount(column);
	}

	@Override
	public int getUnreadCount (final int columnId, final Set<Integer> excludeColumnIds, final ScrollState scroll) {
		return this.dbAdaptor.getUnreadCount(columnId, excludeColumnIds, scroll);
	}

	@Override
	public int getScrollUpCount (final Column column) {
		return this.dbAdaptor.getScrollUpCount(column);
	}

	@Override
	public int getScrollUpCount (final int columnId, final Set<Integer> excludeColumnIds, final boolean withInlineMediaOnly, final ScrollState scroll) {
		return this.dbAdaptor.getScrollUpCount(columnId, excludeColumnIds, withInlineMediaOnly, scroll);
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
	public ScrollState getScroll (final int columnId) {
		return this.dbAdaptor.getScroll(columnId);
	}

	@Override
	public void notifyTwListenersColumnState (final int columnId, final ColumnState eventType) {
		this.dbAdaptor.notifyTwListenersColumnState(columnId, eventType);
	}

	@Override
	public void addPostToOutput (final OutboxTweet ot) {
		this.dbAdaptor.addPostToOutput(ot);
	}

	@Override
	public void updateOutboxEntry (final OutboxTweet ot) {
		this.dbAdaptor.updateOutboxEntry(ot);
	}

	@Override
	public List<OutboxTweet> getOutboxEntries () {
		return this.dbAdaptor.getOutboxEntries();
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
	public void housekeep () {
		this.dbAdaptor.housekeep();
	}

	@Override
	public long getTotalTweetsEverSeen () {
		return this.dbAdaptor.getTotalTweetsEverSeen();
	}

	@Override
	public double getTweetsPerHour (final int columnId) {
		return this.dbAdaptor.getTweetsPerHour(columnId);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

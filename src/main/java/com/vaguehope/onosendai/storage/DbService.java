package com.vaguehope.onosendai.storage;

import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.vaguehope.onosendai.config.Column;
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
	public void deleteTweet (final Column column, final Tweet tweet) {
		this.dbAdaptor.deleteTweet(column, tweet);
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf) {
		return this.dbAdaptor.getTweets(columnId, numberOf);
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
	public void addTwUpdateListener (final Runnable action) {
		this.dbAdaptor.addTwUpdateListener(action);
	}

	@Override
	public void removeTwUpdateListener (final Runnable action) {
		this.dbAdaptor.removeTwUpdateListener(action);
	}

	@Override
	public void storeScroll (final int columnId, final ScrollState state) {
		this.dbAdaptor.storeScroll(columnId, state);
	}

	@Override
	public ScrollState getScroll (final int columnId) {
		return this.dbAdaptor.getScroll(columnId);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

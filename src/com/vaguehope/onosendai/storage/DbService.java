package com.vaguehope.onosendai.storage;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

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
	public IBinder onBind (Intent arg0) {
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
	public void storeTweets (int columnId, List<Tweet> tweets) {
		this.dbAdaptor.storeTweets(columnId, tweets);
	}

	@Override
	public ArrayList<Tweet> getTweets (int columnId, int numberOf) {
		return this.dbAdaptor.getTweets(columnId, numberOf);
	}

	@Override
	public void addTwUpdateListener (Runnable action) {
		this.dbAdaptor.addTwUpdateListener(action);
	}

	@Override
	public void removeTwUpdateListener (Runnable action) {
		this.dbAdaptor.removeTwUpdateListener(action);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

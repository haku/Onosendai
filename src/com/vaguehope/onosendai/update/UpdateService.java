package com.vaguehope.onosendai.update;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.demo.FakeData;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.storage.DbClient;

public class UpdateService extends IntentService {

	protected final CountDownLatch dbReadyLatch = new CountDownLatch(1);
	private DbClient bndDb;

	public UpdateService () {
		super("OnosendaiUpdateService");
	}

	@Override
	public void onCreate () {
		super.onCreate();
		connectDb();
	}

	@Override
	public void onDestroy () {
		disconnectDb();
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent (Intent i) {
		Log.i(C.TAG, "UpdateService invoked.");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
		wl.acquire();
		try {
			doWork();
		}
		finally {
			wl.release();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void connectDb () {
		Log.i(C.TAG, "US Binding DB service...");
		this.bndDb = new DbClient(getApplicationContext(), new Runnable() {
			@Override
			public void run () {
				UpdateService.this.dbReadyLatch.countDown();
				Log.i(C.TAG, "US DB service bound.");
			}
		});
	}

	private void disconnectDb () {
		this.bndDb.finalize();
		Log.i(C.TAG, "US DB service rebound.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doWork () {
		if (connectionPresent()) {
			fetchTweets();
		}
		else {
			Log.i(C.TAG, "No connection, aborted.");
		}
	}

	private void fetchTweets () {
		// TODO check which columns need refreshing.
		int columnId = 0;
		TweetList tweets = FakeData.makeFakeTweets(); // TODO
		Log.i(C.TAG, "US fetched " + tweets.count() + " tweets.");

		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(3, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {/**/}
		if (dbReady) {
			this.bndDb.getDb().storeTweets(columnId, tweets.getTweets());
		}
		else {
			Log.e(C.TAG, "Time out waiting for DB service to connect.");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private boolean connectionPresent () {
		ConnectivityManager cMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
		if ((netInfo != null) && (netInfo.getState() != null)) {
			return netInfo.getState().equals(State.CONNECTED);
		}
		return false;
	}

}

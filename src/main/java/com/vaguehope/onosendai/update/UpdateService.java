package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import twitter4j.TwitterException;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.provider.twitter.TwitterFeed;
import com.vaguehope.onosendai.provider.twitter.TwitterFeeds;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.util.LogWrapper;

public class UpdateService extends IntentService {

	protected final LogWrapper log = new LogWrapper("US");
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
	protected void onHandleIntent (final Intent i) {
		this.log.i("UpdateService invoked.");
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
		this.log.d("Binding DB service...");
		this.bndDb = new DbClient(getApplicationContext(), this.log.getPrefix(), new Runnable() {
			@Override
			public void run () {
				UpdateService.this.dbReadyLatch.countDown();
				UpdateService.this.log.d("DB service bound.");
			}
		});
	}

	private void disconnectDb () {
		this.bndDb.finalize();
		this.log.d("DB service rebound.");
	}

	private boolean waitForDbReady() {
		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(3, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {/**/}
		if (!dbReady) {
			this.log.e("Not updateing: Time out waiting for DB service to connect.");
		}
		return dbReady;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doWork () {
		if (connectionPresent()) {
			fetchTweets();
		}
		else {
			this.log.i("No connection, all updating aborted.");
		}
	}

	private void fetchTweets () {
		Config conf;
		try {
			conf = new Config();
		}
		catch (IOException e) {
			this.log.w("Can not update: " + e.getMessage());
			return;
		}
		catch (JSONException e) {
			this.log.w("Can not update: " + e.getMessage());
			return;
		}

		final TwitterProvider twitterProvider = new TwitterProvider();

		Map<Integer, Column> columns = conf.getColumns();
		for (Column column : columns.values()) {
			Account account = conf.getAccount(column.accountId);
			if (account == null) {
				this.log.e("Unknown acountId: '" + column.accountId + "'.");
				continue;
			}
			switch (account.provider) {
				case TWITTER:
					try {
						twitterProvider.addAccount(account);
						TwitterFeed feed = TwitterFeeds.parse(column.resource);
						TweetList tweets = twitterProvider.getTweets(feed, account);
						if (!waitForDbReady()) return;
						this.bndDb.getDb().storeTweets(column.index, tweets.getTweets());
					}
					catch (TwitterException e) {
						this.log.w("Failed to fetch tweets: " + e.getMessage());
					}
					break;
				default:
					this.log.e("Unknown account type: " + account.provider);
					continue;
			}
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

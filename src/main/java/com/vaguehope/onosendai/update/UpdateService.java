package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.List;
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
import com.vaguehope.onosendai.model.Tweet;
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
		this.bndDb.dispose();
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
			fetchColumns();
		}
		else {
			this.log.i("No connection, all updating aborted.");
		}
	}

	private void fetchColumns () {
		Config conf;
		try {
			conf = new Config();
		}
		catch (IOException e) {
			this.log.w("Can not update: %s", e.getMessage());
			return;
		}
		catch (JSONException e) {
			this.log.w("Can not update: %s", e.getMessage());
			return;
		}

		final TwitterProvider twitterProvider = new TwitterProvider();
		try {
			fetchColumns(conf, twitterProvider);
		}
		finally {
			twitterProvider.shutdown();
		}
	}

	private void fetchColumns (final Config conf, final TwitterProvider twitterProvider) {
		for (Column column : conf.getColumns().values()) {
			// TODO parallelise this.
			fetchColumn(conf, column, twitterProvider);
		}
	}

	public void fetchColumn (final Config conf, final Column column, final TwitterProvider twitterProvider) {
		final long startTime = System.nanoTime();
		Account account = conf.getAccount(column.accountId);
		if (account == null) {
			this.log.e("Unknown acountId: '%s'.", column.accountId);
			return;
		}
		switch (account.provider) {
			case TWITTER:
				try {
					twitterProvider.addAccount(account);
					TwitterFeed feed = TwitterFeeds.parse(column.resource);
					if (!waitForDbReady()) return;

					long sinceId = -1;
					List<Tweet> existingTweets = this.bndDb.getDb().getTweets(column.id, 1);
					if (existingTweets.size() > 0) sinceId = existingTweets.get(existingTweets.size() - 1).getId();

					TweetList tweets = twitterProvider.getTweets(feed, account, sinceId);
					this.bndDb.getDb().storeTweets(column, tweets.getTweets());

					long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
					this.log.i("Fetched %d items for '%s' in %d millis.", tweets.count(), column.title, durationMillis);
				}
				catch (TwitterException e) {
					this.log.w("Failed to fetch tweets: %s", e.getMessage());
				}
				break;
			default:
				this.log.e("Unknown account type: %s", account.provider);
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

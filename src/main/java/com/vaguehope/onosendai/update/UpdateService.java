package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleFeed;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
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

		final ProviderMgr providerMgr = new ProviderMgr();
		try {
			fetchColumns(conf, providerMgr);
		}
		finally {
			providerMgr.shutdown();
		}
	}

	private void fetchColumns (final Config conf, final ProviderMgr providerMgr) {
		final long startTime = System.nanoTime();

		Collection<Column> columns = conf.getColumns().values();
		if (columns.size() >= C.MIN_COLUMS_TO_USE_THREADPOOL) {
			fetchColumnsMultiThread(conf, providerMgr, columns);
		}
		else {
			fetchColumnsSingleThread(conf, providerMgr, columns);
		}

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		this.log.i("Fetched %d columns in %d millis.", columns.size(), durationMillis);
	}

	private void fetchColumnsSingleThread (final Config conf, final ProviderMgr providerMgr, final Collection<Column> columns) {
		for (Column column : columns) {
			fetchColumn(conf, column, providerMgr);
		}
	}

	private void fetchColumnsMultiThread (final Config conf, final ProviderMgr providerMgr, final Collection<Column> columns) {
		int poolSize = Math.min(columns.size(), C.MAX_THREAD_POOL_SIZE);
		this.log.i("Using thread pool size %d for %d columns.", poolSize, columns.size());
		ExecutorService ex = Executors.newFixedThreadPool(poolSize);
		try {
			Map<Column, Future<Void>> jobs = new LinkedHashMap<Column, Future<Void>>();
			for (Column column : columns) {
				jobs.put(column, ex.submit(new FetchColumn(conf, column, providerMgr, this)));
			}
			for (Entry<Column, Future<Void>> job : jobs.entrySet()) {
				try {
					job.getValue().get();
				}
				catch (InterruptedException e) {
					this.log.w("Error fetching column '%s': %s %s", job.getKey().title, e.getClass().getName(), e.getMessage());
				}
				catch (ExecutionException e) {
					this.log.w("Error fetching column '%s': %s %s", job.getKey().title, e.getClass().getName(), e.getMessage());
				}
			}
		}
		finally {
			ex.shutdownNow();
		}
	}

	private static class FetchColumn implements Callable<Void> {

		private final Config conf;
		private final Column column;
		private final ProviderMgr providerMgr;
		private final UpdateService updateService;

		public FetchColumn (final Config conf, final Column column, final ProviderMgr providerMgr, final UpdateService updateService) {
			this.conf = conf;
			this.column = column;
			this.providerMgr = providerMgr;
			this.updateService = updateService;
		}

		@Override
		public Void call () throws Exception {
			this.updateService.fetchColumn(this.conf, this.column, this.providerMgr);
			return null;
		}

	}

	public void fetchColumn (final Config conf, final Column column, final ProviderMgr providerMgr) {
		final long startTime = System.nanoTime();
		final Account account = conf.getAccount(column.accountId);
		if (account == null) {
			this.log.e("Unknown acountId: '%s'.", column.accountId);
			return;
		}
		switch (account.provider) {
			case TWITTER:
				try {
					final TwitterProvider twitterProvider = providerMgr.getTwitterProvider();
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
					this.log.w("Failed to fetch from Twitter: %s", e.getMessage());
				}
				break;
			case SUCCESSWHALE:
				try {
					final SuccessWhaleProvider successWhaleProvider = providerMgr.getSuccessWhaleProvider();
					successWhaleProvider.addAccount(account);
					SuccessWhaleFeed feed = new SuccessWhaleFeed(column);
					if (!waitForDbReady()) return;

					TweetList tweets = successWhaleProvider.getTweets(feed, account);
					this.bndDb.getDb().storeTweets(column, tweets.getTweets());

					long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
					this.log.i("Fetched %d items for '%s' in %d millis.", tweets.count(), column.title, durationMillis);
				}
				catch (SuccessWhaleException e) {
					this.log.w("Failed to fetch from Success Whale: %s", e.getMessage());
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

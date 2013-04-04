package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class UpdateService extends IntentService {

	public static final String ARG_COLUMN_ID = "column_id";
	public static final String ARG_IS_MANUAL = "is_manual";

	private static final String KEY_PREFIX_COL_LAST_REFRESH_TIME = "COL_LAST_REFRESH_TIME_";
	protected static final LogWrapper LOG = new LogWrapper("US");

	private final CountDownLatch dbReadyLatch = new CountDownLatch(1);
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
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
		wl.acquire();
		try {
			final int columnId = i.getExtras() != null ? i.getExtras().getInt(ARG_COLUMN_ID, -1) : -1;
			final boolean manual = i.getExtras() != null ? i.getExtras().getBoolean(ARG_IS_MANUAL) : false;
			LOG.i("UpdateService invoked (column_id=%d, is_manual=%b).", columnId, manual);
			doWork(columnId, manual);
		}
		finally {
			wl.release();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void connectDb () {
		LOG.d("Binding DB service...");
		final CountDownLatch latch = this.dbReadyLatch;
		this.bndDb = new DbClient(getApplicationContext(), LOG.getPrefix(), new Runnable() {
			@Override
			public void run () {
				latch.countDown();
				LOG.d("DB service bound.");
			}
		});
	}

	private void disconnectDb () {
		this.bndDb.dispose();
		LOG.d("DB service rebound.");
	}

	private boolean waitForDbReady () {
		boolean dbReady = false;
		try {
			dbReady = this.dbReadyLatch.await(C.DB_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {/**/}
		if (!dbReady) LOG.e("Not updateing: Time out waiting for DB service to connect.");
		return dbReady;
	}

	protected DbInterface getDb () {
		final DbClient d = this.bndDb;
		if (d == null) return null;
		return d.getDb();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doWork (final int columnId, final boolean manual) {
		if (connectionPresent()) {
			fetchColumns(columnId, manual);
		}
		else {
			LOG.i("No connection, all updating aborted.");
		}
	}

	private void fetchColumns (final int columnId, final boolean manual) {
		Config conf;
		try {
			conf = new Config();
		}
		catch (IOException e) {
			LOG.w("Can not update: %s", e.toString());
			return;
		}
		catch (JSONException e) {
			LOG.w("Can not update: %s", e.toString());
			return;
		}

		if (!waitForDbReady()) return;
		final ProviderMgr providerMgr = new ProviderMgr(getDb());
		try {
			fetchColumns(conf, columnId, manual, providerMgr);
		}
		finally {
			providerMgr.shutdown();
		}
	}

	private void fetchColumns (final Config conf, final int columnId, final boolean manual, final ProviderMgr providerMgr) {
		final long startTime = System.nanoTime();

		final Collection<Column> columns = new ArrayList<Column>();
		if (columnId >= 0) {
			columns.add(conf.getColumnById(columnId));
		}
		else {
			columns.addAll(removeNotFetchable(conf.getColumns()));
		}

		if (!manual) removeNotDue(columns);
		// For now treating the configured interval as an 'attempt rate' not 'success rate' so write update time now.
		final long now = System.currentTimeMillis();
		for (final Column column : columns) {
			getDb().storeValue(KEY_PREFIX_COL_LAST_REFRESH_TIME + column.getId(), String.valueOf(now));
		}

		LOG.i("Updating columns: %s.", Column.titles(columns));

		if (columns.size() >= C.MIN_COLUMS_TO_USE_THREADPOOL) {
			fetchColumnsMultiThread(conf, providerMgr, columns);
		}
		else {
			fetchColumnsSingleThread(conf, providerMgr, columns);
		}

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.i("Fetched %d columns in %d millis.", columns.size(), durationMillis);
	}

	private static Collection<Column> removeNotFetchable (final Collection<Column> columns) {
		List<Column> ret = new ArrayList<Column>();
		for (Column column : columns) {
			if (column.getAccountId() != null) ret.add(column);
		}
		return ret;
	}

	private void removeNotDue (final Collection<Column> columns) {
		final Iterator<Column> colItr = columns.iterator();
		final long now = System.currentTimeMillis();
		while (colItr.hasNext()) {
			final Column column = colItr.next();
			final String lastTimeRaw = getDb().getValue(KEY_PREFIX_COL_LAST_REFRESH_TIME + column.getId());
			if (lastTimeRaw == null) continue;
			final long lastTime = Long.parseLong(lastTimeRaw);
			if (lastTime <= 0L) continue;
			if (now - lastTime < TimeUnit.MINUTES.toMillis(column.getRefreshIntervalMins())) colItr.remove();
		}
	}

	private void fetchColumnsSingleThread (final Config conf, final ProviderMgr providerMgr, final Collection<Column> columns) {
		for (Column column : columns) {
			fetchColumn(conf, column, providerMgr);
		}
	}

	private void fetchColumnsMultiThread (final Config conf, final ProviderMgr providerMgr, final Collection<Column> columns) {
		int poolSize = Math.min(columns.size(), C.MAX_THREAD_POOL_SIZE);
		LOG.i("Using thread pool size %d for %d columns.", poolSize, columns.size());
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
					LOG.w("Error fetching column '%s': %s %s", job.getKey().getTitle(), e.getClass().getName(), e.toString());
				}
				catch (ExecutionException e) {
					LOG.w("Error fetching column '%s': %s %s", job.getKey().getTitle(), e.getClass().getName(), e.toString());
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
		public Void call () {
			this.updateService.fetchColumn(this.conf, this.column, this.providerMgr);
			return null;
		}

	}

	public void fetchColumn (final Config conf, final Column column, final ProviderMgr providerMgr) {
		final long startTime = System.nanoTime();
		final Account account = conf.getAccount(column.getAccountId());
		if (account == null) {
			LOG.e("Unknown acountId: '%s'.", column.getAccountId());
			return;
		}
		switch (account.getProvider()) {
			case TWITTER:
				try {
					final TwitterProvider twitterProvider = providerMgr.getTwitterProvider();
					twitterProvider.addAccount(account);
					TwitterFeed feed = TwitterFeeds.parse(column.getResource());

					long sinceId = -1;
					List<Tweet> existingTweets = getDb().getTweets(column.getId(), 1);
					if (existingTweets.size() > 0) sinceId = Long.parseLong(existingTweets.get(existingTweets.size() - 1).getSid());

					TweetList tweets = twitterProvider.getTweets(feed, account, sinceId);
					getDb().storeTweets(column, tweets.getTweets());

					long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
					LOG.i("Fetched %d items for '%s' in %d millis.", tweets.count(), column.getTitle(), durationMillis);
				}
				catch (TwitterException e) {
					LOG.w("Failed to fetch from Twitter: %s", e.toString());
				}
				break;
			case SUCCESSWHALE:
				try {
					final SuccessWhaleProvider successWhaleProvider = providerMgr.getSuccessWhaleProvider();
					successWhaleProvider.addAccount(account);
					SuccessWhaleFeed feed = new SuccessWhaleFeed(column);

					TweetList tweets = successWhaleProvider.getTweets(feed, account);
					getDb().storeTweets(column, tweets.getTweets());

					long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
					LOG.i("Fetched %d items for '%s' in %d millis.", tweets.count(), column.getTitle(), durationMillis);
				}
				catch (SuccessWhaleException e) {
					LOG.w("Failed to fetch from SuccessWhale: %s", e.toString());
				}
				break;
			default:
				LOG.e("Unknown account type: %s", account.getProvider());
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

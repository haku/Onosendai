package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbClient;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.NetHelper;

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
		catch (final InterruptedException e) {/**/}
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
		if (NetHelper.connectionPresent(this)) {
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
		catch (final IOException e) {
			LOG.w("Can not update: %s", e.toString());
			return;
		}
		catch (final JSONException e) {
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

		if (columns.size() >= C.UPDATER_MIN_COLUMS_TO_USE_THREADPOOL) {
			fetchColumnsMultiThread(conf, providerMgr, columns);
		}
		else {
			fetchColumnsSingleThread(conf, providerMgr, columns);
		}

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.i("Fetched %d columns in %d millis.", columns.size(), durationMillis);
	}

	private static Collection<Column> removeNotFetchable (final Collection<Column> columns) {
		final List<Column> ret = new ArrayList<Column>();
		for (final Column column : columns) {
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
		for (final Column column : columns) {
			final Account account = resolveColumnsAccount(conf, column);
			if (account == null) continue;
			FetchColumn.fetchColumn(getDb(), account, column, providerMgr);
		}
	}

	private void fetchColumnsMultiThread (final Config conf, final ProviderMgr providerMgr, final Collection<Column> columns) {
		final int poolSize = Math.min(columns.size(), C.UPDATER_MAX_THREADS);
		LOG.i("Using thread pool size %d for %d columns.", poolSize, columns.size());
		final ExecutorService ex = Executors.newFixedThreadPool(poolSize);
		try {
			final Map<Column, Future<Void>> jobs = new LinkedHashMap<Column, Future<Void>>();
			for (final Column column : columns) {
				final Account account = resolveColumnsAccount(conf, column);
				if (account == null) continue;
				jobs.put(column, ex.submit(new FetchColumn(getDb(), account, column, providerMgr)));
			}
			for (final Entry<Column, Future<Void>> job : jobs.entrySet()) {
				try {
					job.getValue().get();
				}
				catch (final InterruptedException e) {
					LOG.w("Error fetching column '%s': %s %s", job.getKey().getTitle(), e.getClass().getName(), e.toString());
				}
				catch (final ExecutionException e) {
					LOG.w("Error fetching column '%s': %s %s", job.getKey().getTitle(), e.getClass().getName(), e.toString());
				}
			}
		}
		finally {
			ex.shutdownNow();
		}
	}

	private static Account resolveColumnsAccount (final Config conf, final Column column) {
		final Account account = conf.getAccount(column.getAccountId());
		if (account == null) LOG.e("Unknown acountId: '%s'.", column.getAccountId());
		return account;
	}

}

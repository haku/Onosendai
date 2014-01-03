package com.vaguehope.onosendai.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.content.Intent;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.NetHelper;

public class UpdateService extends DbBindingService {

	public static final String ARG_COLUMN_ID = "column_id";
	public static final String ARG_IS_MANUAL = "is_manual";

	protected static final LogWrapper LOG = new LogWrapper("US");

	public UpdateService () {
		super("OnosendaiUpdateService", LOG);
	}

	@Override
	protected void doWork (final Intent i) {
		final int columnId = i.getIntExtra(ARG_COLUMN_ID, Integer.MIN_VALUE);
		final boolean manual = i.getBooleanExtra(ARG_IS_MANUAL, false);
		LOG.i("UpdateService invoked (column_id=%d, is_manual=%b).", columnId, manual);
		doWork(columnId, manual);
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
			conf = new Prefs(getBaseContext()).asConfig();
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

		final Collection<Column> columns = columnsToFetch(conf, columnId, manual);
		fetchColumns(conf, providerMgr, columns);
		if (!manual) Notifications.update(getBaseContext(), getDb(), columns);

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.i("Fetched %d columns in %d millis.", columns.size(), durationMillis);
	}

	private Collection<Column> columnsToFetch (final Config conf, final int columnId, final boolean manual) {
		final Collection<Column> columns = new ArrayList<Column>();
		if (columnId > Integer.MIN_VALUE) {
			columns.add(conf.getColumnById(columnId));
		}
		else {
			columns.addAll(removeNotFetchable(conf.getColumns()));
		}

		if (!manual) removeNotDue(columns);
		// For now treating the configured interval as an 'attempt rate' not 'success rate' so write update time now.
		final long now = System.currentTimeMillis();
		for (final Column column : columns) {
			getDb().storeValue(KvKeys.KEY_PREFIX_COL_LAST_REFRESH_TIME + column.getId(), String.valueOf(now));
		}

		return columns;
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
			final int refIntMins = column.getRefreshIntervalMins();
			if (refIntMins < 1) colItr.remove(); // Do not refresh columns not configured to refresh.
			final String lastTimeRaw = getDb().getValue(KvKeys.KEY_PREFIX_COL_LAST_REFRESH_TIME + column.getId());
			if (lastTimeRaw == null) continue; // Never refreshed.
			final long lastTime = Long.parseLong(lastTimeRaw);
			if (lastTime <= 0L) continue; // Probably never refreshed.
			if (now - lastTime < TimeUnit.MINUTES.toMillis(refIntMins)) colItr.remove(); // No not refresh up to date columns.
		}
	}

	private void fetchColumns (final Config conf, final ProviderMgr providerMgr, final Collection<Column> columns) {
		LOG.i("Updating columns: %s.", Column.titles(columns));
		if (columns.size() >= C.UPDATER_MIN_COLUMS_TO_USE_THREADPOOL) {
			fetchColumnsMultiThread(conf, providerMgr, columns);
		}
		else {
			fetchColumnsSingleThread(conf, providerMgr, columns);
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

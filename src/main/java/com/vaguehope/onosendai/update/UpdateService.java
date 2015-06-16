package com.vaguehope.onosendai.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Filters;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.NetHelper;

public class UpdateService extends DbBindingService {

	public static final String ARG_COLUMN_IDS = "column_ids";
	public static final String ARG_IS_MANUAL = "is_manual";

	protected static final LogWrapper LOG = new LogWrapper("US");

	public UpdateService () {
		super("OnosendaiUpdateService", LOG);
	}

	@Override
	protected void doWork (final Intent i) {
		final int[] columnIds = i.getIntArrayExtra(ARG_COLUMN_IDS);
		final boolean manual = i.getBooleanExtra(ARG_IS_MANUAL, false);
		LOG.i("UpdateService invoked (column_ids=%s, is_manual=%b).", Arrays.toString(columnIds), manual);
		doWork(columnIds, manual);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doWork (final int[] columnIds, final boolean manual) {
		if (NetHelper.connectionPresent(this)) {
			fetchColumns(columnIds, manual);
		}
		else {
			LOG.i("No connection, all updating aborted.");
		}
	}

	private void fetchColumns (final int[] columnIds, final boolean manual) {
		final Prefs prefs = new Prefs(getBaseContext());

		final Config conf;
		try {
			conf = prefs.asConfig();
		}
		catch (final JSONException e) {
			LOG.w("Can not update: %s", e.toString());
			return;
		}
		final UpdateRequest req = new UpdateRequest(columnIds, manual, new Filters(prefs.readFilters()));

		if (!waitForDbReady()) return;
		final Collection<Column> columnsFetched;
		final ProviderMgr providerMgr = new ProviderMgr(getDb());
		try {
			columnsFetched = fetchColumns(conf, req, providerMgr);
		}
		finally {
			providerMgr.shutdown();
		}

		HosakaSyncService.startServiceIfConfigured(this, conf, columnIds);
		FetchPictureService.startServiceIfConfigured(this, prefs, columnsFetched, manual);
	}

	private Collection<Column> fetchColumns (final Config conf, final UpdateRequest req, final ProviderMgr providerMgr) {
		final long startTime = System.nanoTime();

		final Collection<Column> columns = columnsToFetch(conf, req);
		LOG.i("Updating columns: %s.", Column.titles(columns));

		final Collection<FetchFeedRequest> fetches = feedsToFetchs(conf, columns);
		fetchFeeds(providerMgr, req, fetches);

		if (!req.manual) Notifications.update(getBaseContext(), getDb(), columns);

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.i("Fetched %d columns in %d millis.", columns.size(), durationMillis);

		return columns;
	}

	private Collection<Column> columnsToFetch (final Config conf, final UpdateRequest req) {
		final Collection<Column> columns = new ArrayList<Column>();
		if (req.columnIds != null && req.columnIds.length > 0) {
			for (final int columnId : req.columnIds) {
				final Column column = conf.getColumnById(columnId);
				if (column != null) {
					columns.add(column);
				}
				else {
					LOG.i("Column not found: %s", columnId);
				}
			}
		}
		else {
			columns.addAll(conf.getColumns());
		}

		if (!req.manual) removeNotDue(columns);
		// For now treating the configured interval as an 'attempt rate' not 'success rate' so write update time now.
		final long now = System.currentTimeMillis();
		for (final Column column : columns) {
			getDb().storeValue(KvKeys.KEY_PREFIX_COL_LAST_REFRESH_TIME + column.getId(), String.valueOf(now));
		}

		return columns;
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

	private static Collection<FetchFeedRequest> feedsToFetchs (final Config conf, final Collection<Column> columns) {
		final Collection<FetchFeedRequest> ret = new ArrayList<FetchFeedRequest>();
		for (final Column col : columns) {
			for (final ColumnFeed feed : col.getFeeds()) {
				final Account account = resolveFeedAccount(conf, feed);
				if (account != null) ret.add(new FetchFeedRequest(col, feed, account));
			}
		}
		return ret;
	}

	private void fetchFeeds (final ProviderMgr providerMgr, final UpdateRequest req, final Collection<FetchFeedRequest> fetches) {
		if (fetches.size() >= C.UPDATER_MIN_COLUMS_TO_USE_THREADPOOL) {
			fetchFeedsMultiThread(providerMgr, fetches, req);
		}
		else {
			fetchFeedsSingleThread(providerMgr, fetches, req);
		}
	}

	private void fetchFeedsSingleThread (final ProviderMgr providerMgr, final Collection<FetchFeedRequest> feeds, final UpdateRequest req) {
		for (final FetchFeedRequest feed : feeds) {
			FetchColumn.fetchColumn(getDb(), feed, providerMgr, req.filters);
		}
	}

	private void fetchFeedsMultiThread (final ProviderMgr providerMgr, final Collection<FetchFeedRequest> feeds, final UpdateRequest req) {
		final int poolSize = Math.min(feeds.size(), C.UPDATER_MAX_THREADS);
		LOG.i("Using thread pool size %d for %d feeds.", poolSize, feeds.size());
		final ExecutorService ex = Executors.newFixedThreadPool(poolSize);
		try {
			final Map<FetchFeedRequest, Future<Void>> jobs = new LinkedHashMap<FetchFeedRequest, Future<Void>>();
			for (final FetchFeedRequest feed : feeds) {
				jobs.put(feed, ex.submit(new FetchColumn(getDb(), feed, providerMgr, req.filters)));
			}
			for (final Entry<FetchFeedRequest, Future<Void>> job : jobs.entrySet()) {
				try {
					job.getValue().get();
				}
				catch (final InterruptedException e) {
					LOG.w("Error fetching feed '%s': %s %s", job.getKey().getUiTitle(), e.getClass().getName(), e.toString());
				}
				catch (final ExecutionException e) {
					LOG.w("Error fetching feed '%s': %s %s", job.getKey().getUiTitle(), e.getClass().getName(), e.toString());
				}
			}
		}
		finally {
			ex.shutdownNow();
		}
	}

	private static Account resolveFeedAccount (final Config conf, final ColumnFeed feed) {
		final Account account = conf.getAccount(feed.getAccountId());
		if (account == null) LOG.e("Unknown acountId: '%s'.", feed.getAccountId());
		return account;
	}

	private static class UpdateRequest {

		public final int[] columnIds;
		public final boolean manual;
		public final Filters filters;

		public UpdateRequest (final int[] columnIds, final boolean manual, final Filters filters) {
			this.columnIds = columnIds;
			this.manual = manual;
			this.filters = filters;
		}

	}

}

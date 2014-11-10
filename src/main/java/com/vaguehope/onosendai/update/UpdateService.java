package com.vaguehope.onosendai.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import android.database.Cursor;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.ui.pref.AdvancedPrefFragment;
import com.vaguehope.onosendai.util.BatteryHelper;
import com.vaguehope.onosendai.util.IoHelper;
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
		final Prefs prefs = new Prefs(getBaseContext());

		Config conf;
		try {
			conf = prefs.asConfig();
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

		considerFetchingPictures(prefs, conf);
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

	private void considerFetchingPictures (final Prefs prefs, final Config conf) {
		try {
			if (prefs.getSharedPreferences().getBoolean(AdvancedPrefFragment.KEY_PREFETCH_MEDIA, false)) {
				if (NetHelper.isWifi(this)) {
					final float bl = BatteryHelper.level(getApplicationContext());
					if (bl >= C.MIN_BAT_BG_FETCH_PICTURES) {
						LOG.i("Fetching pictures (bl=%s) ...", bl);
						fetchPictures(conf);
					}
					else {
						LOG.i("Not fetching pictures; battery %s < %s.", bl, C.MIN_BAT_BG_FETCH_PICTURES);
					}
				}
				else {
					LOG.i("Not fetching pictures; not on WiFi.");
				}
			}
		}
		catch (Exception e) {
			LOG.e("Failed to fetch pictures.", e);
		}
	}

	private void fetchPictures (final Config conf) {
		final long startTime = System.nanoTime();

		final List<String> mediaUrls = new ArrayList<String>();
		for (final Column col : conf.getColumns()) {
			mediaUrls.addAll(findPicturesToFetch(col));
		}
		final int downloadCount = downloadPictures(mediaUrls);

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		LOG.i("Fetched %d pictures in %d millis.", downloadCount, durationMillis);
	}

	/**
	 * Ordered oldest (first to be scrolled up to) first.
	 */
	private List<String> findPicturesToFetch (final Column col) {
		final ScrollState scroll = getDb().getScroll(col.getId());
		final Cursor cursor = getDb().getTweetsCursor(col.getId(), col.getExcludeColumnIds(), true); // XXX for now only get first image, later include extra images?
		try {
			final TweetCursorReader reader = new TweetCursorReader();
			if (cursor != null && cursor.moveToFirst()) {
				final List<String> mediaUrls = new ArrayList<String>();
				do {
					if (reader.readTime(cursor) < scroll.getUnreadTime()) break; // Stop gathering URLs at unread point.
					final String mediaUrl = reader.readInlineMedia(cursor);
					if (mediaUrl != null) mediaUrls.add(mediaUrl);
				}
				while (cursor.moveToNext());
				Collections.reverse(mediaUrls); // Fetch oldest pictures first.
				return mediaUrls;
			}
			return Collections.emptyList();
		}
		finally {
			IoHelper.closeQuietly(cursor);
		}
	}

	private int downloadPictures (final List<String> mediaUrls) {
		if (mediaUrls == null || mediaUrls.size() < 1) return 0;

		final HybridBitmapCache hybridBitmapCache = new HybridBitmapCache(this, 0);

		final Map<String, FetchPicture> jobs = new LinkedHashMap<String, FetchPicture>();
		for (final String mediaUrl : mediaUrls) {
			if (!hybridBitmapCache.touchFileIfExists(mediaUrl)) {
				jobs.put(mediaUrl, new FetchPicture(hybridBitmapCache, mediaUrl));
			}
		}
		if (jobs.size() < 1) return 0;

		final int poolSize = Math.min(jobs.size(), C.UPDATER_MAX_THREADS);
		LOG.i("Downloading %s pictures using %s threads.", jobs.size(), poolSize);
		final ExecutorService ex = Executors.newFixedThreadPool(poolSize);
		try {
			final Map<String, Future<Void>> futures = new LinkedHashMap<String, Future<Void>>();
			for (Entry<String, FetchPicture> job : jobs.entrySet()) {
				futures.put(job.getKey(), ex.submit(job.getValue()));
			}

			for (final Entry<String, Future<Void>> future : futures.entrySet()) {
				try {
					future.getValue().get();
				}
				catch (final InterruptedException e) {
					LOG.w("Error fetching picture '%s': %s %s", future.getKey(), e.getClass().getName(), e.toString());
				}
				catch (final ExecutionException e) {
					LOG.w("Error fetching picture '%s': %s %s", future.getKey(), e.getClass().getName(), e.toString());
				}
			}

			return futures.size();
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

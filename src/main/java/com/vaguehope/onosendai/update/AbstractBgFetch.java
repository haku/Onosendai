package com.vaguehope.onosendai.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.PrefetchMode;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.Selection;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.util.BatteryHelper;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.NetHelper;

public abstract class AbstractBgFetch<D> extends DbBindingService {

	public static final String ARG_COLUMN_IDS = "column_ids";
	public static final String ARG_IS_MANUAL = "is_manual";

	private static final LogWrapper LOG = new LogWrapper("ABF");

	protected static void startServiceIfConfigured (final Class<? extends AbstractBgFetch<?>> cls, final String prefKey, final Context context, final Prefs prefs, final Collection<Column> columns, final boolean manual) {
		final PrefetchMode prefetchMode = readPrefetchMode(prefs, prefKey);

		if (prefetchMode == PrefetchMode.NO) {
			return;
		}
		else if (prefetchMode == PrefetchMode.ALWAYS) {
			startService(cls, context, columns, manual);
		}
		else if (prefetchMode == PrefetchMode.WIFI_ONLY) {
			if (NetHelper.isWifi(context)) {
				startService(cls, context, columns, manual);
			}
			else {
				LOG.i("Not fetching; not on WiFi.");
			}
		}
		else {
			LOG.i("Not fetching; unknown mode: %s.", prefetchMode);
		}
	}

	private static PrefetchMode readPrefetchMode (final Prefs prefs, final String prefKey) {
		return PrefetchMode.parseValue(
				prefs.getSharedPreferences().getString(
						prefKey,
						PrefetchMode.NO.getValue()));
	}

	private static void startService (final Class<? extends AbstractBgFetch<?>> cls, final Context context, final Collection<Column> columns, final boolean manual) {
		final int[] columnIds = new int[columns.size()];
		final Iterator<Column> columnsIttr = columns.iterator();
		for (int i = 0; i < columnIds.length; i++) {
			columnIds[i] = columnsIttr.next().getId();
		}

		final Intent intent = new Intent(context, cls);
		intent.putExtra(ARG_COLUMN_IDS, columnIds);
		intent.putExtra(ARG_IS_MANUAL, manual);
		context.startService(intent);
	}

	private final boolean withInlineMediaOnly;

	public AbstractBgFetch (final Class<? extends AbstractBgFetch<?>> cls, final boolean withInlineMediaOnly, final LogWrapper log) {
		super(cls.getSimpleName(), log);
		this.withInlineMediaOnly = withInlineMediaOnly;
	}

	@Override
	protected void doWork (final Intent i) {
		final int[] columnIds = i.getIntArrayExtra(ARG_COLUMN_IDS);
		final boolean manual = i.getBooleanExtra(ARG_IS_MANUAL, false);
		getLog().i("%s invoked (column_ids=%s, is_manual=%b).", getClass().getSimpleName(), Arrays.toString(columnIds), manual);
		doWork(columnIds, manual);
	}

	private void doWork (final int[] columnIds, final boolean manual) {
		if (NetHelper.connectionPresent(this)) {
			fetchColumns(columnIds, manual);
		}
		else {
			getLog().i("No connection, all fetching aborted.");
		}
	}

	private void fetchColumns (final int[] columnIds, final boolean manual) {
		final Prefs prefs = new Prefs(getBaseContext());

		Config conf;
		try {
			conf = prefs.asConfig();
		}
		catch (final JSONException e) {
			getLog().w("Can not parse conf: %s", e.toString());
			return;
		}

		final Collection<Column> columns = new ArrayList<Column>();
		for (final int colId : columnIds) {
			columns.add(conf.getColumnById(colId));
		}

		if (!waitForDbReady()) return;

		fetchIfBatteryOk(columns, manual, conf);
	}

	private void fetchIfBatteryOk (final Collection<Column> columnsToFetch, final boolean manual, final Config conf) {
		final double batLimit = manual ? C.MIN_BAT_BG_FETCH_MANUAL : C.MIN_BAT_BG_FETCH_SCHEDULED;
		final float bl = BatteryHelper.level(getApplicationContext());
		if (bl < batLimit) {
			getLog().i("Not fetching; battery %s < %s.", bl, batLimit);
			return;
		}
		getLog().i("Fetching (bl=%s, m=%s) ...", bl, manual);
		fetch(columnsToFetch, conf);
	}

	private void fetch (final Collection<Column> columnsToFetch, final Config conf) {
		final long startTime = System.nanoTime();

		final List<D> metas = new ArrayList<D>();
		for (final Column col : columnsToFetch) {
			metas.addAll(findToFetch(col, conf));
		}
		final int downloadCount = download(metas);

		final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		getLog().i("Fetched %d in %d millis.", downloadCount, durationMillis);
	}

	/**
	 * Ordered oldest (first to be scrolled up to) first.
	 */
	private List<D> findToFetch (final Column col, final Config conf) {
		final DbInterface db = getDb();
		final ScrollState scroll = db.getScroll(col.getId());
		final Cursor cursor = db.getTweetsCursor(col.getId(), Selection.FILTERED, col.getExcludeColumnIds(), this.withInlineMediaOnly);
		try {
			final TweetCursorReader reader = new TweetCursorReader();
			if (cursor != null && cursor.moveToFirst()) {
				final List<D> metas = new ArrayList<D>();
				do {
					if (scroll != null && reader.readTime(cursor) < scroll.getUnreadTime()) break; // Stop gathering URLs at unread point.
					readUrls(cursor, reader, col, conf, metas);

					final List<Meta> quotedMetas = db.getTweetMetasOfType(reader.readUid(cursor), MetaType.QUOTED_SID);
					if (quotedMetas != null) {
						for (final Meta m : quotedMetas) {
							if (m.getData() != null) {
								final Tweet quotedTweet = db.getTweetDetails(m.getData());
								if (quotedTweet != null) {
									readUrls(quotedTweet, col, conf, metas);
								}
								else {
									getLog().w("Quoted tweet not in DB: %s", m.getData());
								}
							}
						}
					}
				}
				while (cursor.moveToNext());
				Collections.reverse(metas); // Fetch oldest first.
				return metas;
			}
			return Collections.emptyList();
		}
		finally {
			IoHelper.closeQuietly(cursor);
		}
	}

	protected abstract void readUrls (final Cursor cursor, final TweetCursorReader reader, Column col, Config conf, final List<D> retMetas);
	protected abstract void readUrls (final Tweet tweet, Column col, Config conf, final List<D> retMetas);

	private int download (final List<D> metas) {
		if (metas == null || metas.size() < 1) return 0;

		final ProviderMgr provMgr = new ProviderMgr(getDb());
		try {
			return download(metas, provMgr);
		}
		finally {
			provMgr.shutdown();
		}
	}

	private int download (final List<D> metas, final ProviderMgr provMgr) {
		final Map<String, Callable<?>> jobs = new LinkedHashMap<String, Callable<?>>();
		makeJobs(metas, provMgr, jobs);
		if (jobs.size() < 1) return 0;

		final int poolSize = Math.min(jobs.size(), C.UPDATER_MAX_THREADS);
		getLog().i("Downloading %s using %s threads.", jobs.size(), poolSize);
		final ExecutorService ex = Executors.newFixedThreadPool(poolSize);
		try {
			final Map<String, Future<?>> futures = new LinkedHashMap<String, Future<?>>();
			for (final Entry<String, Callable<?>> job : jobs.entrySet()) {
				futures.put(job.getKey(), ex.submit(job.getValue()));
			}
			for (final Entry<String, Future<?>> future : futures.entrySet()) {
				try {
					future.getValue().get();
				}
				catch (final InterruptedException e) {
					getLog().w("Error fetching '%s': %s %s", future.getKey(), e.getClass().getName(), e.toString());
				}
				catch (final ExecutionException e) {
					getLog().w("Error fetching '%s': %s %s", future.getKey(), e.getClass().getName(), e.toString());
				}
			}

			return futures.size();
		}
		finally {
			ex.shutdownNow();
		}
	}

	protected abstract void makeJobs (final List<D> metas, ProviderMgr provMgr, final Map<String, Callable<?>> jobs);

}

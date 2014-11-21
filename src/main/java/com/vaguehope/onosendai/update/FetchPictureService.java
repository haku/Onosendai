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
import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.model.PrefetchImages;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.ui.pref.FetchingPrefFragment;
import com.vaguehope.onosendai.util.BatteryHelper;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.NetHelper;

public class FetchPictureService extends DbBindingService {

	public static final String ARG_COLUMN_IDS = "column_ids";
	public static final String ARG_IS_MANUAL = "is_manual";

	protected static final LogWrapper LOG = new LogWrapper("FPS");

	public static void startServiceIfConfigured (final Context context, final Prefs prefs, final Collection<Column> columns, final boolean manual) {
		final PrefetchImages prefetchMode = readPrefetchMode(prefs);

		if (prefetchMode == PrefetchImages.NO) {
			return;
		}
		else if (prefetchMode == PrefetchImages.ALWAYS) {
			startService(context, columns, manual);
		}
		else if (prefetchMode == PrefetchImages.WIFI_ONLY) {
			if (NetHelper.isWifi(context)) {
				startService(context, columns, manual);
			}
			else {
				LOG.i("Not fetching pictures; not on WiFi.");
			}
		}
		else {
			LOG.i("Not fetching pictures; unknown mode: %s.", prefetchMode);
		}
	}

	private static PrefetchImages readPrefetchMode (final Prefs prefs) {
		return PrefetchImages.parseValue(
				prefs.getSharedPreferences().getString(
						FetchingPrefFragment.KEY_PREFETCH_MEDIA,
						PrefetchImages.NO.getValue()));
	}

	private static void startService (final Context context, final Collection<Column> columns, final boolean manual) {
		final int[] columnIds = new int[columns.size()];
		final Iterator<Column> columnsIttr = columns.iterator();
		for (int i = 0; i < columnIds.length; i++) {
			columnIds[i] = columnsIttr.next().getId();
		}

		final Intent intent = new Intent(context, FetchPictureService.class);
		intent.putExtra(ARG_COLUMN_IDS, columnIds);
		intent.putExtra(ARG_IS_MANUAL, manual);
		context.startService(intent);
	}

	public FetchPictureService () {
		super(FetchPictureService.class.getSimpleName(), LOG);
	}

	@Override
	protected void doWork (final Intent i) {
		final int[] columnIds = i.getIntArrayExtra(ARG_COLUMN_IDS);
		final boolean manual = i.getBooleanExtra(ARG_IS_MANUAL, false);
		LOG.i("%s invoked (column_ids=%s, is_manual=%b).", getClass().getSimpleName(), Arrays.toString(columnIds), manual);
		doWork(columnIds, manual);
	}

	private void doWork (final int[] columnIds, final boolean manual) {
		if (NetHelper.connectionPresent(this)) {
			fetchColumns(columnIds, manual);
		}
		else {
			LOG.i("No connection, all fetching aborted.");
		}
	}

	private void fetchColumns (final int[] columnIds, final boolean manual) {
		final Prefs prefs = new Prefs(getBaseContext());

		Config conf;
		try {
			conf = prefs.asConfig();
		}
		catch (final JSONException e) {
			LOG.w("Can not parse conf: %s", e.toString());
			return;
		}

		final Collection<Column> columns = new ArrayList<Column>();
		for (final int colId : columnIds) {
			columns.add(conf.getColumnById(colId));
		}

		if (!waitForDbReady()) return;

		fetchPicutresIfBatteryOk(columns, manual);
	}

	private void fetchPicutresIfBatteryOk (final Collection<Column> columnsToFetch, final boolean manual) {
		final double batLimit = manual ? C.MIN_BAT_BG_FETCH_PICTURES_MANUAL : C.MIN_BAT_BG_FETCH_PICTURES_SCHEDULED;
		final float bl = BatteryHelper.level(getApplicationContext());
		if (bl < batLimit) {
			LOG.i("Not fetching pictures; battery %s < %s.", bl, batLimit);
			return;
		}
		LOG.i("Fetching pictures (bl=%s, m=%s) ...", bl, manual);
		fetchPictures(columnsToFetch);
	}

	private void fetchPictures (final Collection<Column> columnsToFetch) {
		final long startTime = System.nanoTime();

		final List<String> mediaUrls = new ArrayList<String>();
		for (final Column col : columnsToFetch) {
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
					if (scroll != null && reader.readTime(cursor) < scroll.getUnreadTime()) break; // Stop gathering URLs at unread point.
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
			for (final Entry<String, FetchPicture> job : jobs.entrySet()) {
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

}

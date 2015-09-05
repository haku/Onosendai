package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.ScrollState.ScrollDirection;
import com.vaguehope.onosendai.provider.hosaka.HosakaColumn;
import com.vaguehope.onosendai.provider.hosaka.HosakaProvider;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.ScrollChangeType;
import com.vaguehope.onosendai.storage.SaveScrollNow;
import com.vaguehope.onosendai.ui.pref.FetchingPrefFragment;
import com.vaguehope.onosendai.util.DateHelper;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class HosakaSyncService extends DbBindingService {

	protected static final LogWrapper LOG = new LogWrapper("HSS");

	public static void startServiceIfConfigured (final Context context, final Config conf, final int[] columnIds) {
		if (columnIds != null && columnIds.length > 0) return; // Not on manual with specific columns.
		if (conf.firstAccountOfType(AccountProvider.HOSAKA) == null) return; // Must have account configured.
		startService(context);
	}

	private static void startService (final Context context) {
		context.startService(new Intent(context, HosakaSyncService.class));
	}

	public HosakaSyncService () {
		super(HosakaSyncService.class.getSimpleName(), LOG);
	}

	@Override
	protected void doWork (final Intent i) {
		final Prefs prefs = new Prefs(getBaseContext());
		final Config conf;
		try {
			conf = prefs.asConfig();
		}
		catch (final JSONException e) {
			LOG.w("Can not send to Hosaka: %s", e.toString());
			return;
		}

		// XXX Currently this assumes only one Hosaka account.
		// TODO Make UI stop user adding more than one Hosaka account.

		final Account account = conf.firstAccountOfType(AccountProvider.HOSAKA);
		if (account == null) {
			LOG.i("Not sending to Hosaka: no account found.");
			return;
		}

		if (!waitForDbReady()) return;
		final DbInterface db = getDb();

		SaveScrollNow.requestAndWaitForUiToSaveScroll(db);

		final Map<String, Column> hashToCol = new HashMap<String, Column>();
		final Map<String, HosakaColumn> toPush = new HashMap<String, HosakaColumn>();
		for (final Column col : conf.getColumns()) {
			if (InternalColumnType.fromColumn(col) != null) continue; // Do not sync internal columns.

			final String hash = HosakaColumn.columnHash(col, conf);
			hashToCol.put(hash, col);
			final ScrollState ss = db.getScroll(col.getId());
			if (ss == null) continue; // In case of (new) empty columns.
			// Always add all columns, even if sent before new values.
			// - Old / regressed values will be filtered server side.
			// - Values sent can be used to filter response.
			// - Also useful as do not know state of remote DB.
			toPush.put(hash, new HosakaColumn(null /* ss.getItemId(); TODO ScrollState to also store sid? */, ss.getItemTime(), ss.getUnreadTime(), ss.getScrollDirection()));
		}

		final HosakaProvider prov = new HosakaProvider();
		try {
			// Make POST even if not really sending anything new, as may be fetching new state.
			final long startTime = now();
			final Map<String, HosakaColumn> returnedColumns = prov.sendColumns(account, toPush);
			final long durationMillis = TimeUnit.NANOSECONDS.toMillis(now() - startTime);
			LOG.i("Sent %s in %d millis: %s", account.getAccessToken(), durationMillis, toPush);

			final boolean syncScroll = prefs.getSharedPreferences().getBoolean(FetchingPrefFragment.KEY_SYNC_SCROLL, false);

			final Map<Column, ScrollState> colToNewScroll = new HashMap<Column, ScrollState>();
			for (final Entry<String, HosakaColumn> e : returnedColumns.entrySet()) {
				final String hash = e.getKey();
				final Column col = hashToCol.get(hash);
				final HosakaColumn before = toPush.get(hash);
				final HosakaColumn after = e.getValue();
				if (col != null && before != null &&
						(after.getUnreadTime() > before.getUnreadTime() ||
						(syncScroll && before.getScrollDirection() == ScrollDirection.UP && after.getItemTime() > before.getItemTime()))) {
					colToNewScroll.put(col, after.toScrollState());
				}
			}
			db.mergeAndStoreScrolls(colToNewScroll, syncScroll ? ScrollChangeType.UNREAD_AND_SCROLL : ScrollChangeType.UNREAD);
			LOG.i("Merged %s columns: %s.", colToNewScroll.size(), colToNewScroll);

			storeResult(db, toPush.size(), colToNewScroll.size(), null);
		}
		catch (final IOException e) {
			storeResult(db, toPush.size(), 0, e);
		}
		catch (final JSONException e) {
			storeResult(db, toPush.size(), 0, e);
		}
		finally {
			prov.shutdown();
		}
	}

	private void storeResult (final DbInterface db, final int pushedCount, final int pulledCount, final Exception e) {
		final String status;
		if (e != null) {
			status = String.format("Failed: %s", ExcpetionHelper.causeTrace(e)); //ES
			LOG.w(status);
		}
		else {
			status = String.format("Success: pushed %s and pulled %s columns.", pushedCount, pulledCount); //ES
		}
		db.storeValue(KvKeys.KEY_HOSAKA_STATUS,
				String.format("%s %s",
						DateHelper.formatDateTime(this, System.currentTimeMillis()),
						status));
	}

	private static final long NANO_ORIGIN = System.nanoTime();

	protected static long now () {
		return System.nanoTime() - NANO_ORIGIN;
	}

}

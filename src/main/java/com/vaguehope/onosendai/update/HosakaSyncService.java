package com.vaguehope.onosendai.update;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import com.vaguehope.onosendai.provider.hosaka.HosakaColumn;
import com.vaguehope.onosendai.provider.hosaka.HosakaProvider;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class HosakaSyncService extends DbBindingService {

	protected static final LogWrapper LOG = new LogWrapper("HSS");

	public static void startServiceIfConfigured (final Context context, final Config conf, final int columnId) {
		if (columnId > Integer.MIN_VALUE) return; // Not on manual single column.
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

		// TODO trigger UI to write current scroll state to DB.

		final Map<String, Column> hashToCol = new HashMap<String, Column>();
		final Map<String, HosakaColumn> toPush = new HashMap<String, HosakaColumn>();
		for (final Column col : conf.getColumns()) {
			if (InternalColumnType.fromColumn(col) != null) continue; // Do not sync internal columns.

			final String hash = HosakaColumn.columnHash(conf.getAccount(col.getAccountId()), col);
			hashToCol.put(hash, col);
			final ScrollState ss = db.getScroll(col.getId());
			// Always add all columns, even if sent before new values.
			// - Old / regressed values will be filtered server side.
			// - Values sent can be used to filter response.
			// - Also useful as do not know state of remote DB.
			toPush.put(hash, new HosakaColumn(null /* ss.getItemId(); TODO ScrollState to also store sid? */, ss.getItemTime(), ss.getUnreadTime()));
		}

		final HosakaProvider prov = new HosakaProvider();
		try {
			// Make POST even if not really sending anything new, as may be fetching new state.
			final Map<String, HosakaColumn> returnedColumns = prov.sendColumns(account, toPush);
			LOG.i("Sent %s: %s", account.getAccessToken(), toPush);

			final Map<Column, ScrollState> colToNewScroll = new HashMap<Column, ScrollState>();
			for (final Entry<String, HosakaColumn> e : returnedColumns.entrySet()) {
				final String hash = e.getKey();
				final Column col = hashToCol.get(hash);
				final HosakaColumn before = toPush.get(hash);
				final HosakaColumn after = e.getValue();
				// TODO currently only merge if unread time has moved.
				if (col != null && before != null && after.getUnreadTime() > before.getUnreadTime()) colToNewScroll.put(col, after.toScrollState());
			}
			db.mergeAndStoreScrolls(colToNewScroll);
			LOG.i("Merged %s columns: %s.", colToNewScroll.size(), colToNewScroll);

			// TODO Trigger UI to scroll to new position if new value is greater.

		}
		catch (final IOException e) {
			// TODO notify user in some way?
			LOG.w("Failed to update Hosaka: %s", ExcpetionHelper.causeTrace(e));
		}
		catch (final JSONException e) {
			// TODO notify user in some way?
			LOG.w("Failed to update Hosaka: %s", ExcpetionHelper.causeTrace(e));
		}
		finally {
			prov.shutdown();
		}
	}

}

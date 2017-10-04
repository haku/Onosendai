package com.vaguehope.onosendai.ui.pref;

import java.util.Map;
import java.util.Map.Entry;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.TimeRange;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;

public class ColumnStatsActivity extends Activity {

	private static final LogWrapper LOG = new LogWrapper("CT");

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // TODO check return value.
		setContentView(R.layout.columnstats);

		final ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

		final ListView statLst = (ListView) findViewById(R.id.statsList);
		final ArrayAdapter<StatsItem> statsAdp = new ArrayAdapter<StatsItem>(this, android.R.layout.simple_list_item_1);
		statLst.setAdapter(statsAdp);
		statLst.setOnItemClickListener(new StatClickListener(statsAdp));

		showStatsForAllColumns(statsAdp);
	}

	private void showStatsForAllColumns (final ArrayAdapter<StatsItem> adapter) {
		try {
			final Prefs prefs = new Prefs(getBaseContext());
			final Config conf = prefs.asConfig();
			new GetAllColumnStats(this, conf, adapter).execute();
		}
		catch (final Exception e) { // No point continuing if any exception.
			DialogHelper.alertAndClose(this, e);
			return;
		}
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private static class StatsItem {

		private final String title;
		private final Runnable action;

		public StatsItem (final String title) {
			this(title, null);
		}

		public StatsItem (final String title, final Runnable action) {
			this.title = title;
			this.action = action;
		}

		@Override
		public String toString () {
			return this.title;
		}

		public Runnable getAction () {
			return this.action;
		}
	}

	private class StatClickListener implements OnItemClickListener {

		private final ArrayAdapter<StatsItem> adapter;

		public StatClickListener (final ArrayAdapter<StatsItem> adapter) {
			this.adapter = adapter;
		}

		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			final StatsItem item = this.adapter.getItem(position);
			final Runnable action = item != null ? item.getAction() : null;
			if (action != null) action.run();
		}

	}

	private static abstract class StatsReadingTask extends DbBindingAsyncTask<Void, StatsItem, Exception> {

		protected final Activity activity;
		protected final ArrayAdapter<StatsItem> adapter;

		public StatsReadingTask (final ExecutorEventListener eventListener, final Activity activity, final ArrayAdapter<StatsItem> adapter) {
			super(eventListener, activity);
			this.activity = activity;
			this.adapter = adapter;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.activity.setProgressBarIndeterminateVisibility(true);
			this.adapter.clear();
		}

		@Override
		protected void onProgressUpdate (final StatsItem... values) {
			for (final StatsItem value : values) {
				this.adapter.add(value);
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.activity.setProgressBarIndeterminateVisibility(false);
			if (result != null) {
				LOG.e("Failed to gather column stats.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

	private static class GetAllColumnStats extends StatsReadingTask {

		private final Config conf;

		public GetAllColumnStats (final Activity activity, final Config config, final ArrayAdapter<StatsItem> adapter) {
			super(null, activity, adapter);
			this.conf = config;
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				publishProgress(new StatsItem(String.format(
						"%,d tweets ever",
						db.getTotalTweetsEverSeen()))); //ES
				for (final Column col : this.conf.getColumns()) {
					final TimeRange range = db.getColumnTimeRange(col.getId());
					final double tpd = range != null
							? (range.count / (double) range.rangeSeconds) * 60 * 60 * 24
							: -1;

					publishProgress(new StatsItem(String.format(
							"%s : %s /day", //ES
							col.getTitle(),
							tpd >= 0 ? roundSigFig(tpd, 3) : '?'),
							new Runnable() {
								@Override
								public void run () {
									new GetSingleColumnStats(GetAllColumnStats.this.activity, GetAllColumnStats.this.adapter, col).execute();
								}
							}));
				}
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

	}

	private static class GetSingleColumnStats extends StatsReadingTask {

		private final Column column;

		public GetSingleColumnStats (final Activity activity, final ArrayAdapter<StatsItem> adapter, final Column column) {
			super(null, activity, adapter);
			this.column = column;
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				final TimeRange range = db.getColumnTimeRange(this.column.getId());
				final long rangeCount = range != null ? range.count : 0;

				final Map<String, Long> stats = db.getColumnUserStats(this.column.getId());

				for (final Entry<String, Long> stat : stats.entrySet()) {
					final double tpd = range != null
							? (stat.getValue() / (double) range.rangeSeconds) * 60 * 60 * 24
							: -1;

					publishProgress(new StatsItem(String.format("%s : %s /day (%s / %s)",
							stat.getKey(),
							tpd >= 0 ? roundSigFig(tpd, 3) : '?',
							stat.getValue(),
							rangeCount)));
				}
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

	}

	// https://stackoverflow.com/questions/202302/rounding-to-an-arbitrary-number-of-significant-digits
	// http://stackoverflow.com/a/1581007
	protected static double roundSigFig (final double num, final int n) {
		if (num == 0) return 0;

		final double d = Math.ceil(Math.log10(num < 0 ? -num : num));
		final int power = n - (int) d;

		final double magnitude = Math.pow(10, power);
		final long shifted = Math.round(num * magnitude);
		return shifted / magnitude;
	}

}

package com.vaguehope.onosendai.ui.pref;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class ColumnStatsActivity extends Activity {

	private static final LogWrapper LOG = new LogWrapper("CT");

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // TODO check return value.
		setContentView(R.layout.columnstats);

		final ListView statLst = (ListView) findViewById(R.id.statsList);
		final ArrayAdapter<String> statsAdp = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		statLst.setAdapter(statsAdp);

		try {
			final Prefs prefs = new Prefs(getBaseContext());
			final Config conf = prefs.asConfig();
			new GetColumnStats(this, conf, statsAdp).execute();
		}
		catch (final Exception e) { // No point continuing if any exception.
			DialogHelper.alertAndClose(this, e);
			return;
		}
	}

	private static class GetColumnStats extends DbBindingAsyncTask<Void, String, Exception> {

		private final Activity activity;
		private final Config conf;
		private final ArrayAdapter<String> adapter;

		public GetColumnStats (final Activity activity, final Config config, final ArrayAdapter<String> adapter) {
			super(null, activity);
			this.activity = activity;
			this.conf = config;
			this.adapter = adapter;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.activity.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected void onProgressUpdate (final String... values) {
			for (final String value : values) {
				this.adapter.add(value);
			}
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				publishProgress(String.format("%,d tweets ever", db.getTotalTweetsEverSeen())); //ES
				for (final Column col : this.conf.getColumns()) {
					final double tph = db.getTweetsPerHour(col.getId());
					publishProgress(String.format("%s : %s /hour", col.getTitle(), tph >= 0 ? roundSigFig(tph, 3) : '?')); //ES
				}
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
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

package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.v4.util.Pair;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Filters;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.Selection;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class FiltersPrefFragment extends PreferenceFragment {

	public static final String KEY_SHOW_FILTERED = "pref_show_filtered";
	protected static final LogWrapper LOG = new LogWrapper("FP");

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshFiltersList();
	}

	protected Prefs getPrefs () {
		if (this.prefs == null) throw new IllegalStateException("Prefs has not been initialised.");
		return this.prefs;
	}

	protected void refreshFiltersList () {
		getPreferenceScreen().removeAll();

		final CheckBoxPreference showFiltered = new CheckBoxPreference(getActivity());
		showFiltered.setKey(KEY_SHOW_FILTERED);
		showFiltered.setTitle("Show filtered"); //ES
		showFiltered.setSummary("For testing filters."); //ES
		getPreferenceScreen().addPreference(showFiltered);

		final Preference reapplyFilters = new Preference(getActivity());
		reapplyFilters.setTitle("Reapply Filters"); //ES
		reapplyFilters.setSummary("Apply filter rules to already downloaded tweets"); //ES
		reapplyFilters.setOnPreferenceClickListener(this.reapplyFiltersListener);
		getPreferenceScreen().addPreference(reapplyFilters);

		final Preference addFilter = new Preference(getActivity());
		addFilter.setTitle("Add Filter"); //ES
		addFilter.setSummary("Plain string or regex"); //ES
		addFilter.setOnPreferenceClickListener(new AddFilterClickListener(this));
		getPreferenceScreen().addPreference(addFilter);

		for (final String filterId : getPrefs().readFilterIds()) {
			final String filter = getPrefs().readFilter(filterId);
			getPreferenceScreen().addPreference(new FilterDialogPref(getActivity(), filterId, filter, this));
		}
	}

	protected void promptNewFilter () {
		final String id = getPrefs().getNextFilterId();
		final FilterDialog dlg = new FilterDialog(getActivity());

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Filter (" + id + ")"); //ES
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					final String newFilter = dlg.getValue();
					getPrefs().writeFilter(id, newFilter);
				}
				catch (final Exception e) {
					DialogHelper.alert(getActivity(), "Failed to write new filter: ", e);
				}
				refreshFiltersList();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		final AlertDialog alertDialog = dlgBuilder.create();
		dlg.setOnValidateListener(new Listener<Boolean>() {
			@Override
			public void onAnswer (final Boolean valid) {
				alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(valid);
			}
		});
		alertDialog.show();
		dlg.validate();
	}

	protected void askDeleteFilter (final String filterId, final String filter) {
		DialogHelper.askYesNo(getActivity(),
				String.format("Delete the filter %s?", filter), //ES
				new Runnable() {
					@Override
					public void run () {
						getPrefs().deleteFilter(filterId);
						refreshFiltersList();
					}
				});
	}

	private static class AddFilterClickListener implements OnPreferenceClickListener {

		private final FiltersPrefFragment host;

		public AddFilterClickListener (final FiltersPrefFragment host) {
			this.host = host;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.host.promptNewFilter();
			return true;
		}
	}

	private final OnPreferenceClickListener reapplyFiltersListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			new ReapplyFiltersTask(getActivity(), FiltersPrefFragment.this.prefs).execute();
			return true;
		}
	};

	private static class ReapplyFiltersTask extends DbBindingAsyncTask<Void, String, Exception> {

		private final Prefs prefs;
		private ProgressDialog dialog;

		public ReapplyFiltersTask (final Context context, final Prefs prefs) {
			super(context);
			this.prefs = prefs;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(getContext(), "Reapplying Filters", "...", true); //ES
		}

		@Override
		protected void onProgressUpdate (final String... msgs) {
			if (msgs == null || msgs.length < 1) return;
			this.dialog.setMessage(msgs[0]);
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				publishProgress("Parsing filters..."); //ES
				final Filters filters = new Filters(this.prefs.readFilters());

				publishProgress("Reading tweets..."); //ES
				final TweetCursorReader reader = new TweetCursorReader();
				long processed = 0L;
				long totalChanges = 0L;
				final long startTime = System.currentTimeMillis();
				for (final Column column : this.prefs.asConfig().getColumns()) {
					if (InternalColumnType.fromColumn(column) != null) continue;
					final List<Pair<Long, Boolean>> changes = new ArrayList<Pair<Long, Boolean>>();
					final Cursor c = db.getTweetsCursor(column.getId(), Selection.ALL);
					try {
						if (c != null && c.moveToFirst()) {
							do {
								final boolean newFiltered = filters.matches(reader.readBody(c));
								if (reader.readFiltered(c) != newFiltered) {
									changes.add(new Pair<Long, Boolean>(reader.readUid(c), newFiltered));
								}
								processed += 1;
								if (processed % 100 == 0) publishProgress("Read " + processed + ", found " + changes.size() + " changes ..."); //ES
							}
							while (c.moveToNext());
						}
					}
					finally {
						IoHelper.closeQuietly(c);
					}
					publishProgress("Saving " + changes.size() + " changes in " + column.getTitle() + "..."); //ES
					db.updateTweetFiltered(changes);
					LOG.i("Saved %s filter changes in %s.", changes.size(), column.getTitle());
					totalChanges += changes.size();
				}
				LOG.i("Checked %s filters against %s tweets and wrote %s changes in %sms.",
						filters.size(), processed, totalChanges, System.currentTimeMillis() - startTime);
				return null;
			}
			catch (final Exception e) {
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.dialog.dismiss();
			if (result != null) {
				LOG.e("Error while reapplying filters.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

}

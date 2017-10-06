package com.vaguehope.onosendai.ui.pref;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.v4.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Filters;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.Selection;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.DateHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class FiltersPrefFragment extends PreferenceFragment {

	public static final String KEY_SHOW_FILTERED = "pref_show_filtered";
	private static final String FILTERS_FILE_PREFIX = "onosendai-filters-";
	private static final String FILTERS_FILE_EXT = ".txt";
	protected static final LogWrapper LOG = new LogWrapper("FP");

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		refreshFiltersList();
	}

	protected Prefs getPrefs () {
		if (this.prefs == null) this.prefs = new Prefs(getPreferenceManager());
		return this.prefs;
	}

	protected void refreshFiltersList () {
		getPreferenceScreen().removeAll();

		final CheckBoxPreference showFiltered = new CheckBoxPreference(getActivity());
		showFiltered.setKey(KEY_SHOW_FILTERED);
		showFiltered.setTitle("Show filtered"); //ES
		showFiltered.setSummary("For testing filters.  Does not work on seamless columns."); //ES
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

	private static final int MNU_FILE_READ = 1001;
	private static final int MNU_FILE_WRITE = 1002;

	@Override
	public void onCreateOptionsMenu (final Menu menu, final MenuInflater inflater) {
		menu.add(Menu.NONE, MNU_FILE_READ, Menu.NONE, "Read from file"); //ES
		menu.add(Menu.NONE, MNU_FILE_WRITE, Menu.NONE, "Write to file"); //ES
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case MNU_FILE_READ:
				startFileRead();
				return true;
			case MNU_FILE_WRITE:
				startFileWrite();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void promptNewFilter () {
		promptNewFilter(getActivity(), getPrefs(), new Listener<String>() {
			@Override
			public void onAnswer (final String answer) {
				refreshFiltersList();
			}
		}, null);
	}

	protected static void promptNewFilter (final Context context, final Prefs prefs, final Listener<String> onAdded, final String prefilFilter) {
		final String id = prefs.getNextFilterId();
		final FilterDialog dlg = new FilterDialog(context, prefilFilter, false);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
		dlgBuilder.setTitle("New Filter (" + id + ")"); //ES
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					final String newFilter = dlg.getValue();
					prefs.writeFilter(id, newFilter);
					onAdded.onAnswer(newFilter);
				}
				catch (final Exception e) {
					DialogHelper.alert(context, "Failed to write new filter: ", e);
				}
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
		dlg.validateAndNotify();
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
			new ReapplyFiltersTask(getActivity(), getPrefs()).execute();
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
								final boolean newFiltered = filters.matches(reader.readBody(c), reader.readUsername(c), reader.readOwnerUsername(c));
								if (reader.readFiltered(c) != newFiltered) {
									changes.add(new Pair<Long, Boolean>(reader.readUid(c), newFiltered));
									totalChanges += 1;
								}
								processed += 1;
								if (processed % 100 == 0) publishProgress("Read " + processed + ", found " + totalChanges + " changes ..."); //ES
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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void startFileRead () {
		final File[] files = Environment.getExternalStorageDirectory().listFiles(new FilenameFilter() {
			@Override
			public boolean accept (final File dir, final String filename) {
				return filename.startsWith(FILTERS_FILE_PREFIX) && filename.endsWith(FILTERS_FILE_EXT);
			}
		});
		DialogHelper.askItem(getActivity(), "Select File", files, new Function<File, String>() { //ES
			@Override
			public String exec (final File file) {
				return file.getName();
			}
		}, new Listener<File>() {
			@Override
			public void onAnswer (final File file) {
				new FileReadTask(FiltersPrefFragment.this, getPrefs(), file).execute();
			}
		});
	}

	private void startFileWrite () {
		final File file = new File(Environment.getExternalStorageDirectory(),
				FILTERS_FILE_PREFIX
				+ DateHelper.standardDateTimeFormat().format(new Date())
				+ FILTERS_FILE_EXT);
		new FileWriteTask(FiltersPrefFragment.this, getPrefs(), file).execute();
	}

	private static class FileReadTask extends AsyncTask<Void, Void, Result<Pair<Integer, Integer>>> {

		private final FiltersPrefFragment host;
		private final Prefs prefs;
		private final File file;
		private ProgressDialog dialog;

		public FileReadTask (final FiltersPrefFragment host, final Prefs prefs, final File file) {
			this.host = host;
			this.prefs = prefs;
			this.file = file;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getActivity(), "Filters", "Fetching...", true); //ES
		}

		@Override
		protected Result<Pair<Integer, Integer>> doInBackground (final Void... params) {
			try {
				final Collection<String> bps = IoHelper.fileToCollection(this.file);

				final Set<String> existing = new HashSet<String>();
				for (final String filterId : this.prefs.readFilterIds()) {
					existing.add(this.prefs.readFilter(filterId));
				}

				int merged = 0;
				for (final String bp : bps) {
					if (!existing.contains(bp)) {
						this.prefs.writeFilter(this.prefs.getNextFilterId(), bp);
						merged += 1;
					}
				}

				LOG.i("Read %s filters, added %s new filters.", bps.size(), merged);
				return new Result<Pair<Integer, Integer>>(new Pair<Integer, Integer>(bps.size(), merged));
			}
			catch (final Exception e) { // NOSONAR want to report errors to UI.
				return new Result<Pair<Integer, Integer>>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<Pair<Integer, Integer>> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				this.host.refreshFiltersList();
				DialogHelper.alert(this.host.getActivity(), "Read " + result.getData().first + " and added " + result.getData().second + "."); //ES
			}
			else {
				LOG.e("Error reading filters.", result.getE());
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

	private static class FileWriteTask extends AsyncTask<Void, String, Result<Integer>> {

		private final FiltersPrefFragment host;
		private final Prefs prefs;
		private final File file;
		private ProgressDialog dialog;

		public FileWriteTask (final FiltersPrefFragment host, final Prefs prefs, final File file) {
			this.host = host;
			this.prefs = prefs;
			this.file = file;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getActivity(), "Filters", "Writing...", true); //ES
		}

		@Override
		protected void onProgressUpdate (final String... msgs) {
			if (msgs == null || msgs.length < 1) return;
			this.dialog.setMessage(msgs[0]);
		}

		@Override
		protected Result<Integer> doInBackground (final Void... params) {
			try {
				final LinkedHashSet<String> toPush = new LinkedHashSet<String>();
				for (final String filterId : this.prefs.readFilterIds()) {
					toPush.add(this.prefs.readFilter(filterId));
				}
				IoHelper.collectionToFile(toPush, this.file);
				LOG.d("Write %s filters to %s.", toPush.size(), this.file.getAbsolutePath());
				return new Result<Integer>(toPush.size());
			}
			catch (final Exception e) { // NOSONAR want to report errors to UI.
				return new Result<Integer>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<Integer> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				DialogHelper.alert(this.host.getActivity(), "Wrote " + result.getData() + " filters.\n\n" + this.file.getAbsolutePath()); //ES
			}
			else {
				LOG.e("Error writing filters.", result.getE());
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

}

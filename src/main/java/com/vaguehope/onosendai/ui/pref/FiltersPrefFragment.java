package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.support.v4.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Filters;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.Selection;
import com.vaguehope.onosendai.storage.TweetCursorReader;
import com.vaguehope.onosendai.storage.VolatileKvStore;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.Titleable;

public class FiltersPrefFragment extends PreferenceFragment {

	public static final String KEY_SHOW_FILTERED = "pref_show_filtered";
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

	private static final int MNU_SW_PULL = 1001;
	private static final int MNU_SW_PUSH = 1002;

	@Override
	public void onCreateOptionsMenu (final Menu menu, final MenuInflater inflater) {
		menu.add(Menu.NONE, MNU_SW_PULL, Menu.NONE, "Pull from SuccessWhale"); //ES
		menu.add(Menu.NONE, MNU_SW_PUSH, Menu.NONE, "Push to SuccessWhale"); //ES
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case MNU_SW_PULL:
				startSwPull();
				return true;
			case MNU_SW_PUSH:
				startSwPush();
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
								final boolean newFiltered = filters.matches(reader.readBody(c), reader.readUsername(c), reader.readUserSubtitle(c));
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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Collection<Account> readAccountsOrAlert () {
		try {
			return this.prefs.readAccounts();
		}
		catch (final JSONException e) {
			DialogHelper.alert(getActivity(), "Failed to read accounts.", e); //ES
			return null;
		}
	}

	private void askSwAccount (final Listener<Account> onSwAccount) {
		final Collection<Account> acs = readAccountsOrAlert();
		if (acs == null) return;
		final List<Account> swAcs = new ArrayList<Account>();
		for (final Account a : acs) {
			if (a.getProvider() == AccountProvider.SUCCESSWHALE) swAcs.add(a);
		}
		if (swAcs.size() < 1) {
			DialogHelper.alert(getActivity(), "No SW accounts configured."); //ES
		}
		else if (swAcs.size() > 1) {
			DialogHelper.askItem(getActivity(), "Account", new ArrayList<Account>(acs), onSwAccount);
		}
		else {
			onSwAccount.onAnswer(swAcs.get(0));
		}
	}

	private void startSwPull () {
		askSwAccount(new Listener<Account>() {
			@Override
			public void onAnswer (final Account account) {
				new SwPullTask(FiltersPrefFragment.this, getPrefs(), account).execute();
			}
		});
	}

	private enum MergeMode implements Titleable {
		OVERWRITE("Overwrite"), //ES
		MERGE("Merge"); //ES
		private final String title;

		private MergeMode (final String title) {
			this.title = title;
		}

		@Override
		public String getUiTitle () {
			return this.title;
		}
	}

	private void startSwPush () {
		askSwAccount(new Listener<Account>() {
			@Override
			public void onAnswer (final Account account) {
				startSwPush(account);
			}
		});
	}

	protected void startSwPush (final Account account) {
		DialogHelper.askItem(getActivity(), "Mode", MergeMode.values(), new Listener<MergeMode>() { //ES
			@Override
			public void onAnswer (final MergeMode mergeMode) {
				new SwPushTask(FiltersPrefFragment.this, getPrefs(), account, mergeMode).execute();
			}
		});
	}

	private static class SwPullTask extends AsyncTask<Void, Void, Result<Pair<Integer, Integer>>> {

		private final FiltersPrefFragment host;
		private final Prefs prefs;
		private final Account account;
		private ProgressDialog dialog;

		public SwPullTask (final FiltersPrefFragment host, final Prefs prefs, final Account account) {
			this.host = host;
			this.prefs = prefs;
			this.account = account;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getActivity(), "Filters", "Fetching...", true); //ES
		}

		@Override
		protected Result<Pair<Integer, Integer>> doInBackground (final Void... params) {
			final SuccessWhaleProvider swProv = new SuccessWhaleProvider(new VolatileKvStore());
			try {
				final List<String> bps = swProv.getBannedPhrases(this.account);

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

				LOG.i("Fetched %s filters from SW, added %s new filters.", bps.size(), merged);
				return new Result<Pair<Integer, Integer>>(new Pair<Integer, Integer>(bps.size(), merged));
			}
			catch (final Exception e) { // NOSONAR want to report errors to UI.
				return new Result<Pair<Integer, Integer>>(e);
			}
			finally {
				swProv.shutdown();
			}
		}

		@Override
		protected void onPostExecute (final Result<Pair<Integer, Integer>> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				this.host.refreshFiltersList();
				DialogHelper.alert(this.host.getActivity(), "Fetched " + result.getData().first + " and added " + result.getData().second + "."); //ES
			}
			else {
				LOG.e("Error fetching filters.", result.getE());
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

	private static class SwPushTask extends AsyncTask<Void, String, Result<Integer>> {

		private final FiltersPrefFragment host;
		private final Prefs prefs;
		private final Account account;
		private final MergeMode mergeMode;
		private ProgressDialog dialog;

		public SwPushTask (final FiltersPrefFragment host, final Prefs prefs, final Account account, final MergeMode mergeMode) {
			this.host = host;
			this.prefs = prefs;
			this.account = account;
			this.mergeMode = mergeMode;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getActivity(), "Filters", "Pushing...", true); //ES
		}

		@Override
		protected void onProgressUpdate (final String... msgs) {
			if (msgs == null || msgs.length < 1) return;
			this.dialog.setMessage(msgs[0]);
		}

		@Override
		protected Result<Integer> doInBackground (final Void... params) {
			final SuccessWhaleProvider swProv = new SuccessWhaleProvider(new VolatileKvStore());
			try {
				final LinkedHashSet<String> toPush = new LinkedHashSet<String>();
				if (this.mergeMode == MergeMode.MERGE) {
					publishProgress("Fetching current..."); //ES
					toPush.addAll(swProv.getBannedPhrases(this.account));
					LOG.i("Fetched %s existing filters from SW.");
					publishProgress("Merging and pushing..."); //ES
				}
				else if (this.mergeMode == MergeMode.OVERWRITE) {
					// Nothing.
				}
				else {
					throw new IllegalStateException("Unknown: " + this.mergeMode);
				}
				for (final String filterId : this.prefs.readFilterIds()) {
					toPush.add(this.prefs.readFilter(filterId));
				}
				swProv.setBannedPhrases(this.account, new ArrayList<String>(toPush));
				LOG.d("Pushed %s filters to SW.", toPush.size());
				return new Result<Integer>(toPush.size());
			}
			catch (final Exception e) { // NOSONAR want to report errors to UI.
				return new Result<Integer>(e);
			}
			finally {
				swProv.shutdown();
			}
		}

		@Override
		protected void onPostExecute (final Result<Integer> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				DialogHelper.alert(this.host.getActivity(), "Pushed " + result.getData() + " filters."); //ES
			}
			else {
				LOG.e("Error pushing filters.", result.getE());
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

}

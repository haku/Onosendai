package com.vaguehope.onosendai.ui.pref;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.pref.ColumnChooser.ColumnChoiceListener;
import com.vaguehope.onosendai.update.KvKeys;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class ColumnsPrefFragment extends PreferenceFragment {

	private static final LogWrapper LOG = new LogWrapper("CPF");

	private Prefs prefs;
	private ColumnChooser columnChooser;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshColumnsList();
		this.columnChooser = new ColumnChooser(getActivity(), this.prefs, this.columnChoiceListener);
	}

	protected Prefs getPrefs () {
		return this.prefs;
	}

	protected void refreshColumnsList () {
		getPreferenceScreen().removeAll();

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Column"); //ES
		pref.setSummary("Add a new column of updates"); //ES
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		getPreferenceScreen().addPreference(pref);

		try {
			final Config config = getPrefs().asConfig();
			for (final Column column : config.getColumns()) {
				final List<Account> accounts = config.getAccounts(column.uniqAccountIds());
				getPreferenceScreen().addPreference(new ColumnDialogPreference(getActivity(), column, accounts, this));
			}
		}
		catch (final JSONException e) {
			DialogHelper.alertAndClose(getActivity(), "Error reading preferences:", e);
		}
	}

	private final ColumnChoiceListener columnChoiceListener = new ColumnChoiceListener() {
		@Override
		public void onColumn (final Account account, final String resource, final String title) {
			promptAddColumn(account, resource, title);
		}
	};

	protected void promptAddColumn () {
		this.columnChooser.promptAddColumn();
	}

	protected void promptAddColumn (final Account account, final String resource, final String title) {
		final int id = getPrefs().getNextColumnId();
		final ColumnDialog dlg = new ColumnDialog(getActivity(), this.prefs, id);
		if (resource != null) dlg.setFeeds(Collections.singleton(new ColumnFeed(account != null ? account.getId() : null, resource)));
		if (title != null) dlg.setTitle(title);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Column (" + id + ")"); //ES
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					final Column column = dlg.getValue();
					getPrefs().writeNewColumn(column);
					moveColumnToPosition(column, dlg.getPosition());
				}
				catch (final JSONException e) {
					DialogHelper.alert(getActivity(), "Failed to write new column: ", e);
				}
				refreshColumnsList();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER); //ES
		dlgBuilder.create().show();
	}

	protected void moveColumnToPosition (final Column column, final int newPosition) {
		this.prefs.moveColumnToPosition(column.getId(), newPosition);
	}

	protected void askDeleteColumn (final Column column) {
		DialogHelper.askYesNo(getActivity(),
				String.format("Delete the column %s and all its data?", column.getUiTitle()), //ES
				new Runnable() {
					@Override
					public void run () {
						try {
							discardColumnFromExcludes(column);
							deleteDataForColumn(column);
							getPrefs().deleteColumn(column);
							refreshColumnsList();
						}
						catch (final Exception e) { // NOSONAR Need to report errors.
							DialogHelper.alert(getActivity(), e);
						}
					}
				});
	}

	protected void discardColumnFromExcludes (final Column delCol) throws JSONException {
		final Integer delColId = Integer.valueOf(delCol.getId());
		for (final Column otherCol : this.prefs.readColumns()) {
			if (delCol.getId() == otherCol.getId()) continue;
			if (setContains(otherCol.getExcludeColumnIds(), delColId)) {
				final Set<Integer> newExcludeColumnIds = copyOfSetWithout(otherCol.getExcludeColumnIds(), delColId);
				this.prefs.writeUpdatedColumn(new Column(newExcludeColumnIds, otherCol));
			}
		}
	}

	protected void deleteDataForColumn (final Column column) {
		final OsPreferenceActivity act = (OsPreferenceActivity) getActivity();
		final DbInterface db = act.getDb();
		if (db == null) throw new IllegalStateException("Database not bound, aborting column deletion.");
		db.deleteTweets(column);
		db.deleteValue(KvKeys.colLastPushTime(column));
		db.deleteValue(KvKeys.colLastRefreshError(column));
		db.deleteValue(KvKeys.colLastRefreshTime(column));
		for (final ColumnFeed feed : column.getFeeds()) {
			db.deleteValue(KvKeys.feedSinceId(column, feed));
		}
		LOG.i("Deleted: %s", column);
	}

	protected void deleteDataForFeeds (final Column column, final Set<ColumnFeed> removedFeeds) {
		final OsPreferenceActivity act = (OsPreferenceActivity) getActivity();
		final DbInterface db = act.getDb();
		if (db == null) throw new IllegalStateException("Database not bound, aborting column deletion.");
		for (final ColumnFeed feed : removedFeeds) {
			db.deleteValue(KvKeys.feedSinceId(column, feed));
		}
		LOG.i("Deleted matadata for feeds: %s", removedFeeds);
	}

	private static class AddAcountClickListener implements OnPreferenceClickListener {

		private final ColumnsPrefFragment columnsPrefFragment;

		public AddAcountClickListener (final ColumnsPrefFragment columnsPrefFragment) {
			this.columnsPrefFragment = columnsPrefFragment;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.columnsPrefFragment.promptAddColumn();
			return true;
		}
	}

	private static <T> boolean setContains (final Set<T> set, final T item) {
		if (set == null) return false;
		return set.contains(item);
	}

	private static <T> Set<T> copyOfSetWithout (final Set<T> set, final T item) {
		final Set<T> newSet = new HashSet<T>(set);
		if (!newSet.remove(item)) throw new IllegalStateException("Failed to remove " + set + " from " + newSet + ".");
		return newSet;
	}

}

package com.vaguehope.onosendai.ui.pref;

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
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.pref.ColumnChooser.ColumnChoiceListener;
import com.vaguehope.onosendai.util.DialogHelper;

public class ColumnsPrefFragment extends PreferenceFragment {

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
		pref.setTitle("Add Column");
		pref.setSummary("Add a new column of updates");
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		getPreferenceScreen().addPreference(pref);

		try {
			final Config config = getPrefs().asConfig();
			for (final Column column : config.getColumns()) {
				final Account account = config.getAccount(column.getAccountId());
				getPreferenceScreen().addPreference(new ColumnDialogPreference(getActivity(), column, account, this));
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
		if (account == null) throw new IllegalArgumentException("Account can not be null.");

		final int id = getPrefs().getNextColumnId();
		final ColumnDialog dlg = new ColumnDialog(getActivity(), this.prefs, id, account.getId());
		if (resource != null) dlg.setResource(resource);
		if (title != null) dlg.setTitle(title);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Column (" + id + ")");
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
		dlgBuilder.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		dlgBuilder.create().show();
	}

	protected void moveColumnToPosition (final Column column, final int newPosition) {
		this.prefs.moveColumnToPosition(column.getId(), newPosition);
	}

	protected void askDeleteColumn (final Column column) {
		DialogHelper.askYesNo(getActivity(),
				String.format("Delete the column %s and all its data?", column.getUiTitle()),
				new Runnable() {
					@Override
					public void run () {
						deleteDataForColumn(column);
						getPrefs().deleteColumn(column);
						refreshColumnsList();
					}
				});
	}

	protected void deleteDataForColumn (final Column column) {
		final OsPreferenceActivity act = (OsPreferenceActivity) getActivity();
		final DbInterface db = act.getDb();
		if (db == null) {
			DialogHelper.alert(getActivity(), "Database not bound, aborting column deletion.");
			return;
		}
		db.deleteTweets(column);
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

}

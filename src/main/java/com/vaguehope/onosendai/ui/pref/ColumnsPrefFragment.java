package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;

public class ColumnsPrefFragment extends PreferenceFragment {

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshColumnsList();
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

		final List<Integer> columnIds = getPrefs().readColumnIds();
		for (final Integer columnId : columnIds) {
			try {
				final Column column = getPrefs().readColumn(columnId);
				getPreferenceScreen().addPreference(new ColumnDialogPreference(getActivity(), column, this));
			}
			catch (final JSONException e) {
				DialogHelper.alert(getActivity(), "Failed to read column: ", e);
			}
		}
	}

	protected void promptAddColumn () {
		final int id = getPrefs().getNextColumnId();
		final ColumnDialog dlg = new ColumnDialog(getActivity(), id);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Column (" + id + ")");
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					getPrefs().writeNewColumn(dlg.getValue());
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

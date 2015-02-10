package com.vaguehope.onosendai.ui.pref;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

public class FiltersPrefFragment extends PreferenceFragment {

	public static final String KEY_SHOW_FILTERED = "pref_show_filtered";

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
		showFiltered.setTitle("Show filtered");
		showFiltered.setSummary("For testing filters.");
		getPreferenceScreen().addPreference(showFiltered);

		final Preference addFilter = new Preference(getActivity());
		addFilter.setTitle("Add Filter");
		addFilter.setSummary("Plain string or regex");
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
		dlgBuilder.setTitle("New Filter (" + id + ")");
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
				String.format("Delete the filter %s?", filter),
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

}

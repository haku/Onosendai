package com.vaguehope.onosendai.ui.pref;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.model.PrefetchImages;

public class FetchingPrefFragment extends PreferenceFragment {

	public static final String KEY_PREFETCH_MEDIA = "pref_prefetch_media";

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		addPrefetchMedia();
		addColumnStats();
	}

	private void addPrefetchMedia () {
		final ListPreference pref = new ListPreference(getActivity());
		pref.setKey(KEY_PREFETCH_MEDIA);
		pref.setTitle("Prefetch Media");
		pref.setSummary("Fetch new pictures during background updates.");
		pref.setEntries(PrefetchImages.prefEntries());
		pref.setEntryValues(PrefetchImages.prefEntryValues());
		pref.setDefaultValue(PrefetchImages.NO.getValue());
		getPreferenceScreen().addPreference(pref);
	}

	private void addColumnStats () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Column stats");
		pref.setOnPreferenceClickListener(this.columnStatsClickListener);
		getPreferenceScreen().addPreference(pref);
	}

	private final OnPreferenceClickListener columnStatsClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			showColumnStats();
			return true;
		}
	};

	protected void showColumnStats () {
		startActivity(new Intent(getActivity(), ColumnStatsActivity.class));
	}

}

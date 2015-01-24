package com.vaguehope.onosendai.ui.pref;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Prefs;

public class UiPrefFragment extends PreferenceFragment {

	private static final String DEFAULT_COUNT = "default";
	private static final CharSequence[] COUNTS = new CharSequence[] { DEFAULT_COUNT, "1", "2", "3", "4", "5", "6", "7", "8" };

	private static final String KEY_COLUMNS_PORTRAIT = "pref_columns_portrait";
	private static final String KEY_COLUMNS_LANDSCAPE = "pref_columns_landscape";

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		addColumnsCount(KEY_COLUMNS_PORTRAIT, "Visible columns, portrait");
		addColumnsCount(KEY_COLUMNS_LANDSCAPE, "Visible columns, landscape");
	}

	private void addColumnsCount (final String key, final String title) {
		final ListPreference pref = new ListPreference(getActivity());
		pref.setKey(key);
		pref.setTitle(title);
		pref.setEntries(COUNTS);
		pref.setEntryValues(COUNTS);
		pref.setDefaultValue(DEFAULT_COUNT);
		getPreferenceScreen().addPreference(pref);
	}

	/*
	 * Returns 0.f to 1.f.
	 */
	public static float readColumnWidth (final Activity activity, final Prefs prefs) {
		final int orientation = activity.getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			final String str = prefs.getSharedPreferences().getString(KEY_COLUMNS_PORTRAIT, null);
			if (str != null && !DEFAULT_COUNT.equals(str)) return 1f / Integer.parseInt(str);
		}
		else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			final String str = prefs.getSharedPreferences().getString(KEY_COLUMNS_LANDSCAPE, null);
			if (str != null && !DEFAULT_COUNT.equals(str)) return 1f / Integer.parseInt(str);
		}
		return Float.parseFloat(activity.getResources().getString(R.string.column_width));
	}
}

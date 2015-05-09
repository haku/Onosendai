package com.vaguehope.onosendai.ui.pref;

import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.SupportedLocales;
import com.vaguehope.onosendai.util.LocaleHelper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.widget.ComboPreference;

public class UiPrefFragment extends PreferenceFragment {

	private static final String DEFAULT_COUNT = "default";
	private static final CharSequence[] COUNTS = new CharSequence[] { DEFAULT_COUNT, "1", "2", "3", "4", "5", "6", "7", "8" };

	private static final String KEY_LOCALE = "pref_locale";
	private static final String KEY_COLUMNS_PORTRAIT = "pref_columns_portrait";
	private static final String KEY_COLUMNS_LANDSCAPE = "pref_columns_landscape";
	private static final String KEY_COLUMNS_RTL = "pref_columns_rtl";

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		addLocalePref();
		addColumnsCount(KEY_COLUMNS_PORTRAIT, "Visible columns, portrait"); //ES
		addColumnsCount(KEY_COLUMNS_LANDSCAPE, "Visible columns, landscape"); //ES
		addColumnsRtl();
	}

	private void addLocalePref () {
		final ListPreference pref = new ComboPreference(getActivity());
		pref.setKey(KEY_LOCALE);
		pref.setTitle("Locale"); //ES
		pref.setEntries(SupportedLocales.prefEntries());
		pref.setEntryValues(SupportedLocales.prefEntryValues());
		pref.setDefaultValue(SupportedLocales.DEFAULT.getValue());
		pref.setOnPreferenceChangeListener(this.onLocaleChangeListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addColumnsCount (final String key, final String title) {
		final ListPreference pref = new ComboPreference(getActivity());
		pref.setKey(key);
		pref.setTitle(title);
		pref.setEntries(COUNTS);
		pref.setEntryValues(COUNTS);
		pref.setDefaultValue(DEFAULT_COUNT);
		getPreferenceScreen().addPreference(pref);
	}

	private void addColumnsRtl () {
		final CheckBoxPreference pref = new CheckBoxPreference(getActivity());
		pref.setKey(KEY_COLUMNS_RTL);
		pref.setTitle("Columns RTL"); //ES
		getPreferenceScreen().addPreference(pref);
	}

	final OnPreferenceChangeListener onLocaleChangeListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange (final Preference preference, final Object newValue) {
			final Locale newLocale = readLocale((String) newValue);
			LocaleHelper.setLocale(getActivity(), newLocale);
			return true;
		}
	};

	/**
	 * Returns null for default.
	 */
	public static Locale readLocale (final Context context) {
		return readLocale(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_LOCALE, null));
	}

	protected static Locale readLocale (final String rawLocale) {
		if (StringHelper.isEmpty(rawLocale)) return null;
		final String[] parts = rawLocale.split("_");
		if (parts.length == 1) {
			return new Locale(parts[0]);
		}
		else if (parts.length == 2) {
			return new Locale(parts[0], parts[1]);
		}
		return new Locale(parts[0], parts[1], parts[2]);
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

	public static boolean readColumnsRtl (final Prefs prefs) {
		return prefs.getSharedPreferences().getBoolean(KEY_COLUMNS_RTL, false);
	}

}

package com.vaguehope.onosendai.util;

import android.content.Context;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

public abstract class PrefCache<T> implements OnPreferenceChangeListener {

	private final String prefKey;
	private final String defaultVal;

	private volatile T cache;

	public PrefCache (final String prefKey, final String defaultVal) {
		this.prefKey = prefKey;
		this.defaultVal = defaultVal;
	}

	@Override
	public boolean onPreferenceChange (final Preference preference, final Object newValue) {
		this.cache = null;
		return true;
	}

	public T read (final Context context) {
		if (this.cache == null) {
			this.cache = parse(PreferenceManager.getDefaultSharedPreferences(context).getString(this.prefKey, this.defaultVal));
		}
		return this.cache;
	}

	protected abstract T parse (String s);

}

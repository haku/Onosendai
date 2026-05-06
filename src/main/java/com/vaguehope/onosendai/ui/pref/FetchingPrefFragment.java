package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.model.PrefetchMode;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.PrefCache;

public class FetchingPrefFragment extends PreferenceFragment {

	public static final String KEY_BAT_UPDATE = "pref_bat_update";
	public static final String KEY_BAT_SEND = "pref_bat_send";
	public static final String KEY_PREFETCH_MEDIA = "pref_prefetch_media";
	public static final String KEY_PREFETCH_LINKS = "pref_prefetch_links";
	public static final String KEY_SYNC_SCROLL = "pref_sync_scroll";

	private static final String DEFAULT_BAT_UPDATE = "30";
	private static final String DEFAULT_BAT_SEND = "15";
	private static final CharSequence[] BATTERY_LEVELS = new CharSequence[] { DEFAULT_BAT_UPDATE, "20", "15", "10" };

	private static final BatPrefCache batUpdatePrefCache = new BatPrefCache(KEY_BAT_UPDATE, DEFAULT_BAT_UPDATE);
	private static final BatPrefCache batSendPrefCache = new BatPrefCache(KEY_BAT_SEND, DEFAULT_BAT_SEND);

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		addBatLevel(KEY_BAT_UPDATE, "Min battery for backgrond updates", DEFAULT_BAT_UPDATE, batUpdatePrefCache);
		addBatLevel(KEY_BAT_SEND, "Min battery for retrying sends", DEFAULT_BAT_SEND, batSendPrefCache);
		addPrefetchMedia();
		addPrefetchLinks();
		addColumnStats();
		addSyncScroll();
	}

	private void addBatLevel (final String key, final CharSequence title, final CharSequence defVal, final OnPreferenceChangeListener changeListener) {
		final ListPreference pref = new ListPreference(getActivity());
		pref.setKey(key);
		pref.setTitle(title);
		pref.setEntries(CollectionHelper.map(BATTERY_LEVELS, new Function<CharSequence, CharSequence>() {
			@Override
			public CharSequence exec (final CharSequence input) {
				return input + "%";
			}
		}, new ArrayList<CharSequence>()).toArray(new CharSequence[BATTERY_LEVELS.length]));
		pref.setEntryValues(BATTERY_LEVELS);
		pref.setSummary("%s");
		pref.setDefaultValue(defVal);
		pref.setOnPreferenceChangeListener(changeListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addPrefetchMedia () {
		final ListPreference pref = new ListPreference(getActivity());
		pref.setKey(KEY_PREFETCH_MEDIA);
		pref.setTitle("Prefetch media"); //ES
		pref.setSummary("Fetch new pictures during background updates: %s"); //ES
		pref.setEntries(PrefetchMode.prefEntries());
		pref.setEntryValues(PrefetchMode.prefEntryValues());
		pref.setDefaultValue(PrefetchMode.NO.getValue());
		getPreferenceScreen().addPreference(pref);
	}

	private void addPrefetchLinks () {
		final ListPreference pref = new ListPreference(getActivity());
		pref.setKey(KEY_PREFETCH_LINKS);
		pref.setTitle("Prefetch links"); //ES
		pref.setSummary("Fetch new link titles during background updates: %s"); //ES
		pref.setEntries(PrefetchMode.prefEntries());
		pref.setEntryValues(PrefetchMode.prefEntryValues());
		pref.setDefaultValue(PrefetchMode.NO.getValue());
		getPreferenceScreen().addPreference(pref);
	}

	private void addColumnStats () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Column stats"); //ES
		pref.setOnPreferenceClickListener(this.columnStatsClickListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addSyncScroll () {
		final CheckBoxPreference pref = new CheckBoxPreference(getActivity());
		pref.setKey(KEY_SYNC_SCROLL);
		pref.setTitle("Sync column scroll position"); //ES
		pref.setSummary("But only if last scroll was upward."); //ES
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

	public static float readMinBatForUpdate (final Context context) {
		return batUpdatePrefCache.read(context);
	}

	public static float readMinBatForSend (final Context context) {
		return batSendPrefCache.read(context);
	}

	public static class BatPrefCache extends PrefCache<Float> {

		public BatPrefCache (final String prefKey, final String defaultVal) {
			super(prefKey, defaultVal);
		}

		@Override
		protected Float parse (final String s) {
			return Integer.parseInt(s) / 100f;
		}

	}

}

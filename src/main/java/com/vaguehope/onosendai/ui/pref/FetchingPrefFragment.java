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

import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.PrefetchMode;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.update.KvKeys;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.PrefCache;
import com.vaguehope.onosendai.util.StringHelper;

public class FetchingPrefFragment extends PreferenceFragment {

	public static final String KEY_BAT_UPDATE = "pref_bat_update";
	public static final String KEY_BAT_SEND = "pref_bat_send";
	public static final String KEY_PREFETCH_MEDIA = "pref_prefetch_media";
	public static final String KEY_PREFETCH_LINKS = "pref_prefetch_links";
	public static final String KEY_SYNC_SCROLL = "pref_sync_scroll";

	private static final LogWrapper LOG = new LogWrapper("FPF");

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
		addHosakaStatus();
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
		pref.setSummary("Fetch new pictures during background updates."); //ES
		pref.setEntries(PrefetchMode.prefEntries());
		pref.setEntryValues(PrefetchMode.prefEntryValues());
		pref.setDefaultValue(PrefetchMode.NO.getValue());
		getPreferenceScreen().addPreference(pref);
	}

	private void addPrefetchLinks () {
		final ListPreference pref = new ListPreference(getActivity());
		pref.setKey(KEY_PREFETCH_LINKS);
		pref.setTitle("Prefetch links"); //ES
		pref.setSummary("Fetch new link titles during background updates."); //ES
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

	private void addHosakaStatus () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Hosaka sync status"); //ES
		pref.setSummary("Loading..."); //ES
		getPreferenceScreen().addPreference(pref);
		new GetHosakaStatus(getActivity(), pref).execute();
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

	private static class GetHosakaStatus extends DbBindingAsyncTask<Void, String, Exception> {

		private final Preference pref;

		public GetHosakaStatus (final Context context, final Preference pref) {
			super(context);
			this.pref = pref;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onProgressUpdate (final String... values) {
			for (final String value : values) {
				this.pref.setSummary(value);
			}
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				if (new Prefs(getContext()).asConfig().firstAccountOfType(AccountProvider.HOSAKA) == null) {
					publishProgress("Add a Hosaka account to the accounts page to enable sync."); //ES
				}
				else {
					final String status = db.getValue(KvKeys.KEY_HOSAKA_STATUS);
					publishProgress(StringHelper.isEmpty(status) ? "Never run." : status); //ES
				}
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			if (result != null) DialogHelper.alert(getContext(), result);
		}

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

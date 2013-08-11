package com.vaguehope.onosendai.ui.pref;

import java.io.File;

import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.ConfigBuilder;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.LogcatHelper;

public class AdvancedPrefFragment extends PreferenceFragment {

	private static final LogWrapper LOG = new LogWrapper("ADVPREF");

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		addEntries();
	}

	protected Prefs getPrefs () {
		return this.prefs;
	}

	private void addEntries () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Import deck.conf");
		if (Config.isConfigFilePresent()) {
			pref.setSummary("Replace existing configuration");
			pref.setOnPreferenceClickListener(this.importClickListener);
		}
		else {
			pref.setSummary("File not found: " + Config.configFile().getAbsolutePath());
			pref.setEnabled(false);
		}
		getPreferenceScreen().addPreference(pref);

		final Preference dumpLogPref = new Preference(getActivity());
		dumpLogPref.setTitle("Dump log");
		dumpLogPref.setSummary("Write app log to /sdcard/onosendai-<time>.log");
		dumpLogPref.setOnPreferenceClickListener(this.dumpLogsClickListener);
		getPreferenceScreen().addPreference(dumpLogPref);
	}

	private final OnPreferenceClickListener importClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			askImport();
			return true;
		}
	};

	private final OnPreferenceClickListener dumpLogsClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			dumpLogs();
			return true;
		}
	};

	protected void askImport () {
		DialogHelper.askYesNo(getActivity(),
				"Are you sure you want to overwrite all configuration?",
				new Runnable() {
					@Override
					public void run () {
						doImport();
					}
				});
	}

	protected void doImport () {
		try {
			final Config config = Config.getConfig();
			new ConfigBuilder()
					.config(config)
					.writeOverMain(getActivity());
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to import configuration.", e);
			DialogHelper.alertAndClose(getActivity(), e);
		}
	}

	protected void dumpLogs () {
		try {
			final File file = new File(Environment.getExternalStorageDirectory(), "onosendai-" + System.currentTimeMillis() + ".log");
			LogcatHelper.dumpLog(file);
			DialogHelper.alert(getActivity(), "Log written to:\n" + file.getAbsolutePath());
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to dump log.", e);
			DialogHelper.alert(getActivity(), e);
		}
	}

}

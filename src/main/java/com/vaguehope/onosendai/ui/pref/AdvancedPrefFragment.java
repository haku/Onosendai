package com.vaguehope.onosendai.ui.pref;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.ConfigBuilder;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;

public class AdvancedPrefFragment extends PreferenceFragment {

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
	}

	private final OnPreferenceClickListener importClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			askImport();
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
			DialogHelper.alertAndClose(getActivity(), e);
		}
	}

}

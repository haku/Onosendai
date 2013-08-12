package com.vaguehope.onosendai.ui.pref;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.ConfigBuilder;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.IoHelper;
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
		addImportConfPref();
		addDumpLogPref();
		addDumpReadLaterPref();
	}

	private void addImportConfPref () {
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

	private void addDumpLogPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Dump log");
		pref.setSummary("Write app log to /sdcard/onosendai-<time>.log");
		pref.setOnPreferenceClickListener(this.dumpLogsClickListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addDumpReadLaterPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Dump read later");
		pref.setSummary("Write read later column to /sdcard/reading-<time>.txt");
		pref.setOnPreferenceClickListener(this.dumpReadLaterListener);
		getPreferenceScreen().addPreference(pref);
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

	private final OnPreferenceClickListener dumpReadLaterListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			dumpReadLater();
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

	protected void dumpReadLater () {
		try {
			final File file = new File(Environment.getExternalStorageDirectory(), "reading-" + System.currentTimeMillis() + ".txt");
			new DumpReadLater(getActivity(), this.prefs.asConfig(), file).execute();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to dump read later.", e);
			DialogHelper.alert(getActivity(), e);
		}
	}

	private static class DumpReadLater extends DbBindingAsyncTask<Void, Void, Exception> {

		private final Config conf;
		private final File file;

		public DumpReadLater (final Context context, final Config config, final File file) {
			super(context);
			this.conf = config;
			this.file = file;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			final Column col = this.conf.findInternalColumn(InternalColumnType.LATER);
			final List<Tweet> ts = db.getTweets(col.getId(), 500); // FIXME extract to constant.
			try {
				final PrintWriter w = new PrintWriter(this.file);
				try {
					for (final Tweet tw : ts) {
						final Tweet t = db.getTweetDetails(col.getId(), tw);
						w.append(t.getSid()).append(":").append(String.valueOf(t.getTime())).println();
						w.append(t.getUsername()).append(":").append(t.getFullname()).println();
						w.append(t.getBody()).println();
						if (t.getMetas() != null) for (final Meta m : t.getMetas()) {
							w.append(m.getType().toString()).append(":").append(m.getTitle()).append(":").append(m.getData()).println();
						}
						w.println();
					}
					return null;
				}
				finally {
					IoHelper.closeQuietly(w);
				}
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			if (result == null) {
				DialogHelper.alert(getContext(), "Read later column written to:\n" + this.file.getAbsolutePath());
			}
			else {
				LOG.e("Failed to dump read later.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

}

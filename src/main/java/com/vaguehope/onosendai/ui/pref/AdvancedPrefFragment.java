package com.vaguehope.onosendai.ui.pref;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
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
import com.vaguehope.onosendai.storage.DbInterface.Selection;
import com.vaguehope.onosendai.update.CleanupService;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.LogcatHelper;

public class AdvancedPrefFragment extends PreferenceFragment {

	public static final String KEY_THREAD_INSPECTOR = "pref_adv_thread_inspector";

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
		addThreadInspector();
		addImportConfPref();
		addExportConfPref();
		addDumpLogPref();
		addDumpReadLaterPref();
		addHousekeepPref();
	}

	private void addThreadInspector () {
		final CheckBoxPreference pref = new CheckBoxPreference(getActivity());
		pref.setKey(KEY_THREAD_INSPECTOR);
		pref.setTitle("Thread Inspector"); //ES
		pref.setSummary("For debugging issues with worker threads."); //ES
		getPreferenceScreen().addPreference(pref);
	}

	private void addImportConfPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Import deck.conf"); //ES
		if (Config.isConfigFilePresent()) {
			pref.setSummary("Replace existing configuration"); //ES
			pref.setOnPreferenceClickListener(this.importClickListener);
		}
		else {
			pref.setSummary("File not found: " + Config.configFile().getAbsolutePath());
			pref.setEnabled(false);
		}
		getPreferenceScreen().addPreference(pref);
	}

	private void addExportConfPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Export deck.conf (without secrets)"); //ES
		pref.setSummary("Write to /sdcard/deck.<time>.conf"); //ES
		pref.setOnPreferenceClickListener(this.exportClickListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addDumpLogPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Dump log"); //ES
		pref.setSummary("Write app log to /sdcard/onosendai-<time>.log"); //ES
		pref.setOnPreferenceClickListener(this.dumpLogsClickListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addDumpReadLaterPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Dump read later"); //ES
		pref.setSummary("Write read later column to /sdcard/reading-<time>.txt"); //ES
		pref.setOnPreferenceClickListener(this.dumpReadLaterListener);
		getPreferenceScreen().addPreference(pref);
	}

	private void addHousekeepPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Housekeep"); //ES
		pref.setSummary("Housekeep things now.  Underwhelming."); //ES
		pref.setOnPreferenceClickListener(this.housekeepListener);
		getPreferenceScreen().addPreference(pref);
	}

	private final OnPreferenceClickListener importClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			askImport();
			return true;
		}
	};

	private final OnPreferenceClickListener exportClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			doExport();
			return true;
		}
	};

	private final OnPreferenceClickListener dumpLogsClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			dumpLog();
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

	private final OnPreferenceClickListener housekeepListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			housekeep();
			return true;
		}
	};

	protected void askImport () {
		DialogHelper.askYesNo(getActivity(),
				"Are you sure you want to overwrite all configuration?", //ES
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

			final int accountsWithoutSecrets = config.countAccountsWithoutSecrets();
			if (accountsWithoutSecrets > 0) DialogHelper.alert(getActivity(), String.format(
					"%s accounts imported are missing secrets.  You must add them before things will work again.",
					accountsWithoutSecrets)); //ES
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to import configuration.", e);
			DialogHelper.alertAndClose(getActivity(), e);
		}
	}

	protected void doExport () {
		try {
			final File file = new File(Environment.getExternalStorageDirectory(), "deck." + System.currentTimeMillis() + ".conf");
			IoHelper.stringToFile(this.prefs.asConfig().toJson().toString(2), file);
			DialogHelper.alert(getActivity(), "Written to:\n" + file.getAbsolutePath()); //ES
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to export configuration.", e);
			DialogHelper.alertAndClose(getActivity(), e);
		}
	}

	protected void dumpLog () {
		try {
			final File file = new File(Environment.getExternalStorageDirectory(), "onosendai-" + System.currentTimeMillis() + ".log");
			new DumpLog(getActivity(), file).execute();
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

	protected void housekeep () {
		new Housekeep(getActivity()).execute();
	}

	private static class DumpLog extends AsyncTask<Void, Void, Exception> {

		private final Context context;
		private final File file;

		private ProgressDialog dialog;

		public DumpLog (final Context context, final File file) {
			this.context = context;
			this.file = file;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.context, "Log", "Saving...", true); //ES
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				LogcatHelper.dumpLog(this.file);
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.dialog.dismiss();
			if (result == null) {
				DialogHelper.alert(this.context, "Log written to:\n" + this.file.getAbsolutePath()); //ES
			}
			else {
				LOG.e("Failed to dump read later.", result);
				DialogHelper.alert(this.context, result);
			}
		}

	}

	private static class DumpReadLater extends DbBindingAsyncTask<Void, Void, Exception> {

		private final Config conf;
		private final File file;

		private ProgressDialog dialog;

		public DumpReadLater (final Context context, final Config config, final File file) {
			super(null, context);
			this.conf = config;
			this.file = file;
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(getContext(), "Read Later", "Saving...", true); //ES
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			final Column col = this.conf.findInternalColumn(InternalColumnType.LATER);
			final List<Tweet> ts = db.getTweets(col.getId(), 500, Selection.ALL); // FIXME extract to constant.
			try {
				final PrintWriter w = new PrintWriter(this.file, "UTF-8");
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
			this.dialog.dismiss();
			if (result == null) {
				DialogHelper.alert(getContext(), "Read later column written to:\n" + this.file.getAbsolutePath()); //ES
			}
			else {
				LOG.e("Failed to dump read later.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

	private static class Housekeep extends DbBindingAsyncTask<Void, Void, Exception> {

		private ProgressDialog dialog;

		public Housekeep (final Context context) {
			super(null, context);
		}

		@Override
		protected LogWrapper getLog () {
			return LOG;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(getContext(), "Housekeep", "Please wait...", true); //ES
		}

		@Override
		protected Exception doInBackgroundWithDb (final DbInterface db, final Void... params) {
			try {
				CleanupService.clean(getContext(), db);
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.dialog.dismiss();
			if (result == null) {
				DialogHelper.alert(getContext(), "Housekeep complete."); //ES
			}
			else {
				LOG.e("Failed to housekeep.", result);
				DialogHelper.alert(getContext(), result);
			}
		}

	}

}

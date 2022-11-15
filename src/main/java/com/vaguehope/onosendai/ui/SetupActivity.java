package com.vaguehope.onosendai.ui;

import java.io.File;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.ConfigBuilder;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.mastodon.MastodonColumnFactory;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleColumns;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleColumnsFetcher;
import com.vaguehope.onosendai.provider.twitter.TwitterColumnFactory;
import com.vaguehope.onosendai.ui.pref.MastodonOauthWizard;
import com.vaguehope.onosendai.ui.pref.TwitterOauthWizard;
import com.vaguehope.onosendai.ui.pref.MastodonOauthWizard.MastodonOauthComplete;
import com.vaguehope.onosendai.ui.pref.TwitterOauthWizard.TwitterOauthComplete;
import com.vaguehope.onosendai.ui.pref.TwitterOauthWizard.TwitterOauthHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.LogWrapper;

public class SetupActivity extends Activity {

	private static final LogWrapper LOG = new LogWrapper("SETUP");

	private Prefs prefs;

	private ArrayAdapter<SetupAction> actAdaptor;
	private Spinner spnAct;
	private TextView txtActDes;
	private Button btnContinue;

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);

		this.prefs = new Prefs(this);

		this.spnAct = (Spinner) findViewById(R.id.spnSetupAction);
		this.txtActDes = (TextView) findViewById(R.id.txtActionDescription);
		this.btnContinue = (Button) findViewById(R.id.btnContinue);

		this.actAdaptor = new ArrayAdapter<SetupAction>(this, R.layout.setupactionlistrow);
		this.actAdaptor.add(SetupActionType.MASTODON.toSetupAction(this));
		this.actAdaptor.add(SetupActionType.TWITTER.toSetupAction(this));
		this.actAdaptor.add(SetupActionType.SWIMPORT.toSetupAction(this));

		this.spnAct.setAdapter(this.actAdaptor);
		this.spnAct.setOnItemSelectedListener(this.actOnItemSelectedListener);

		if (Config.isConfigFilePresent()) {
			final SetupAction useConf = SetupActionType.USECONF.toSetupAction(this);
			this.actAdaptor.add(useConf);
			this.spnAct.setSelection(this.actAdaptor.getPosition(useConf));
		}
		else {
			this.actAdaptor.add(SetupActionType.WRITEEXAMPLECONF.toSetupAction(this));
		}

		this.btnContinue.setOnClickListener(this.btnContinueListener);
	}

	protected static LogWrapper getLog () {
		return LOG;
	}

	protected Prefs getPrefs () {
		if (this.prefs == null) throw new IllegalStateException("Prefs has not been initialised.");
		return this.prefs;
	}

	protected ArrayAdapter<SetupAction> getActAdaptor () {
		return this.actAdaptor;
	}

	protected void actionSelected (final SetupAction act) {
		this.txtActDes.setText(act.getDescription()
				.replace("${conf_path}", Config.configFile().getAbsolutePath()));
	}

	protected void runAction () {
		final SetupAction act = (SetupAction) this.spnAct.getSelectedItem();
		switch (act.getType()) {
			case TWITTER:
				doTwitter();
				break;
			case MASTODON:
				doMastodon();
				break;
			case SWIMPORT:
				doSwImport();
				break;
			case WRITEEXAMPLECONF:
				doWriteExampleConf();
				break;
			case USECONF:
				doUseConf();
				break;
			default:
				Toast.makeText(this, "TODO: " + act.getType(), Toast.LENGTH_SHORT).show();
		}
	}

	private final OnItemSelectedListener actOnItemSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			actionSelected(getActAdaptor().getItem(position));
		}

		@Override
		public void onNothingSelected (final AdapterView<?> arg0) {/**/}
	};

	private final OnClickListener btnContinueListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			runAction();
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TwitterOauthWizard twitterOauthWizard;

	private void initTwitterOauthWizard () {
		if (this.twitterOauthWizard != null) return;
		this.twitterOauthWizard = new TwitterOauthWizard(this, new TwitterOauthHelper() {
			@Override
			public void deligateStartActivityForResult (final Intent intent, final int requestCode) {
				startActivityForResult(intent, requestCode);
			}
		});
	}

	private void doTwitter () {
		initTwitterOauthWizard();
		this.twitterOauthWizard.start(new TwitterOauthComplete() {
			@Override
			public String getAccountId () {
				return SetupActivity.this.prefs.getNextAccountId();
			}

			@Override
			public void onAccount (final Account account, final String screenName) throws JSONException {
				SetupActivity.this.prefs.writeNewAccount(account);
				DialogHelper.alert(SetupActivity.this, "Twitter account added:\n" + screenName); //ES
				onTwitterAccountAdded(account);
			}
		});
	}

	private void doMastodon() {
		final MastodonOauthWizard wizard = new MastodonOauthWizard(this, new MastodonOauthComplete() {
			@Override
			public String getAccountId () {
				return SetupActivity.this.prefs.getNextAccountId();
			}

			@Override
			public void onAccount (final Account account, final String screenName) throws JSONException {
				SetupActivity.this.prefs.writeNewAccount(account);
				DialogHelper.alert(SetupActivity.this, "Mastodon account added:\n" + screenName); //ES
				onMastodonAccountAdded(account);
			}
		});
		wizard.start();
	}

	@Override
	public void onActivityResult (final int requestCode, final int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		this.twitterOauthWizard.onActivityResult(requestCode, resultCode, intent);
	}

	protected void onTwitterAccountAdded (final Account account) {
		DialogHelper.alertAndRun(this,
				"To get you started default Twitter columns will be created.  These can be customised later.", //ES
				new Runnable() {
					@Override
					public void run () {
						createTwitterColumnsAndFinish(account);
					}
				});
	}

	protected void createTwitterColumnsAndFinish (final Account account) {
		try {
			new ConfigBuilder()
					.account(account)
					.column(TwitterColumnFactory.homeTimeline(-1, account))
					.column(TwitterColumnFactory.mentions(-1, account))
					.readLater()
					.writeOverMain(this);
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			finish();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to setup Twitter account and columns.", e);
			DialogHelper.alertAndClose(this, e);
		}
	}

	protected void onMastodonAccountAdded (final Account account) {
		DialogHelper.alertAndRun(this,
				"To get you started default Mastodon columns will be created.  These can be customised later.", //ES
				new Runnable() {
					@Override
					public void run () {
						createMastodonColumnsAndFinish(account);
					}
				});
	}

	protected void createMastodonColumnsAndFinish (final Account account) {
		try {
			new ConfigBuilder()
					.account(account)
					.column(MastodonColumnFactory.homeTimeline(-1, account))
					.readLater()
					.writeOverMain(this);
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			finish();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to setup Mastodon account and columns.", e);
			DialogHelper.alertAndClose(this, e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void doWriteExampleConf () {
		try {
			final File f = Config.writeExampleConfig();
			DialogHelper.alertAndClose(this, "Example configuration file written to: " + f.getAbsolutePath()); //ES
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to write example confuration.", e);
			DialogHelper.alertAndClose(this, e);
		}
	}

	private void doUseConf () {
		try {
			final Config config = Config.getConfig();
			new ConfigBuilder()
					.config(config)
					.writeOverMain(this);
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			finish();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to import configuration.", e);
			DialogHelper.alertAndClose(this, e);
		}
	}

	private void doSwImport () {
		final ViewGroup llSetupType = (ViewGroup) findViewById(R.id.llSetupType);
		llSetupType.setVisibility(View.GONE);

		final ViewGroup llLogin = (ViewGroup) findViewById(R.id.llLogin);
		llLogin.setVisibility(View.VISIBLE);

		final EditText txtUsername = (EditText) findViewById(R.id.txtUsername);
		txtUsername.requestFocus();

		this.btnContinue.setOnClickListener(this.btnSwContinueListener);
	}

	private final OnClickListener btnSwContinueListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			doSwFetchConfig();
		}
	};

	protected void doSwFetchConfig () {
		final String username = ((EditText) findViewById(R.id.txtUsername)).getText().toString();
		final String password = ((EditText) findViewById(R.id.txtPassword)).getText().toString();
		final Account acc = new Account("sw0", username, AccountProvider.SUCCESSWHALE, null, null, username, password);
		new SuccessWhaleColumnsFetcher(this, acc, new Listener<SuccessWhaleColumns>() {
			@Override
			public void onAnswer (final SuccessWhaleColumns answer) {
				writeConfig(acc, answer);
			}
		}).execute();
	}

	protected void writeConfig (final Account account, final SuccessWhaleColumns columns) {
		try {
			new ConfigBuilder()
					.account(account)
					.columns(columns.getColumns())
					.readLater()
					.writeOverMain(this);
			startActivity(new Intent(getApplicationContext(), MainActivity.class));
			finish();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			getLog().e("Failed to write imported SuccessWhale configuration.", e);
			DialogHelper.alertAndClose(this, e);
		}
	}

	private enum SetupActionType {
		TWITTER("twitter"),
		MASTODON("mastodon"),
		SWIMPORT("swimport"),
		WRITEEXAMPLECONF("writeexampleconf"),
		USECONF("useconf");

		private final String id;

		private SetupActionType (final String id) {
			this.id = id;
		}

		public String getId () {
			return this.id;
		}

		public SetupAction toSetupAction (final Context context) {
			return new SetupAction(this, context);
		}

	}

	private static class SetupAction {

		private final SetupActionType type;
		private final String title;
		private final String description;

		public SetupAction (final SetupActionType type, final Context context) {
			this.type = type;
			final Resources res = context.getResources();
			this.title = res.getString(res.getIdentifier("setup_act_" + type.getId(), "string", context.getPackageName()));
			this.description = res.getString(res.getIdentifier("setup_des_" + type.getId(), "string", context.getPackageName()));
		}

		public SetupActionType getType () {
			return this.type;
		}

		public String getDescription () {
			return this.description;
		}

		@Override
		public String toString () {
			return this.title;
		}

	}

}

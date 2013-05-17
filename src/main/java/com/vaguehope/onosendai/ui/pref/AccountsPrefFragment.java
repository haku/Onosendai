package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import org.json.JSONException;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.twitter.TwitterOauth;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.Result;

public class AccountsPrefFragment extends PreferenceFragment {

	// TODO merge these lists.
	protected static final AccountProvider[] NEW_ACCOUNT_PROVIDERS = new AccountProvider[] {
			AccountProvider.TWITTER,
			AccountProvider.SUCCESSWHALE
	};
	private static final String[] NEW_ACCOUNT_LABELS = new String[] {
			AccountProvider.TWITTER.toHumanString(),
			AccountProvider.SUCCESSWHALE.toHumanString()
	};

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshAccountsList();
	}

	protected Prefs getPrefs () {
		return this.prefs;
	}

	protected void refreshAccountsList () {
		getPreferenceScreen().removeAll();

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Account");
		pref.setSummary("Add a new Twitter or SuccessWhale account");
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		getPreferenceScreen().addPreference(pref);

		final List<String> accountIds = getPrefs().readAccountIds();
		for (final String accountId : accountIds) {
			try {
				final Account account = getPrefs().readAccount(accountId);
				getPreferenceScreen().addPreference(new AccountDialogPreference(getActivity(), account, this));
			}
			catch (final JSONException e) {
				DialogHelper.alert(getActivity(), "Failed to read account: ", e);
			}
		}
	}

	protected void promptNewAccountType () {
		final AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
		bld.setTitle("Account Type");
		bld.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(NEW_ACCOUNT_LABELS, new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int item) {
				dialog.dismiss();
				promptAddAccount(NEW_ACCOUNT_PROVIDERS[item]);
			}
		});
		bld.show();
	}

	protected void promptAddAccount (final AccountProvider accountProvider) {
		switch (accountProvider) {
			case TWITTER:
				promptAddTwitterAccount();
				break;
			case SUCCESSWHALE:
				promptAddSuccessWhaleAccount();
				break;
			default:
				DialogHelper.alert(getActivity(), "Do not know how to add account of type: " + accountProvider);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Configuration twitterConfiguration;
	private Twitter twitter;
	private RequestToken requestToken;

	private Configuration getTwitterConfiguration () {
		if (this.twitterConfiguration == null) {
			final ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(TwitterOauth.getConsumerKey());
			builder.setOAuthConsumerSecret(TwitterOauth.getConsumerSecret());
			this.twitterConfiguration = builder.build();
		}
		return this.twitterConfiguration;
	}

	protected Twitter getTwitter () {
		if (this.twitter == null) {
			final TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
			this.twitter = factory.getInstance();
		}
		return this.twitter;
	}

	public void stashRequestToken (final RequestToken token) {
		this.requestToken = token;
	}

	public RequestToken unstashRequestToken () {
		final RequestToken token = this.requestToken;
		this.requestToken = null;
		return token;
	}

	private void promptAddTwitterAccount () {
		new TwitterOauthInitTask(this).execute();
	}

	private static class TwitterOauthInitTask extends AsyncTask<Void, Void, Result<RequestToken>> {

		private final AccountsPrefFragment host;
		private ProgressDialog dialog;

		public TwitterOauthInitTask (final AccountsPrefFragment host) {
			this.host = host;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getActivity(), "Onosendai", "Starting Oauth...", true);
		}

		@Override
		protected Result<RequestToken> doInBackground (final Void... params) {
			try {
				return new Result<RequestToken>(this.host.getTwitter().getOAuthRequestToken(TwitterOauth.CALLBACK_URL));
			}
			catch (final Exception e) { // NOSONAR report all errors to user.
				return new Result<RequestToken>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<RequestToken> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				final Intent intent = new Intent(this.host.getActivity(), TwitterLoginActivity.class);
				final RequestToken requtestToken = result.getData();
				this.host.stashRequestToken(requtestToken);
				intent.putExtra(TwitterOauth.IEXTRA_AUTH_URL, requtestToken.getAuthorizationURL());
				this.host.startActivityForResult(intent, 0);
			}
			else {
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

	@Override
	public void onActivityResult (final int requestCode, final int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == 0) {
			if (resultCode == Activity.RESULT_OK) {
				final String oauthVerifier = intent.getExtras().getString(TwitterOauth.IEXTRA_OAUTH_VERIFIER);
				new TwitterOauthPostTask(this, oauthVerifier).execute();
			}
			else if (resultCode == Activity.RESULT_CANCELED) {
				DialogHelper.alert(getActivity(), "Twitter auth canceled.");
			}
		}
	}

	private static class TwitterOauthPostTask extends AsyncTask<Void, Void, Result<AccessToken>> {

		private final AccountsPrefFragment host;
		private final String oauthVerifier;
		private ProgressDialog dialog;

		public TwitterOauthPostTask (final AccountsPrefFragment host, final String oauthVerifier) {
			this.host = host;
			this.oauthVerifier = oauthVerifier;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getActivity(), "Onosendai", "Completing Oauth...", true);
		}

		@Override
		protected Result<AccessToken> doInBackground (final Void... params) {
			try {
				final RequestToken token = this.host.unstashRequestToken();
				return new Result<AccessToken>(this.host.getTwitter().getOAuthAccessToken(token, this.oauthVerifier));
			}
			catch (final Exception e) { // NOSONAR report all errors to user.
				return new Result<AccessToken>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<AccessToken> result) {
			this.dialog.dismiss();
			if (result.isSuccess()) {
				this.host.onGotTwitterAccessToken(result.getData());
			}
			else {
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

	protected void onGotTwitterAccessToken (final AccessToken accessToken) {
		try {
			final String id = getPrefs().getNextAccountId();
			final Account account = new Account(id, AccountProvider.TWITTER,
					getTwitterConfiguration().getOAuthConsumerKey(), getTwitterConfiguration().getOAuthConsumerSecret(),
					accessToken.getToken(), accessToken.getTokenSecret());
			this.prefs.writeNewAccount(account);
			DialogHelper.alert(getActivity(), "Twitter account added:\n" + accessToken.getScreenName());
			refreshAccountsList();
		}
		catch (final Exception e) { // NOSONAR want to show any errors to the user.
			DialogHelper.alert(getActivity(), e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void promptAddSuccessWhaleAccount () {
		final String id = getPrefs().getNextAccountId();
		final AccountDialog dlg = new AccountDialog(getActivity(), id, AccountProvider.SUCCESSWHALE);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Account (" + id + ")");
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					getPrefs().writeNewAccount(dlg.getValue());
				}
				catch (final JSONException e) {
					DialogHelper.alert(getActivity(), "Failed to write new account: ", e);
				}
				refreshAccountsList();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		dlgBuilder.create().show();
	}

	private static class AddAcountClickListener implements OnPreferenceClickListener {

		private final AccountsPrefFragment accountsPrefFragment;

		public AddAcountClickListener (final AccountsPrefFragment accountsPrefFragment) {
			this.accountsPrefFragment = accountsPrefFragment;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.accountsPrefFragment.promptNewAccountType();
			return true;
		}
	}

}

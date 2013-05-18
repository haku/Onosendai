package com.vaguehope.onosendai.ui.pref;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.twitter.TwitterOauth;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class TwitterOauthWizard {

	private static final int INTERNAL_REQUEST_CODE = 100; // Just a constant number.
	private static final LogWrapper LOG = new LogWrapper("TOW");

	private final Activity activity;
	private final Prefs prefs;
	private final TwitterOauthCallback callback;

	private Configuration twitterConfiguration;
	private Twitter twitter;
	private RequestToken requestToken;

	public interface TwitterOauthCallback {
		void onAccountAdded (Account account);
		void deligateStartActivityForResult (Intent intent, int requestCode);
	}

	public TwitterOauthWizard (final Activity activity, final Prefs prefs, final TwitterOauthCallback callback) {
		this.activity = activity;
		this.prefs = prefs;
		this.callback = callback;
	}

	public void start () {
		new TwitterOauthInitTask(this).execute();
	}

	protected static LogWrapper getLog () {
		return LOG;
	}

	protected Activity getActivity () {
		return this.activity;
	}

	protected TwitterOauthCallback getCallback () {
		return this.callback;
	}

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

	protected void stashRequestToken (final RequestToken token) {
		if (this.requestToken != null) throw new IllegalStateException("Request token alrady stashed.");
		if (token == null) throw new IllegalStateException("Can not stash a null token.");
		this.requestToken = token;
	}

	protected RequestToken unstashRequestToken () {
		if (this.requestToken == null) throw new IllegalStateException("Request token has not been stashed.");
		final RequestToken token = this.requestToken;
		this.requestToken = null;
		return token;
	}

	private static class TwitterOauthInitTask extends AsyncTask<Void, Void, Result<RequestToken>> {

		private final TwitterOauthWizard host;
		private ProgressDialog dialog;

		public TwitterOauthInitTask (final TwitterOauthWizard host) {
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
				this.host.getCallback().deligateStartActivityForResult(intent, INTERNAL_REQUEST_CODE);
			}
			else {
				getLog().e("Failed to init OAuth.", result.getE());
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

	public void onActivityResult (final int requestCode, final int resultCode, final Intent intent) {
		LOG.d("onActivityResult(%d, %d, %s)", requestCode, resultCode, intent);
		if (requestCode == INTERNAL_REQUEST_CODE) {
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

		private final TwitterOauthWizard host;
		private final String oauthVerifier;
		private ProgressDialog dialog;

		public TwitterOauthPostTask (final TwitterOauthWizard host, final String oauthVerifier) {
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
				getLog().e("Failed to complete OAuth.", result.getE());
				DialogHelper.alert(this.host.getActivity(), result.getE());
			}
		}

	}

	protected void onGotTwitterAccessToken (final AccessToken accessToken) {
		try {
			LOG.i("Account authorised %s.", accessToken.getScreenName());
			final String id = this.prefs.getNextAccountId();
			final Account account = new Account(id, AccountProvider.TWITTER,
					getTwitterConfiguration().getOAuthConsumerKey(), getTwitterConfiguration().getOAuthConsumerSecret(),
					accessToken.getToken(), accessToken.getTokenSecret());
			this.prefs.writeNewAccount(account);
			DialogHelper.alert(getActivity(), "Twitter account added:\n" + accessToken.getScreenName());
			this.callback.onAccountAdded(account);
		}
		catch (final Exception e) { // NOSONAR want to show any errors to the user.
			LOG.e("Failed to write new Twitter account.", e);
			DialogHelper.alert(getActivity(), e);
		}
	}

}

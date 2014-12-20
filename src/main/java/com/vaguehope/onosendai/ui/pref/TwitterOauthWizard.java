package com.vaguehope.onosendai.ui.pref;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterOauth;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;

public class TwitterOauthWizard {

	private static final LogWrapper LOG = new LogWrapper("TOW");

	private final Context context;
	private final TwitterOauthHelper helperCallback;

	private Configuration twitterConfiguration;
	private Twitter twitter;
	private RequestToken requestToken;

	public interface TwitterOauthHelper {
		void deligateStartActivityForResult (Intent intent, int requestCode);
	}

	public interface TwitterOauthComplete {
		String getAccountId ();
		void onAccount (Account account, String screenName) throws JSONException;
	}

	private final AtomicInteger requestCode = new AtomicInteger(100);
	private final Map<Integer, TwitterOauthComplete> completeCallbacks = new ConcurrentHashMap<Integer, TwitterOauthWizard.TwitterOauthComplete>();

	public TwitterOauthWizard (final Context context, final TwitterOauthHelper helperCallback) {
		this.context = context;
		this.helperCallback = helperCallback;
	}

	public void start (final TwitterOauthComplete completeCallback) {
		final int requestCode = Integer.valueOf(this.requestCode.incrementAndGet());
		this.completeCallbacks.put(requestCode, completeCallback);
		new TwitterOauthInitTask(this, requestCode).execute();
	}

	protected static LogWrapper getLog () {
		return LOG;
	}

	protected Context getContext () {
		return this.context;
	}

	public TwitterOauthHelper getHelperCallback () {
		return this.helperCallback;
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
		private final Integer requestCode;
		private ProgressDialog dialog;

		public TwitterOauthInitTask (final TwitterOauthWizard host, final Integer requestCode) {
			this.host = host;
			this.requestCode = requestCode;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getContext(), "Onosendai", "Starting Oauth...", true);
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
				final Intent intent = new Intent(this.host.getContext(), TwitterLoginActivity.class);
				final RequestToken requtestToken = result.getData();
				this.host.stashRequestToken(requtestToken);
				intent.putExtra(TwitterOauth.IEXTRA_AUTH_URL, requtestToken.getAuthorizationURL());
				this.host.getHelperCallback().deligateStartActivityForResult(intent, this.requestCode);
			}
			else {
				getLog().e("Failed to init OAuth.", result.getE());
				DialogHelper.alert(this.host.getContext(), result.getE());
			}
		}

	}

	public void onActivityResult (final int requestCode, final int resultCode, final Intent intent) {
		LOG.d("onActivityResult(%d, %d, %s)", requestCode, resultCode, intent);
		final TwitterOauthComplete completeCallback = this.completeCallbacks.get(Integer.valueOf(requestCode));
		if (completeCallback != null) {
			if (resultCode == Activity.RESULT_OK) {
				final String oauthVerifier = intent.getExtras().getString(TwitterOauth.IEXTRA_OAUTH_VERIFIER);
				new TwitterOauthPostTask(this, oauthVerifier, completeCallback).execute();
			}
			else if (resultCode == Activity.RESULT_CANCELED) {
				DialogHelper.alert(getContext(), "Twitter auth canceled.");
			}
		}
	}

	private static class TwitterOauthPostTask extends AsyncTask<Void, Void, Result<AccessToken>> {

		private final TwitterOauthWizard host;
		private final String oauthVerifier;
		private final TwitterOauthComplete completeCallback;
		private ProgressDialog dialog;

		public TwitterOauthPostTask (final TwitterOauthWizard host, final String oauthVerifier, final TwitterOauthComplete completeCallback) {
			this.host = host;
			this.oauthVerifier = oauthVerifier;
			this.completeCallback = completeCallback;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getContext(), "Onosendai", "Completing Oauth...", true);
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
				this.host.onGotTwitterAccessToken(result.getData(), this.completeCallback);
			}
			else {
				getLog().e("Failed to complete OAuth.", result.getE());
				DialogHelper.alert(this.host.getContext(), result.getE());
			}
		}

	}

	protected void onGotTwitterAccessToken (final AccessToken accessToken, final TwitterOauthComplete completeCallback) {
		try {
			LOG.i("Account authorised %s.", accessToken.getScreenName());
			final Account account = new Account(completeCallback.getAccountId(), accessToken.getScreenName(), AccountProvider.TWITTER,
					getTwitterConfiguration().getOAuthConsumerKey(), getTwitterConfiguration().getOAuthConsumerSecret(),
					accessToken.getToken(), accessToken.getTokenSecret());
			completeCallback.onAccount(account, accessToken.getScreenName());
		}
		catch (final Exception e) { // NOSONAR want to show any errors to the user.
			LOG.e("Failed to write new Twitter account.", e);
			DialogHelper.alert(getContext(), e);
		}
	}

}

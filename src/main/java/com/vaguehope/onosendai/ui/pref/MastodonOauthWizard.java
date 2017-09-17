package com.vaguehope.onosendai.ui.pref;

import org.json.JSONException;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.provider.mastodon.MastodonAuth;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.LogWrapper;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

public class MastodonOauthWizard {

	public interface MastodonOauthComplete {
		String getAccountId ();

		void onAccount (Account account, String screenName) throws JSONException;
	}

	private static final LogWrapper LOG = new LogWrapper("MOW");

	private final Context context;
	private final MastodonOauthComplete completeCallback;

	public MastodonOauthWizard (final Context context, final MastodonOauthComplete completeCallback) {
		this.context = context;
		this.completeCallback = completeCallback;
	}

	public void start () {
		DialogHelper.askString(getContext(),
				"instanceName: (e.g. \"mastodon.social\")", //ES
				null, false, false, new Listener<String>() {
			@Override
			public void onAnswer (final String instanceName) {
				new MastodonOauthInitTask(MastodonOauthWizard.this, instanceName).execute();
			}
		});
	}

	protected static LogWrapper getLog () {
		return LOG;
	}

	protected Context getContext () {
		return this.context;
	}

	public MastodonOauthComplete getCompleteCallback () {
		return this.completeCallback;
	}

	private static class MastodonOauthInitTask extends AsyncTask<Void, Void, Exception> {

		private final MastodonOauthWizard host;
		private final MastodonAuth mastodonAuth;

		private ProgressDialog dialog;

		public MastodonOauthInitTask (final MastodonOauthWizard host, final String instanceName) {
			this.host = host;
			this.mastodonAuth = new MastodonAuth(instanceName);
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getContext(), "Onosendai", "Starting Oauth...", true); //ES
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				this.mastodonAuth.registerApp();
				this.mastodonAuth.fetchOAuthUrl();
				return null;
			}
			catch (final Exception e) { // NOSONAR report all errors to user.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception exception) {
			this.dialog.dismiss();
			if (exception == null) {
				final String oAuthUrl = this.mastodonAuth.getOauthUrl();

				DialogHelper.alertAndRun(this.host.getContext(), "Remember to copy the token!", new Runnable() {
					@Override
					public void run () {
						final Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(oAuthUrl));
						MastodonOauthInitTask.this.host.getContext().startActivity(i);

						DialogHelper.askString(MastodonOauthInitTask.this.host.getContext(), "Auth code:", null, false, false, new Listener<String>() {
							@Override
							public void onAnswer (final String authCode) {
								LOG.i("Got authCode=%s.", authCode);
								new MastodonOauthPostTask(MastodonOauthInitTask.this.host, MastodonOauthInitTask.this.mastodonAuth, authCode).execute();
							}
						});
					}
				});
			}
			else {
				getLog().e("Failed to init OAuth.", exception);
				DialogHelper.alert(this.host.getContext(), exception);
			}
		}

	}

	private static class MastodonOauthPostTask extends AsyncTask<Void, Void, Exception> {

		private final MastodonOauthWizard host;
		private final MastodonAuth mastodonAuth;
		private final String authCode;

		private ProgressDialog dialog;

		public MastodonOauthPostTask (final MastodonOauthWizard host, final MastodonAuth mastodonAuth, final String authCode) {
			this.host = host;
			this.mastodonAuth = mastodonAuth;
			this.authCode = authCode;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.host.getContext(), "Onosendai", "Completing Oauth...", true); //ES
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				this.mastodonAuth.fetchAccessToken(this.authCode);
				this.mastodonAuth.fetchMyAccount();
				return null;
			}
			catch (final Exception e) { // NOSONAR report all errors to user.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception exception) {
			this.dialog.dismiss();
			if (exception == null) {
				gotAccessToken();
			}
			else {
				getLog().e("Failed to complete OAuth.", exception);
				DialogHelper.alert(this.host.getContext(), exception);
			}
		}

		private void gotAccessToken () {
			try {
				final String osId = this.host.getCompleteCallback().getAccountId();

				final String accountTitle = this.mastodonAuth.getMyAccount().getAcct();
				final String accessToken = this.mastodonAuth.getAccessToken().getAccessToken();
				LOG.i("%s accessToken=%s", accountTitle, accessToken); // TODO stop logging this.

				final Account account = new Account(
						osId,
						accountTitle,
						AccountProvider.MASTODON,
						this.mastodonAuth.getInstanceName(),
						null,
						accessToken,
						null);
				this.host.getCompleteCallback().onAccount(account, accountTitle);
			}
			catch (final Exception e) { // NOSONAR want to show any errors to the user.
				LOG.e("Failed to write new Mastodon account.", e);
				DialogHelper.alert(this.host.getContext(), e);
			}
		}

	}

}

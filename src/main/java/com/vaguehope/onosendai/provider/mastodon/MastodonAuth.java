package com.vaguehope.onosendai.provider.mastodon;

import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.MastodonClient.Builder;
import com.sys1yagi.mastodon4j.api.Scope;
import com.sys1yagi.mastodon4j.api.entity.Account;
import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken;
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Accounts;
import com.sys1yagi.mastodon4j.api.method.Apps;
import com.vaguehope.onosendai.util.StringHelper;

import okhttp3.OkHttpClient;

public class MastodonAuth {

	private static final String APP_NAME = "Onosendai";
	private static final String APP_URL = "https://github.com/haku/Onosendai";

	private final MastodonClient client;

	private volatile AppRegistration registration;
	private volatile String oauthUrl;
	private volatile AccessToken accessToken;
	private volatile Account myAccount;

	public MastodonAuth (final String instanceName) {
		if (StringHelper.isEmpty(instanceName)) throw new IllegalArgumentException("Missing instanceName.");
		this.client = makeMastodonClientBuilder(instanceName).build();
	}

	public String getInstanceName () {
		return this.client.getInstanceName();
	}

	public String getOauthUrl () {
		if (this.oauthUrl == null) throw new IllegalStateException("oauthUrl not set.");
		return this.oauthUrl;
	}

	public AccessToken getAccessToken () {
		if (this.accessToken == null) throw new IllegalStateException("accessToken not set.");
		return this.accessToken;
	}

	public Account getMyAccount () {
		if (this.myAccount == null) throw new IllegalStateException("myAccount not set.");
		return this.myAccount;
	}

	public synchronized void registerApp () throws Mastodon4jRequestException {
		if (this.registration != null) return;

		final Apps apps = new Apps(this.client);
		this.registration = apps.createApp(
				APP_NAME,
				"urn:ietf:wg:oauth:2.0:oob",
				new Scope(Scope.Name.ALL),
				APP_URL).execute();
	}

	public synchronized void fetchOAuthUrl () throws Mastodon4jRequestException {
		if (this.registration == null) throw new IllegalStateException("AppRegistration missing.");

		final Apps apps = new Apps(this.client);
		this.oauthUrl = apps.getOAuthUrl(
				this.registration.getClientId(),
				new Scope(Scope.Name.ALL),
				"urn:ietf:wg:oauth:2.0:oob");
	}

	public synchronized void fetchAccessToken (final String authCode) throws Mastodon4jRequestException {
		if (this.registration == null) throw new IllegalStateException("AppRegistration missing.");

		final Apps apps = new Apps(this.client);
		this.accessToken = apps.getAccessToken(
				this.registration.getClientId(),
				this.registration.getClientSecret(),
				"urn:ietf:wg:oauth:2.0:oob",
				authCode,
				"authorization_code").execute();
	}

	public synchronized void fetchMyAccount () throws Mastodon4jRequestException {
		final MastodonClient authClient = makeMastodonClientBuilder(getInstanceName())
				.accessToken(getAccessToken().getAccessToken())
				.build();
		final Accounts accounts = new Accounts(authClient);
		this.myAccount = accounts.getVerifyCredentials().execute();
	}

	/**
	 * @param instanceName e.g. "mastodon.social"
	 */
	static Builder makeMastodonClientBuilder (final String instanceName) {
		return new MastodonClient.Builder(
				instanceName,
        new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        ,
				new Gson());
	}

}

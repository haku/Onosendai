package com.vaguehope.onosendai.config;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.util.EqualHelper;

public class Account {

	private static final String KEY_ID = "id";
	private static final String KEY_TITLE = "title";
	private static final String KEY_PROVIDER = "provider";

	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";

	private static final String KEY_CONSUMER_KEY = "consumerKey";
	private static final String KEY_CONSUMER_SECRET = "consumerSecret";
	private static final String KEY_ACCESS_TOKEN = "accessToken";
	private static final String KEY_ACCESS_SECRET = "accessSecret";

	private final String id;
	private final String title;
	private final AccountProvider provider;
	private final String consumerKey;
	private final String consumerSecret;
	private final String accessToken;
	private final String accessSecret;

	public Account (final String id, final String title, final AccountProvider provider, final String consumerKey, final String consumerSecret, final String accessToken, final String accessSecret) {
		this.id = id;
		this.title = title;
		this.provider = provider;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessSecret = accessSecret;
	}

	public String humanTitle () {
		if (this.title != null && !this.title.isEmpty()) {
			return this.title;
		}
		return String.format("%s (%s)", this.provider.toHumanString(), this.id);
	}

	public String humanDescription () {
		return String.format("%s account", this.provider.toHumanString());
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Account{").append(this.id)
				.append(",").append(this.provider)
				.append("}");
		return s.toString();
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
		result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
		result = prime * result + ((this.provider == null) ? 0 : this.provider.hashCode());
		result = prime * result + ((this.consumerKey == null) ? 0 : this.consumerKey.hashCode());
		result = prime * result + ((this.consumerSecret == null) ? 0 : this.consumerSecret.hashCode());
		result = prime * result + ((this.accessToken == null) ? 0 : this.accessToken.hashCode());
		result = prime * result + ((this.accessSecret == null) ? 0 : this.accessSecret.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Account)) return false;
		Account that = (Account) o;
		return EqualHelper.equal(this.id, that.id) &&
				EqualHelper.equal(this.title, that.title) &&
				EqualHelper.equal(this.provider, that.provider) &&
				EqualHelper.equal(this.consumerKey, that.consumerKey) &&
				EqualHelper.equal(this.consumerSecret, that.consumerSecret) &&
				EqualHelper.equal(this.accessToken, that.accessToken) &&
				EqualHelper.equal(this.accessSecret, that.accessSecret);
	}

	public String getId () {
		return this.id;
	}

	public String getTitle () {
		return this.title;
	}

	public AccountProvider getProvider () {
		return this.provider;
	}

	public String getConsumerKey () {
		return this.consumerKey;
	}

	public String getConsumerSecret () {
		return this.consumerSecret;
	}

	public String getAccessToken () {
		return this.accessToken;
	}

	public String getAccessSecret () {
		return this.accessSecret;
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(KEY_ID, this.id);
		json.put(KEY_TITLE, this.title);
		json.put(KEY_PROVIDER, String.valueOf(this.provider));
		switch (this.provider) {
			case TWITTER:
				json.put(KEY_CONSUMER_KEY, this.consumerKey);
				json.put(KEY_CONSUMER_SECRET, this.consumerSecret);
				json.put(KEY_ACCESS_TOKEN, this.accessToken);
				json.put(KEY_ACCESS_SECRET, this.accessSecret);
				break;
			case SUCCESSWHALE:
				json.put(KEY_USERNAME, this.accessToken);
				json.put(KEY_PASSWORD, this.accessSecret);
				break;
			case BUFFER:
				json.put(KEY_ACCESS_TOKEN, this.accessToken);
				break;
			default:
				throw new IllegalArgumentException("Unsupported account provilder: " + this.provider);
		}
		return json;
	}

	public static Account parseJson (final String json) throws JSONException {
		if (json == null) return null;
		return parseJson((JSONObject) new JSONTokener(json).nextValue());
	}

	public static Account parseJson (final JSONObject json) throws JSONException {
		if (json == null) return null;
		final String id = json.getString(KEY_ID);
		final AccountProvider provider = AccountProvider.parse(json.getString(KEY_PROVIDER));
		Account account;
		switch (provider) {
			case TWITTER:
				account = parseTwitterAccount(json, id);
				break;
			case SUCCESSWHALE:
				account = parseSuccessWhaleAccount(json, id);
				break;
			case BUFFER:
				account = parseBufferAccount(json, id);
				break;
			default:
				throw new IllegalArgumentException("Unknown provider: " + provider);
		}
		return account;
	}

	private static Account parseTwitterAccount (final JSONObject accountJson, final String id) throws JSONException {
		final String title = accountJson.optString(KEY_TITLE, null);
		final String consumerKey = accountJson.getString(KEY_CONSUMER_KEY);
		final String consumerSecret = accountJson.getString(KEY_CONSUMER_SECRET);
		final String accessToken = accountJson.getString(KEY_ACCESS_TOKEN);
		final String accessSecret = accountJson.getString(KEY_ACCESS_SECRET);
		return new Account(id, title, AccountProvider.TWITTER, consumerKey, consumerSecret, accessToken, accessSecret);
	}

	private static Account parseSuccessWhaleAccount (final JSONObject accountJson, final String id) throws JSONException {
		final String accessToken = accountJson.getString(KEY_USERNAME);
		final String accessSecret = accountJson.getString(KEY_PASSWORD);
		final String title = accountJson.optString(KEY_TITLE, accessToken);
		return new Account(id, title, AccountProvider.SUCCESSWHALE, null, null, accessToken, accessSecret);
	}

	private static Account parseBufferAccount (final JSONObject accountJson, final String id) throws JSONException {
		final String title = accountJson.optString(KEY_TITLE, null);
		final String accessToken = accountJson.getString(KEY_ACCESS_TOKEN);
		return new Account(id, title, AccountProvider.BUFFER, null, null, accessToken, null);
	}

}

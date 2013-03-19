package com.vaguehope.onosendai.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.Environment;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.util.FileHelper;

public class Config {

	private final Map<String, Account> accounts;
	private final Map<Integer, Column> feeds;

	public Config () throws IOException, JSONException {
		File f = new File(Environment.getExternalStorageDirectory().getPath(), C.CONFIG_FILE_NAME);

		if (!f.exists()) {
			FileHelper.resourceToFile("/deck.conf", f);
		}

		String s = FileHelper.fileToString(f);
		JSONObject o = (JSONObject) new JSONTokener(s).nextValue();

		JSONArray accountsJson = o.getJSONArray("accounts");
		this.accounts = parseAccounts(accountsJson);

		JSONArray feedsJson = o.getJSONArray("feeds");
		this.feeds = parseFeeds(feedsJson);
	}

	public Map<String, Account> getAccounts () {
		return this.accounts;
	}

	public Account getAccount (final String accountId) {
		return this.accounts.get(accountId);
	}

	public Map<Integer, Column> getColumns () {
		return this.feeds;
	}

	public Column getColumn (final int id) {
		return this.feeds.get(Integer.valueOf(id));
	}

	public Column findInternalColumn (final InternalColumnType res) {
		for (Column col : getColumns().values()) {
			if (res.matchesColumn(col)) return col;
		}
		return null;
	}

	private static Map<String, Account> parseAccounts (final JSONArray accountsJson) throws JSONException {
		Map<String, Account> ret = new HashMap<String, Account>();
		for (int i = 0; i < accountsJson.length(); i++) {
			JSONObject accountJson = accountsJson.getJSONObject(i);
			String id = accountJson.getString("id");
			AccountProvider provider = AccountProvider.parse(accountJson.getString("provider"));
			Account account;
			switch (provider) {
				case TWITTER:
					account = parseTwitterAccount(accountJson, id);
					break;
				case SUCCESSWHALE:
					account = parseSuccessWhaleAccount(accountJson, id);
					break;
				default:
					throw new IllegalArgumentException("Unknown provider: " + provider);
			}

			ret.put(id, account);
		}
		return Collections.unmodifiableMap(ret);
	}

	private static Account parseTwitterAccount (final JSONObject accountJson, final String id) throws JSONException {
		Account account;
		String consumerKey = accountJson.getString("consumerKey");
		String consumerSecret = accountJson.getString("consumerSecret");
		String accessToken = accountJson.getString("accessToken");
		String accessSecret = accountJson.getString("accessSecret");
		account = new Account(id, AccountProvider.TWITTER, consumerKey, consumerSecret, accessToken, accessSecret);
		return account;
	}

	private static Account parseSuccessWhaleAccount (final JSONObject accountJson, final String id) throws JSONException {
		Account account;
		String accessToken = accountJson.getString("username");
		String accessSecret = accountJson.getString("password");
		account = new Account(id, AccountProvider.SUCCESSWHALE, null, null, accessToken, accessSecret);
		return account;
	}

	/**
	 * // TODO allow multiple feeds per column.
	 */
	private static Map<Integer, Column> parseFeeds (final JSONArray columnsJson) throws JSONException {
		Map<Integer, Column> ret = new HashMap<Integer, Column>();
		for (int i = 0; i < columnsJson.length(); i++) {
			JSONObject colJson = columnsJson.getJSONObject(i);
			int index = colJson.getInt("id");
			String title = colJson.getString("title");
			String account = colJson.has("account") ? colJson.getString("account") : null;
			String resource = colJson.getString("resource");
			String refresh = colJson.has("refresh") ? colJson.getString("refresh") : null;
			ret.put(Integer.valueOf(i), new Column(index, title, account, resource, refresh));
		}
		return Collections.unmodifiableMap(ret);
	}

}

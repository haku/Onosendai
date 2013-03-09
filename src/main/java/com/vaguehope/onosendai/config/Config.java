package com.vaguehope.onosendai.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
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
	private final Map<Integer, Column> columns;

	public Config () throws IOException, JSONException {
		File f = new File(Environment.getExternalStorageDirectory().getPath(), C.CONFIG_FILE_NAME);

		if (!f.exists()) {
			FileHelper.resourceToFile("/deck.conf", f);
		}

		String s = FileHelper.fileToString(f);
		JSONObject o = (JSONObject) new JSONTokener(s).nextValue();

		JSONArray accountsJson = o.getJSONArray("accounts");
		this.accounts = parseAccounts(accountsJson);

		JSONArray columnsJson = o.getJSONArray("columns");
		this.columns = parseColumns(columnsJson);
	}

	public Map<String, Account> getAccounts () {
		return this.accounts;
	}

	public Map<Integer, Column> getColumns () {
		return this.columns;
	}

	public Column getColumn (final int index) {
		return this.columns.get(Integer.valueOf(index));
	}

	private static Map<String, Account> parseAccounts (final JSONArray accountsJson) throws JSONException {
		Map<String, Account> ret = new HashMap<String, Account>();
		for (int i = 0; i < accountsJson.length(); i++) {
			JSONObject accountJson = accountsJson.getJSONObject(i);
			String id = accountJson.getString("id");
			AccountProvider provider = AccountProvider.parse(accountJson.getString("provider"));
			String consumerKey = accountJson.getString("consumerKey");
			String consumerSecret = accountJson.getString("consumerSecret");
			String accessToken = accountJson.getString("accessToken");
			String accessSecret = accountJson.getString("accessSecret");
			ret.put(id, new Account(id, provider, consumerKey, consumerSecret, accessToken, accessSecret));
		}
		return ret;
	}

	private static Map<Integer, Column> parseColumns (final JSONArray columnsJson) throws JSONException {
		Map<Integer, Column> ret = new HashMap<Integer, Column>();
		for (int i = 0; i < columnsJson.length(); i++) {
			JSONObject colJson = columnsJson.getJSONObject(i);
			String title = colJson.getString("title");
			String account = colJson.getString("account");
			String resource = colJson.getString("resource");
			String refresh = colJson.getString("refresh");
			ret.put(Integer.valueOf(i), new Column(i, title, account, resource, refresh));
		}
		return ret;
	}

	public enum AccountProvider {
		TWITTER;

		public static AccountProvider parse (final String s) {
			return valueOf(s.toUpperCase(Locale.UK));
		}

	}

	public static class Account {

		public final String id;
		public final AccountProvider provider;
		public final String consumerKey;
		public final String consumerSecret;
		public final String accessToken;
		public final String accessSecret;

		public Account (final String id, final AccountProvider provider, final String consumerKey, final String consumerSecret, final String accessToken, final String accessSecret) {
			this.id = id;
			this.provider = provider;
			this.consumerKey = consumerKey;
			this.consumerSecret = consumerSecret;
			this.accessToken = accessToken;
			this.accessSecret = accessSecret;
		}

		@Override
		public String toString () {
			StringBuilder s = new StringBuilder();
			s.append("Account{").append(this.id)
					.append(",").append(this.provider)
					.append("}");
			return s.toString();
		}

	}

	public static class Column {

		public final int index;
		public final String title;
		public final String accountId;
		public final String resource;
		public final String refresh;

		public Column (final int index, final String title, final String accountId, final String resource, final String refresh) {
			this.index = index;
			this.title = title;
			this.accountId = accountId;
			this.resource = resource;
			this.refresh = refresh;
		}

		@Override
		public String toString () {
			StringBuilder s = new StringBuilder();
			s.append("Column{").append(this.index)
					.append(",").append(this.title)
					.append(",").append(this.accountId)
					.append(",").append(this.resource)
					.append(",").append(this.refresh)
					.append("}");
			return s.toString();
		}

	}

}

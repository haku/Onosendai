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

	public Account getAccount (final String accountId) {
		return this.accounts.get(accountId);
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
		return Collections.unmodifiableMap(ret);
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
		return Collections.unmodifiableMap(ret);
	}

}

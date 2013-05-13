package com.vaguehope.onosendai.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.Environment;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class Config {

	private static final String KEY_ID = "id";
	private static final String KEY_TITLE = "title";
	private static final String KEY_ACCOUNT = "account";
	private static final String KEY_RESOURCE = "resource";
	private static final String KEY_REFRESH = "refresh";
	private static final String KEY_EXCLUDE = "exclude";

	private static final LogWrapper LOG = new LogWrapper("CFG");

	public static boolean isConfigured () {
		return configFile().exists();
	}

	public static boolean isTemplateConfigured () {
		// TODO Check not same as internal template.
		return templateFile().exists();
	}

	public static File writeTemplateConfig () throws ConfigException {
		try {
			final File t = templateFile();
			if (t.exists()) throw new ConfigException("Template file '" + t.getAbsolutePath() + "' already exists.");
			IoHelper.resourceToFile("/deck.conf", t);
			return t;
		}
		catch (IOException e) {
			throw new ConfigException(e);
		}
	}

	public static void useTemplateConfig () throws ConfigException {
		final File t = templateFile();
		if (!isTemplateConfigured()) throw new ConfigException("Template file '" + t.getAbsolutePath() + "' does not exist.");

		final File f = configFile();
		if (f.exists()) throw new ConfigException("Config file '" + f.getAbsolutePath() + "' already exists.");

		if (!t.renameTo(f)) throw new ConfigException("Failed to rename '" + t.getAbsolutePath() + "' to '" + f.getAbsolutePath() + "'.");
	}

	public static Config getConfig () throws ConfigUnavailableException {
		final File f = configFile();
		if (!f.exists()) throw new ConfigUnavailableException("Config file '" + f.getAbsolutePath() + "' does not exist.");
		try {
			return new Config(f);
		}
		catch (IOException e) {
			throw new ConfigUnavailableException(e);
		}
		catch (JSONException e) {
			throw new ConfigUnavailableException(e);
		}
	}

	private static File configFile () {
		return new File(Environment.getExternalStorageDirectory().getPath(), C.CONFIG_FILE_NAME);
	}

	private static File templateFile () {
		return new File(Environment.getExternalStorageDirectory().getPath(), C.TEMPLATE_CONFIG_FILE_NAME);
	}

	private final Map<String, Account> accounts;
	private final List<Column> feeds;

	private Config (final File f) throws IOException, JSONException {
		final String s = IoHelper.fileToString(f);
		final JSONObject o = (JSONObject) new JSONTokener(s).nextValue();

		final JSONArray accountsJson = o.getJSONArray("accounts");
		this.accounts = parseAccounts(accountsJson);

		final JSONArray feedsJson = o.getJSONArray("feeds");
		this.feeds = parseFeeds(feedsJson);
	}

	public Map<String, Account> getAccounts () {
		return this.accounts;
	}

	public Account getAccount (final String accountId) {
		return this.accounts.get(accountId);
	}

	public List<Column> getColumns () {
		return this.feeds;
	}

	public Column getColumnByPosition (final int position) {
		return this.feeds.get(position);
	}

	public Column getColumnById (final int columnId) {
		for (final Column col : this.feeds) {
			if (columnId == col.getId()) return col;
		}
		return null;
	}

	public int getColumnPositionById (final int columnId) {
		for (final Column col : this.feeds) {
			if (columnId == col.getId()) return this.feeds.indexOf(col);
		}
		return -1;
	}

	public Column findInternalColumn (final InternalColumnType res) {
		for (final Column col : getColumns()) {
			if (res.matchesColumn(col)) return col;
		}
		return null;
	}

	private static Map<String, Account> parseAccounts (final JSONArray accountsJson) throws JSONException {
		final Map<String, Account> ret = new HashMap<String, Account>();
		for (int i = 0; i < accountsJson.length(); i++) {
			final JSONObject accountJson = accountsJson.getJSONObject(i);
			final String id = accountJson.getString(KEY_ID);
			final AccountProvider provider = AccountProvider.parse(accountJson.getString("provider"));
			Account account;
			switch (provider) {
				case TWITTER:
					account = parseTwitterAccount(accountJson, id);
					break;
				case SUCCESSWHALE:
					account = parseSuccessWhaleAccount(accountJson, id);
					break;
				case BUFFER:
					account = parseBufferAccount(accountJson, id);
					break;
				default:
					throw new IllegalArgumentException("Unknown provider: " + provider);
			}
			ret.put(id, account);
		}
		return Collections.unmodifiableMap(ret);
	}

	private static Account parseTwitterAccount (final JSONObject accountJson, final String id) throws JSONException {
		final String consumerKey = accountJson.getString("consumerKey");
		final String consumerSecret = accountJson.getString("consumerSecret");
		final String accessToken = accountJson.getString("accessToken");
		final String accessSecret = accountJson.getString("accessSecret");
		return new Account(id, AccountProvider.TWITTER, consumerKey, consumerSecret, accessToken, accessSecret);
	}

	private static Account parseSuccessWhaleAccount (final JSONObject accountJson, final String id) throws JSONException {
		final String accessToken = accountJson.getString("username");
		final String accessSecret = accountJson.getString("password");
		return new Account(id, AccountProvider.SUCCESSWHALE, null, null, accessToken, accessSecret);
	}

	private static Account parseBufferAccount (final JSONObject accountJson, final String id) throws JSONException {
		final String accessToken = accountJson.getString("accessToken");
		return new Account(id, AccountProvider.BUFFER, null, null, accessToken, null);
	}

	/**
	 * // TODO allow multiple feeds per column.
	 */
	private static List<Column> parseFeeds (final JSONArray columnsJson) throws JSONException {
		final List<Column> ret = new ArrayList<Column>();
		for (int i = 0; i < columnsJson.length(); i++) {
			final JSONObject colJson = columnsJson.getJSONObject(i);
			final int id = colJson.getInt(KEY_ID);
			final String title = colJson.getString(KEY_TITLE);
			final String account = colJson.has(KEY_ACCOUNT) ? colJson.getString(KEY_ACCOUNT) : null;
			final String resource = colJson.getString(KEY_RESOURCE);
			final String refreshRaw = colJson.optString(KEY_REFRESH, null);
			final int refreshIntervalMins = parseFeedRefreshInterval(refreshRaw, account, title);
			final int[] excludeColumnIds = parseFeedExcludeColumns(colJson, title);
			final boolean notify = colJson.optBoolean("notify", false);
			ret.add(new Column(id, title, account, resource, refreshIntervalMins, excludeColumnIds, notify));
		}
		return Collections.unmodifiableList(ret);
	}

	private static int parseFeedRefreshInterval (final String refreshRaw, final String account, final String title) {
		final int refreshIntervalMins = TimeParser.parseDuration(refreshRaw);
		if (refreshIntervalMins < 1 && account != null) LOG.w("Column '%s' has invalid refresh interval: '%s'.", title, refreshRaw);
		return refreshIntervalMins;
	}

	private static int[] parseFeedExcludeColumns (final JSONObject colJson, final String title) throws JSONException {
		int[] excludeColumnIds = null;
		if (colJson.has(KEY_EXCLUDE)) {
			final JSONArray exArr = colJson.optJSONArray(KEY_EXCLUDE);
			if (exArr != null) {
				excludeColumnIds = asIntArr(exArr);
			}
			else {
				final int exId = colJson.optInt(KEY_EXCLUDE, Integer.MIN_VALUE);
				if (exId > Integer.MIN_VALUE) {
					excludeColumnIds = new int[] { exId };
				}
			}
			if (excludeColumnIds == null) LOG.w("Column '%s' has invalid exclude value: '%s'.", title, colJson.getString(KEY_EXCLUDE));
		}
		return excludeColumnIds;
	}

	private static int[] asIntArr (final JSONArray arr) throws JSONException {
		final int[] ret = new int[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			ret[i] = arr.getInt(i);
		}
		return ret;
	}

}

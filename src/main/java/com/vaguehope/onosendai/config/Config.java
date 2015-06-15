package com.vaguehope.onosendai.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.Environment;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.StringHelper;

public class Config {

	private static final String SECTION_ACCOUNTS = "accounts";
	private static final String SECTION_FEEDS = "feeds";

	public static boolean isConfigFilePresent () {
		return configFile().exists();
	}

	public static File writeExampleConfig () throws ConfigException {
		try {
			final File t = configFile();
			if (t.exists()) throw new ConfigException("Configuration file '" + t.getAbsolutePath() + "' already exists.");
			IoHelper.resourceToFile("/deck.conf", t);
			return t;
		}
		catch (final IOException e) {
			throw new ConfigException(e);
		}
	}

	public static Config getConfig () throws ConfigUnavailableException {
		final File f = configFile();
		if (!f.exists()) throw new ConfigUnavailableException("Configuration file '" + f.getAbsolutePath() + "' does not exist.");
		try {
			return new Config(f);
		}
		catch (final IOException e) {
			throw new ConfigUnavailableException(e);
		}
		catch (final JSONException e) {
			throw new ConfigUnavailableException(e);
		}
	}

	public static File configFile () {
		return new File(Environment.getExternalStorageDirectory().getPath(), C.CONFIG_FILE_NAME);
	}

	private final Map<String, Account> accounts;
	private final List<Column> feeds;

	private Config (final File f) throws IOException, JSONException {
		final String s = IoHelper.fileToString(f);
		final JSONObject o = (JSONObject) new JSONTokener(s).nextValue();
		this.accounts = parseAccounts(o.getJSONArray(SECTION_ACCOUNTS));
		this.feeds = parseFeeds(o.getJSONArray(SECTION_FEEDS));
	}

	Config (final Collection<Account> accounts, final Collection<Column> columns) {
		final Map<String, Account> a = new LinkedHashMap<String, Account>();
		for (final Account account : accounts) {
			a.put(account.getId(), account);
		}

		final List<Column> f = new ArrayList<Column>();
		f.addAll(columns);

		this.accounts = Collections.unmodifiableMap(a);
		this.feeds = Collections.unmodifiableList(f);
	}

	public Map<String, Account> getAccounts () {
		return this.accounts;
	}

	public Account getAccount (final String accountId) {
		if (StringHelper.isEmpty(accountId)) return null;
		return this.accounts.get(accountId);
	}

	public List<Account> getAccounts (final Collection<String> accountIds) {
		if (accountIds == null || accountIds.size() < 1) return Collections.emptyList();
		final List<Account> ret = new ArrayList<Account>(accountIds.size());
		for (final String id : accountIds) {
			final Account a = getAccount(id);
			if (a != null) ret.add(a);
		}
		return ret;
	}

	public Account firstAccountOfType (final AccountProvider provider) {
		for (final Account a : this.accounts.values()) {
			if (a.getProvider() == provider) return a;
		}
		return null;
	}

	public int getColumnCount () {
		return this.feeds.size();
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

	public int countAccountsWithoutSecrets () {
		int ret = 0;
		for (final Account account : this.accounts.values()) {
			if (!account.hasSecrets()) ret += 1;
		}
		return ret;
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject j = new JSONObject();

		final JSONArray aj = new JSONArray();
		for (final Account a : this.accounts.values()) {
			aj.put(a.withoutSecrets().toJson());
		}
		j.put(SECTION_ACCOUNTS, aj);

		final JSONArray fj = new JSONArray();
		for (final Column c : this.feeds) {
			fj.put(c.toJson());
		}
		j.put(SECTION_FEEDS, fj);

		return j;
	}

	private static Map<String, Account> parseAccounts (final JSONArray accountsJson) throws JSONException {
		final Map<String, Account> ret = new LinkedHashMap<String, Account>();
		for (int i = 0; i < accountsJson.length(); i++) {
			final Account account = Account.parseJson(accountsJson.getJSONObject(i));
			ret.put(account.getId(), account);
		}
		return Collections.unmodifiableMap(ret);
	}

	/**
	 * // TODO allow multiple feeds per column.
	 */
	private static List<Column> parseFeeds (final JSONArray columnsJson) throws JSONException {
		final List<Column> ret = new ArrayList<Column>();
		for (int i = 0; i < columnsJson.length(); i++) {
			final Column column = Column.parseJson(columnsJson.getJSONObject(i));
			ret.add(column);
		}
		return Collections.unmodifiableList(ret);
	}

}

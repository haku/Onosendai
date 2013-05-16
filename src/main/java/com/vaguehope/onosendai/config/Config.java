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

public class Config {

	static final String SECTION_ACCOUNTS = "accounts";
	static final String SECTION_FEEDS = "feeds";

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

	static File configFile () {
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
		this.accounts = parseAccounts(o.getJSONArray(SECTION_ACCOUNTS));
		this.feeds = parseFeeds(o.getJSONArray(SECTION_FEEDS));
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

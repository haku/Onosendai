package com.vaguehope.onosendai.config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vaguehope.onosendai.util.IoHelper;

public class ConfigBuilder {

	private final JSONObject obj;
	private final JSONArray accounts;
	private final JSONArray feeds;

	public ConfigBuilder () throws ConfigException {
		try {
			this.obj = new JSONObject();
			this.accounts = new JSONArray();
			this.feeds = new JSONArray();
			this.obj.put(Config.SECTION_ACCOUNTS, this.accounts);
			this.obj.put(Config.SECTION_FEEDS, this.feeds);
		}
		catch (final JSONException e) {
			throw new ConfigException(e);
		}
	}

	public ConfigBuilder account (final Account account) throws ConfigException {
		try {
			final JSONObject accountObj = new JSONObject();
			switch (account.getProvider()) {
				case SUCCESSWHALE:
					accountObj.put(Config.KEY_ID, account.getId());
					accountObj.put(Config.KEY_PROVIDER, account.getProvider().toString());
					accountObj.put(Config.KEY_USERNAME, account.getAccessToken());
					accountObj.put(Config.KEY_PASSWORD, account.getAccessSecret());
					break;
				default:
					throw new IllegalArgumentException("Unsupported account provilder: " + account.getProvider());
			}
			this.accounts.put(accountObj);
			return this;
		}
		catch (final JSONException e) {
			throw new ConfigException(e);
		}
	}

	public ConfigBuilder columns (final List<Column> columns) throws ConfigException {
		for (final Column column : columns) {
			column(column);
		}
		return this;
	}

	public ConfigBuilder column (final Column column) throws ConfigException {
		try {
			final JSONObject colObj = new JSONObject();

			colObj.put(Config.KEY_ID, column.getId());
			colObj.put(Config.KEY_TITLE, column.getTitle());
			colObj.put(Config.KEY_ACCOUNT, column.getAccountId());
			colObj.put(Config.KEY_RESOURCE, column.getResource());
			colObj.put(Config.KEY_REFRESH, column.getRefreshIntervalMins() + "mins");
			colObj.put(Config.KEY_EXCLUDE, toJsonArray(column.getExcludeColumnIds()));
			colObj.put(Config.KEY_NOTIFY, column.isNotify());

			this.feeds.put(colObj);
			return this;
		}
		catch (final JSONException e) {
			throw new ConfigException(e);
		}
	}

	public ConfigBuilder readLater () throws ConfigException {
		column(new Column(this.feeds.length(), "Reading List", null, InternalColumnType.LATER.name(), -1, null, false));
		return this;
	}

	public void writeMain () throws ConfigException {
		final File f = Config.configFile();
		if (f.exists()) throw new ConfigException("Config file '" + f.getAbsolutePath() + "' already exists.");
		write(f);
	}

	public void write (final File f) throws ConfigException {
		try {
			IoHelper.stringToFile(this.obj.toString(2), f);
		}
		catch (final JSONException e) {
			throw new ConfigException(e);
		}
		catch (final IOException e) {
			throw new ConfigException(e);
		}
	}

	private static JSONArray toJsonArray (final int[] arr) {
		if (arr == null) return null;
		final JSONArray ja = new JSONArray();
		for (int i : arr) {
			ja.put(i);
		}
		return ja;
	}

}

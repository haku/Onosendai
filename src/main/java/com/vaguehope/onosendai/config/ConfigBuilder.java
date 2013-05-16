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
			this.accounts.put(account.toJson());
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
			this.feeds.put(column.toJson());
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

}

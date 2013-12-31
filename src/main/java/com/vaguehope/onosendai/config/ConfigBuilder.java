package com.vaguehope.onosendai.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONException;

import android.content.Context;

public class ConfigBuilder {

	private final Map<String, Account> accounts;
	private final Map<Integer, Column> columns;

	public ConfigBuilder () {
		this.accounts = new LinkedHashMap<String, Account>();
		this.columns = new LinkedHashMap<Integer, Column>();
	}

	public ConfigBuilder config (final Config config) throws ConfigException {
		accounts(config.getAccounts().values());
		columns(config.getColumns());
		return this;
	}

	public ConfigBuilder accounts (final Collection<Account> acnts) throws ConfigException {
		for (Account account : acnts) {
			account(account);
		}
		return this;
	}

	public ConfigBuilder account (final Account account) throws ConfigException {
		if (account.getId() == null || account.getId().isEmpty()) throw new ConfigException("Account is missing Id.");
		if (this.accounts.containsKey(account.getId())) throw new ConfigException("Account ID already used: " + account.getId());
		this.accounts.put(account.getId(), account);
		return this;
	}

	public ConfigBuilder columns (final Collection<Column> cols) throws ConfigException {
		for (final Column column : cols) {
			column(column);
		}
		return this;
	}

	public ConfigBuilder column (final Column col) throws ConfigException {
		Column colToAdd = col;
		int id = col.getId();

		if (id < 0) {
			id = this.columns.size();
			colToAdd = new Column(id, colToAdd);
		}

		final Integer key = Integer.valueOf(id);
		if (this.columns.containsKey(key)) throw new ConfigException("Column ID already used: " + id);

		this.columns.put(id, colToAdd);
		return this;
	}

	public ConfigBuilder readLater () throws ConfigException {
		column(new Column(this.columns.size(), "Reading List", null, InternalColumnType.LATER.name(), -1, null, false, false));
		return this;
	}

	public void writeOverMain (final Context context) throws ConfigException {
		try {
			final Prefs prefs = new Prefs(context);
			prefs.writeOver(this.accounts.values(), this.columns.values());
		}
		catch (JSONException e) {
			throw new ConfigException(e);
		}
	}

}

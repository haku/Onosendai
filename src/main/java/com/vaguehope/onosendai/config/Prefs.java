package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.vaguehope.onosendai.util.ArrayHelper;

public class Prefs {

	private static final String ID_SEP = ":";
	private static final String KEY_ACCOUNT_IDS = "account_ids";
	private static final String KEY_ACCOUNT_PREFIX = "account_";
	private static final String KEY_COLUMN_IDS = "column_ids";
	private static final String KEY_COLUMN_PREFIX = "column_";

	private final SharedPreferences sharedPreferences;

	public Prefs (final Context context) {
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public Prefs (final PreferenceManager preferenceManager) {
		this.sharedPreferences = preferenceManager.getSharedPreferences();
	}

	public boolean isConfigured () {
		return readAccountIds().size() > 0;
	}

	public void writeOver (final Collection<Account> accounts, final Collection<Column> columns) throws JSONException {
		for (final String id : readColumnIdsStr()) {
			deleteColumn(id);
		}
		for (final String id : readAccountIds()) {
			deleteAccount(id);
		}
		for (final Account account : accounts) {
			writeNewAccount(account);
		}
		for (final Column column : columns) {
			writeNewColumn(column);
		}
	}

	public String getNextAccountId () {
		return nextId(readAccountIds(), KEY_ACCOUNT_PREFIX);
	}

	public List<String> readAccountIds () {
		return readIds(KEY_ACCOUNT_IDS);
	}

	public Account readAccount (final String id) throws JSONException {
		final String raw = this.sharedPreferences.getString(id, null);
		if (raw == null) return null;
		return Account.parseJson(raw);
	}

	public void writeNewAccount (final Account account) throws JSONException {
		final String id = account.getId();
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");

		final String json = account.toJson().toString();
		final String idsS = appendId(readAccountIds(), id);

		final Editor e = this.sharedPreferences.edit();
		e.putString(id, json);
		e.putString(KEY_ACCOUNT_IDS, idsS);
		e.commit();
	}

	public void deleteAccount (final Account account) {
		deleteAccount(account.getId());
	}

	public void deleteAccount (final String id) {
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");

		final List<String> ids = new ArrayList<String>();
		ids.addAll(readAccountIds());
		if (!ids.remove(id)) throw new IllegalStateException("Tried to delete account '" + id + "' that does not exist in '" + ids + "'.");
		final String idsS = ArrayHelper.join(ids, ID_SEP);

		final Editor e = this.sharedPreferences.edit();
		e.putString(KEY_ACCOUNT_IDS, idsS);
		e.remove(id);
		e.commit();
	}

	public static String makeColumnId (final int id) {
		return KEY_COLUMN_PREFIX + id;
	}

	private static int parseColumnId (final String id) {
		if (!id.startsWith(KEY_COLUMN_PREFIX)) throw new IllegalArgumentException("Column id '" + id + "' does not start with '" + KEY_COLUMN_PREFIX + "'.");
		return Integer.parseInt(id.substring(KEY_COLUMN_PREFIX.length()));
	}

	public int getNextColumnId () {
		return parseColumnId(nextId(readColumnIdsStr(), KEY_COLUMN_PREFIX));
	}

	public List<Integer> readColumnIds () {
		final List<Integer> ret = new ArrayList<Integer>();
		for (final String id : readColumnIdsStr()) {
			ret.add(parseColumnId(id));
		}
		return ret;
	}

	private List<String> readColumnIdsStr () {
		return readIds(KEY_COLUMN_IDS);
	}

	public Column readColumn (final int id) throws JSONException {
		final String raw = this.sharedPreferences.getString(makeColumnId(id), null);
		if (raw == null) return null;
		return Column.parseJson(raw);
	}

	public void writeNewColumn (final Column column) throws JSONException {
		final String id = makeColumnId(column.getId());

		final String json = column.toJson().toString();
		final String idsS = appendId(readColumnIdsStr(), id);

		final Editor e = this.sharedPreferences.edit();
		e.putString(id, json);
		e.putString(KEY_COLUMN_IDS, idsS);
		e.commit();
	}

	public void deleteColumn (final Column column) {
		deleteColumn(makeColumnId(column.getId()));
	}

	public void deleteColumn (final String id) {
		final List<String> ids = new ArrayList<String>();
		ids.addAll(readColumnIdsStr());
		if (!ids.remove(id)) throw new IllegalStateException("Tried to delete column '" + id + "' that does not exist in '" + ids + "'.");
		final String idsS = ArrayHelper.join(ids, ID_SEP);

		final Editor e = this.sharedPreferences.edit();
		e.putString(KEY_COLUMN_IDS, idsS);
		e.remove(id);
		e.commit();
	}

	private static String nextId (final List<String> existingIds, final String prefix) {
		int x = 1;
		while (true) {
			final String id = prefix + x;
			if (!existingIds.contains(id)) return id;
			x += 1;
		}
	}

	private List<String> readIds (final String key) {
		final String ids = this.sharedPreferences.getString(key, null);
		if (ids == null || ids.length() < 1) return Collections.emptyList();
		return Arrays.asList(ids.split(ID_SEP));
	}

	private static String appendId (final List<String> existingIds, final String id) {
		final List<String> ids = new ArrayList<String>();
		ids.addAll(existingIds);
		ids.add(id);
		return ArrayHelper.join(ids, ID_SEP);
	}

}

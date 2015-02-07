package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

public class Prefs {

	private static final String ID_SEP = ":";
	private static final String KEY_ACCOUNT_IDS = "account_ids";
	private static final String KEY_ACCOUNT_PREFIX = "account_";
	private static final String KEY_COLUMN_IDS = "column_ids";
	private static final String KEY_COLUMN_PREFIX = "column_";
	private static final String KEY_FILTER_IDS = "filter_ids";
	private static final String KEY_FILTER_PREFIX = "filter_";

	private static final LogWrapper LOG = new LogWrapper("PRF");

	private final SharedPreferences sharedPreferences;

	public Prefs (final Context context) {
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public Prefs (final PreferenceManager preferenceManager) {
		this.sharedPreferences = preferenceManager.getSharedPreferences();
	}

	public SharedPreferences getSharedPreferences () {
		return this.sharedPreferences;
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

	public Config asConfig () throws JSONException {
		return new Config(readAccounts(), readColumns());
	}

	public String getNextAccountId () {
		return nextId(readAccountIds(), KEY_ACCOUNT_PREFIX);
	}

	public List<String> readAccountIds () {
		return readIds(KEY_ACCOUNT_IDS);
	}

	public Collection<Account> readAccounts () throws JSONException {
		final Collection<Account> ret = new ArrayList<Account>();
		for (final String id : readAccountIds()) {
			ret.add(readAccount(id));
		}
		return ret;
	}

	public Account readAccount (final String id) throws JSONException {
		if (StringHelper.isEmpty(id)) return null;
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

		LOG.i("Wrote new account %s.", id);
	}

	public void updateExistingAccount (final Account account) throws JSONException {
		final String id = account.getId();
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");

		final String json = account.toJson().toString();

		final Editor e = this.sharedPreferences.edit();
		e.putString(id, json);
		e.commit();

		LOG.i("Updated account %s.", id);
	}

	public void deleteAccount (final Account account) {
		deleteAccount(account.getId());
	}

	public void deleteAccount (final String id) {
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");

		final List<String> ids = new ArrayList<String>(readAccountIds());
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

	/**
	 * 0 based.
	 */
	public int readColumnPosition (final int id) {
		return readColumnPosition(makeColumnId(id));
	}

	private int readColumnPosition (final String id) {
		return readColumnIdsStr().indexOf(id);
	}

	public List<Column> readColumns () throws JSONException {
		final List<Column> ret = new ArrayList<Column>();
		for (final String id : readColumnIdsStr()) {
			ret.add(readColumn(id));
		}
		return ret;
	}

	public Map<Integer, Column> readColumnsAsMap () throws JSONException {
		final Map<Integer, Column> map = new LinkedHashMap<Integer, Column>();
		for (final Column col : readColumns()) {
			map.put(Integer.valueOf(col.getId()), col);
		}
		return map;
	}

	public Column readColumn (final int id) throws JSONException {
		return readColumn(makeColumnId(id));
	}

	private Column readColumn (final String id) throws JSONException {
		final String raw = this.sharedPreferences.getString(id, null);
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

	public void writeUpdatedColumn (final Column column) throws JSONException {
		final String id = makeColumnId(column.getId());
		final String json = column.toJson().toString();
		final Editor e = this.sharedPreferences.edit();
		e.putString(id, json);
		e.commit();
	}

	public void moveColumnToPosition (final int id, final int position) {
		moveColumnToPosition(makeColumnId(id), position);
	}

	private void moveColumnToPosition (final String id, final int position) {
		final List<String> ids = readColumnIdsStr();
		if (ids.indexOf(id) == position) return; // Already in that position.
		if (position < 0 || position >= ids.size()) throw new IllegalArgumentException("Position '" + position + "' is out of bounds.");

		final List<String> newIds = new ArrayList<String>(ids);
		if (!newIds.remove(id)) throw new IllegalArgumentException("ID '" + id + "' not found.");
		newIds.add(position, id);
		final String idsS = ArrayHelper.join(newIds, ID_SEP);

		final Editor e = this.sharedPreferences.edit();
		e.putString(KEY_COLUMN_IDS, idsS);
		e.commit();
	}

	public void deleteColumn (final Column column) {
		deleteColumn(makeColumnId(column.getId()));
	}

	private void deleteColumn (final String id) {
		final List<String> ids = new ArrayList<String>(readColumnIdsStr());
		if (!ids.remove(id)) throw new IllegalStateException("Tried to delete column '" + id + "' that does not exist in '" + ids + "'.");
		final String idsS = ArrayHelper.join(ids, ID_SEP);

		final Editor e = this.sharedPreferences.edit();
		e.putString(KEY_COLUMN_IDS, idsS);
		e.remove(id);
		e.commit();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public String getNextFilterId () {
		return nextId(readFilterIds(), KEY_FILTER_PREFIX);
	}

	public List<String> readFilterIds () {
		return readIds(KEY_FILTER_IDS);
	}

	public String readFilter (final String id) {
		if (StringHelper.isEmpty(id)) return null;
		return this.sharedPreferences.getString(id, null);
	}

	public void writeFilter (final String id, final String filter) {
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Filter ID missing.");

		final String idsS = appendId(readFilterIds(), id);

		final Editor e = this.sharedPreferences.edit();
		e.putString(id, filter);
		e.putString(KEY_FILTER_IDS, idsS);
		e.commit();

		LOG.i("Wrote new filter %s: %s.", id, filter);
	}

	public void deleteFilter (final String id) {
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Filter ID missing.");

		final List<String> ids = new ArrayList<String>(readFilterIds());
		if (!ids.remove(id)) throw new IllegalStateException("Tried to delete filter '" + id + "' that does not exist in '" + ids + "'.");
		final String idsS = ArrayHelper.join(ids, ID_SEP);

		final Editor e = this.sharedPreferences.edit();
		e.putString(KEY_FILTER_IDS, idsS);
		e.remove(id);
		e.commit();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

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
		final List<String> ids = new ArrayList<String>(existingIds);
		ids.add(id);
		return ArrayHelper.join(ids, ID_SEP);
	}

}

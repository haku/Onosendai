package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.vaguehope.onosendai.util.ArrayHelper;

public class Prefs {

	private static final String ID_SEP = ":";
	private static final String KEY_ACCOUNT_IDS = "account_ids";
	private static final String KEY_ACCOUNT_PREFIX = "account_";

	private final PreferenceManager preferenceManager;

	public Prefs (final PreferenceManager preferenceManager) {
		this.preferenceManager = preferenceManager;
	}

	private SharedPreferences getSharedPreferences () {
		return this.preferenceManager.getSharedPreferences();
	}

	public String getNextAccountId () {
		final List<String> ids = readAccountIds();
		int x = 0;
		while (true) {
			final String id = KEY_ACCOUNT_PREFIX + x;
			if (!ids.contains(id)) return id;
			x += 1;
		}
	}

	public List<String> readAccountIds () {
		final String ids = getSharedPreferences().getString(KEY_ACCOUNT_IDS, null);
		if (ids == null || ids.length() < 1) return Collections.emptyList();
		return Arrays.asList(ids.split(ID_SEP));
	}

	public Account readAccount (final String id) throws JSONException {
		final String raw = getSharedPreferences().getString(id, null);
		if (raw == null) return null;
		return Account.parseJson(raw);
	}

	public void writeNewAccount (final Account account) throws JSONException {
		final String id = account.getId();
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");

		final String json = account.toJson().toString();

		final List<String> ids = new ArrayList<String>();
		ids.addAll(readAccountIds());
		ids.add(id);
		final String idsS = ArrayHelper.join(ids, ID_SEP);

		final Editor e = getSharedPreferences().edit();
		e.putString(id, json);
		e.putString(KEY_ACCOUNT_IDS, idsS);
		e.commit();
	}

	public void deleteAccount (final Account account) {
		final String id = account.getId();
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");

		final List<String> ids = new ArrayList<String>();
		ids.addAll(readAccountIds());
		if (!ids.remove(id)) throw new IllegalStateException("Tried to delete account '" + id + "' that does not exist in '" + ids + "'.");
		final String idsS = ArrayHelper.join(ids, ID_SEP);

		final Editor e = getSharedPreferences().edit();
		e.putString(KEY_ACCOUNT_IDS, idsS);
		e.remove(id);
		e.commit();
	}

}

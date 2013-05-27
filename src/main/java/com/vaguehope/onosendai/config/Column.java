package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Titleable;

public class Column implements Titleable {

	private static final String KEY_ID = "id";
	private static final String KEY_TITLE = "title";
	private static final String KEY_ACCOUNT = "account";
	private static final String KEY_RESOURCE = "resource";
	private static final String KEY_REFRESH = "refresh";
	private static final String KEY_EXCLUDE = "exclude";
	private static final String KEY_NOTIFY = "notify";

	private static final LogWrapper LOG = new LogWrapper("COL");

	private final int id;
	private final String title;
	private final String accountId;
	private final String resource;
	private final int refreshIntervalMins;
	private final Set<Integer> excludeColumnIds;
	private final boolean notify;

	public Column (final int id, final Column c) {
		this(id, c.getTitle(), c.getAccountId(), c.getResource(), c.getRefreshIntervalMins(), c.getExcludeColumnIds(), c.isNotify());
	}

	public Column (
			final int id,
			final String title,
			final String accountId,
			final String resource,
			final int refreshIntervalMins,
			final Set<Integer> excludeColumnIds,
			final boolean notify) {
		this.id = id;
		this.title = title;
		this.accountId = accountId;
		this.resource = resource;
		this.refreshIntervalMins = refreshIntervalMins;
		this.excludeColumnIds = excludeColumnIds;
		this.notify = notify;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id;
		result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
		result = prime * result + ((this.accountId == null) ? 0 : this.accountId.hashCode());
		result = prime * result + ((this.resource == null) ? 0 : this.resource.hashCode());
		result = prime * result + this.refreshIntervalMins;
		result = prime * result + ((this.excludeColumnIds == null) ? 0 : this.title.hashCode());
		result = prime * result + (this.notify ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Column)) return false;
		Column that = (Column) o;
		return EqualHelper.equal(this.id, that.id) &&
				EqualHelper.equal(this.title, that.title) &&
				EqualHelper.equal(this.accountId, that.accountId) &&
				EqualHelper.equal(this.resource, that.resource) &&
				this.refreshIntervalMins == that.refreshIntervalMins &&
				EqualHelper.equal(this.excludeColumnIds, that.excludeColumnIds) &&
				EqualHelper.equal(this.notify, that.notify);
	}

	@Override
	public String getUiTitle () {
		return this.title != null && !this.title.isEmpty()
				? this.title
				: String.format("Column %s", this.id);
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.id)
				.append(",").append(this.title)
				.append(",").append(this.accountId)
				.append(",").append(this.resource)
				.append(",").append(this.refreshIntervalMins)
				.append(",").append(this.excludeColumnIds)
				.append(",").append(this.notify)
				.append("}");
		return s.toString();
	}

	public int getId () {
		return this.id;
	}

	public String getTitle () {
		return this.title;
	}

	public String getAccountId () {
		return this.accountId;
	}

	public String getResource () {
		return this.resource;
	}

	public int getRefreshIntervalMins () {
		return this.refreshIntervalMins;
	}

	public Set<Integer> getExcludeColumnIds () {
		return this.excludeColumnIds;
	}

	public boolean isNotify () {
		return this.notify;
	}

	public static List<String> titles (final Collection<Column> columns) {
		if (columns == null) return null;
		List<String> ret = new ArrayList<String>(columns.size());
		for (Column col : columns) {
			ret.add(col.getTitle());
		}
		return ret;
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(KEY_ID, getId());
		json.put(KEY_TITLE, getTitle());
		json.put(KEY_ACCOUNT, getAccountId());
		json.put(KEY_RESOURCE, getResource());
		json.put(KEY_REFRESH, getRefreshIntervalMins() + "mins");
		json.put(KEY_EXCLUDE, toJsonArray(getExcludeColumnIds()));
		json.put(KEY_NOTIFY, isNotify());
		return json;
	}

	private static JSONArray toJsonArray (final Set<Integer> ints) {
		if (ints == null) return null;
		final JSONArray ja = new JSONArray();
		for (Integer i : ints) {
			ja.put(i);
		}
		return ja;
	}

	public static Column parseJson (final String json) throws JSONException {
		if (json == null) return null;
		return parseJson((JSONObject) new JSONTokener(json).nextValue());
	}

	public static Column parseJson (final JSONObject json) throws JSONException {
		final int id = json.getInt(KEY_ID);
		final String title = json.getString(KEY_TITLE);
		final String account = json.has(KEY_ACCOUNT) ? json.getString(KEY_ACCOUNT) : null;
		final String resource = json.getString(KEY_RESOURCE);
		final String refreshRaw = json.optString(KEY_REFRESH, null);
		final int refreshIntervalMins = parseFeedRefreshInterval(refreshRaw, account, title);
		final Set<Integer> excludeColumnIds = parseFeedExcludeColumns(json, title);
		final boolean notify = json.optBoolean(KEY_NOTIFY, false);
		return new Column(id, title, account, resource, refreshIntervalMins, excludeColumnIds, notify);
	}

	private static int parseFeedRefreshInterval (final String refreshRaw, final String account, final String title) {
		final int refreshIntervalMins = TimeParser.parseDuration(refreshRaw);
		if (refreshIntervalMins < 0 && account != null) LOG.w("Column '%s' has invalid refresh interval: '%s'.", title, refreshRaw);
		return refreshIntervalMins;
	}

	private static Set<Integer> parseFeedExcludeColumns (final JSONObject colJson, final String title) throws JSONException {
		Set<Integer> excludeColumnIds = null;
		if (colJson.has(KEY_EXCLUDE)) {
			final JSONArray exArr = colJson.optJSONArray(KEY_EXCLUDE);
			if (exArr != null) {
				excludeColumnIds = asIntSet(exArr);
			}
			else {
				final int exId = colJson.optInt(KEY_EXCLUDE, Integer.MIN_VALUE);
				if (exId > Integer.MIN_VALUE) {
					excludeColumnIds = Collections.singleton(Integer.valueOf(exId));
				}
			}
			if (excludeColumnIds == null) LOG.w("Column '%s' has invalid exclude value: '%s'.", title, colJson.getString(KEY_EXCLUDE));
		}
		return excludeColumnIds == null ? null : Collections.unmodifiableSet(excludeColumnIds);
	}

	private static Set<Integer> asIntSet (final JSONArray arr) throws JSONException {
		final Set<Integer> ret = new HashSet<Integer>();
		for (int i = 0; i < arr.length(); i++) {
			ret.add(Integer.valueOf(arr.getInt(i)));
		}
		return ret;
	}

}

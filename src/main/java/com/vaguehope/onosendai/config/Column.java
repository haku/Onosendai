package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class Column {

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
	private final int[] excludeColumnIds;
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
			final int[] excludeColumnIds,
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
		result = prime * result + ((this.accountId == null) ? 0 : this.accountId.hashCode());
		result = prime * result + this.id;
		result = prime * result + this.refreshIntervalMins;
		result = prime * result + ((this.resource == null) ? 0 : this.resource.hashCode());
		result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
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
				this.refreshIntervalMins == that.refreshIntervalMins;
	}

	public String humanId () {
		return this.title != null && !this.title.isEmpty()
				? this.title
				: String.format("Column %s", this.id);
	}

	public String humanDescription () {
		if (this.accountId != null && !this.accountId.isEmpty()) {
			return String.format("Column for account %s", this.accountId); // TODO want something like 'SuccessWhale column'.
		}
		return "";
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.id)
				.append(",").append(this.title)
				.append(",").append(this.accountId)
				.append(",").append(this.resource)
				.append(",").append(this.refreshIntervalMins)
				.append(",").append(Arrays.toString(this.excludeColumnIds))
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

	public int[] getExcludeColumnIds () {
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

	private static JSONArray toJsonArray (final int[] arr) {
		if (arr == null) return null;
		final JSONArray ja = new JSONArray();
		for (int i : arr) {
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
		final int[] excludeColumnIds = parseFeedExcludeColumns(json, title);
		final boolean notify = json.optBoolean(KEY_NOTIFY, false);
		return new Column(id, title, account, resource, refreshIntervalMins, excludeColumnIds, notify);
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

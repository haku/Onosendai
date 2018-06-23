package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.Titleable;

public class Column implements Titleable {

	public static final int ID_CACHED = -101;

	private static final String KEY_ID = "id";
	private static final String KEY_TITLE = "title";
	private static final String KEY_FEEDS = "feeds";
	private static final String KEY_ACCOUNT = "account";
	private static final String KEY_RESOURCE = "resource";
	private static final String KEY_REFRESH = "refresh";
	private static final String KEY_EXCLUDE = "exclude";
	private static final String KEY_HIDE_RT = "hide_rt";
	private static final String KEY_NOTIFY = "notify";
	private static final String KEY_INLINE_MEDIA = "inline_media";
	private static final String KEY_HD_MEDIA = "hd_media";

	private static final LogWrapper LOG = new LogWrapper("COL");

	private final int id;
	private final String title;
	private final Set<ColumnFeed> feeds;
	private final int refreshIntervalMins;
	private final Set<Integer> excludeColumnIds;
	private final boolean hideRetweets;
	private final NotificationStyle notificationStyle;
	private final InlineMediaStyle inlineMediaStyle;
	private final boolean hdMedia;

	public Column (final int id, final Column c) {
		this(id, c.getTitle(), c.getFeeds(), c.getRefreshIntervalMins(), c.getExcludeColumnIds(), c.isHideRetweets(),
				c.getNotificationStyle(), c.getInlineMediaStyle(), c.isHdMedia());
	}

	public Column (final Set<Integer> newExcludeColumnIds, final Column c) {
		this(c.getId(), c.getTitle(), c.getFeeds(), c.getRefreshIntervalMins(), newExcludeColumnIds, c.isHideRetweets(),
				c.getNotificationStyle(), c.getInlineMediaStyle(), c.isHdMedia());
	}

	public Column (
			final int id,
			final String title,
			final ColumnFeed feed,
			final int refreshIntervalMins,
			final Set<Integer> excludeColumnIds,
			final boolean hideRetweets,
			final NotificationStyle notificationStyle,
			final InlineMediaStyle inlineMediaStyle,
			final boolean hdMedia) {
		this(id, title, feed != null ? Collections.singleton(feed) : null, refreshIntervalMins, excludeColumnIds, hideRetweets,
				notificationStyle, inlineMediaStyle, hdMedia);
	}

	public Column (
			final int id,
			final String title,
			final Set<ColumnFeed> feeds,
			final int refreshIntervalMins,
			final Set<Integer> excludeColumnIds,
			final boolean hideRetweets,
			final NotificationStyle notificationStyle,
			final InlineMediaStyle inlineMediaStyle,
			final boolean hdMedia) {
		this.id = id;
		this.title = title;
		this.feeds = feeds != null ? Collections.unmodifiableSet(CollectionHelper.assertNoNulls(feeds)) : null;
		this.refreshIntervalMins = refreshIntervalMins;
		this.excludeColumnIds = excludeColumnIds;
		this.hideRetweets = hideRetweets;
		this.notificationStyle = notificationStyle;
		this.inlineMediaStyle = inlineMediaStyle;
		this.hdMedia = hdMedia;
	}

	public Column replaceAccount (final Account newAccount, final InternalColumnType ict) {
		final ColumnFeed oldFeed = ict.findInFeeds(getFeeds());
		if (oldFeed == null) throw new IllegalArgumentException("ICT " + ict + " not found in feeds: " + getFeeds());
		final List<ColumnFeed> newFeeds = new ArrayList<ColumnFeed>(getFeeds());
		Collections.replaceAll(newFeeds, oldFeed, new ColumnFeed(newAccount.getId(), oldFeed.getResource()));
		return new Column(getId(), getTitle(), new LinkedHashSet<ColumnFeed>(newFeeds), getRefreshIntervalMins(),
				getExcludeColumnIds(), isHideRetweets(), getNotificationStyle(), getInlineMediaStyle(), isHdMedia());
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id;
		result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
		result = prime * result + ((this.feeds == null) ? 0 : this.feeds.hashCode());
		result = prime * result + this.refreshIntervalMins;
		result = prime * result + ((this.excludeColumnIds == null) ? 0 : this.title.hashCode());
		result = prime * result + (this.notificationStyle == null ? 0 : this.notificationStyle.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Column)) return false;
		final Column that = (Column) o;
		return EqualHelper.equal(this.id, that.id) &&
				EqualHelper.equal(this.title, that.title) &&
				EqualHelper.equal(this.feeds, that.feeds) &&
				this.refreshIntervalMins == that.refreshIntervalMins &&
				EqualHelper.equal(this.excludeColumnIds, that.excludeColumnIds) &&
				EqualHelper.equal(this.hideRetweets, that.hideRetweets) &&
				EqualHelper.equal(this.notificationStyle, that.notificationStyle) &&
				EqualHelper.equal(this.inlineMediaStyle, that.inlineMediaStyle) &&
				EqualHelper.equal(this.hdMedia, that.hdMedia);
	}

	@Override
	public String getUiTitle () {
		return this.title != null && !this.title.isEmpty()
				? this.title
				: String.format("Column %s", this.id);
	}

	@Override
	public String toString () {
		final StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.id)
				.append(",").append(this.title)
				.append(",").append(this.feeds)
				.append(",").append(this.refreshIntervalMins)
				.append(",").append(this.excludeColumnIds)
				.append(",").append(this.hideRetweets)
				.append(",").append(this.notificationStyle)
				.append(",").append(this.inlineMediaStyle)
				.append(",").append(this.hdMedia)
				.append("}");
		return s.toString();
	}

	public int getId () {
		return this.id;
	}

	public String getTitle () {
		return this.title;
	}

	/**
	 * Does not return null.
	 */
	public Set<ColumnFeed> getFeeds () {
		if (this.feeds == null) return Collections.emptySet();
		return this.feeds;
	}

	public int getRefreshIntervalMins () {
		return this.refreshIntervalMins;
	}

	public Set<Integer> getExcludeColumnIds () {
		return this.excludeColumnIds;
	}

	public boolean isHideRetweets () {
		return this.hideRetweets;
	}

	public NotificationStyle getNotificationStyle () {
		return this.notificationStyle;
	}

	public InlineMediaStyle getInlineMediaStyle () {
		return this.inlineMediaStyle;
	}

	public boolean isHdMedia () {
		return this.hdMedia;
	}

	/**
	 * Does not return null.
	 */
	public Set<String> uniqAccountIds () {
		final Set<String> ret = new LinkedHashSet<String>();
		for (final ColumnFeed cf : getFeeds()) {
			if (!StringHelper.isEmpty(cf.getAccountId())) ret.add(cf.getAccountId());
		}
		return ret;
	}

	public static List<String> titles (final Collection<Column> columns) {
		if (columns == null) return null;
		final List<String> ret = new ArrayList<String>(columns.size());
		for (final Column col : columns) {
			ret.add(col.getTitle());
		}
		return ret;
	}

	/**
	 * Does not return null.
	 */
	public static Set<String> uniqAccountIds (final Collection<Column> cols) {
		if (cols == null || cols.size() < 1) return Collections.emptySet();
		final Set<String> ret = new LinkedHashSet<String>();
		for (final Column col : cols) {
			for (final ColumnFeed cf : col.getFeeds()) {
				if (!StringHelper.isEmpty(cf.getAccountId())) ret.add(cf.getAccountId());
			}
		}
		return ret;
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(KEY_ID, getId());
		json.put(KEY_TITLE, getTitle());

		final Set<ColumnFeed> fs = getFeeds();
		if (fs.size() == 1) {
			final ColumnFeed cf = fs.iterator().next();
			json.put(KEY_ACCOUNT, cf.getAccountId());
			json.put(KEY_RESOURCE, cf.getResource());
		}
		else if (fs.size() > 1) {
			final JSONArray feedsArr = new JSONArray();
			for (final ColumnFeed cf : fs) {
				feedsArr.put(cf.toJson());
			}
			json.put(KEY_FEEDS, feedsArr);
		}

		json.put(KEY_REFRESH, getRefreshIntervalMins() + "mins");
		json.put(KEY_EXCLUDE, toJsonArray(getExcludeColumnIds()));
		json.put(KEY_HIDE_RT, isHideRetweets());
		json.put(KEY_NOTIFY, getNotificationStyle() != null ? getNotificationStyle().toJson() : null);
		json.put(KEY_INLINE_MEDIA, getInlineMediaStyle() != null ? getInlineMediaStyle().serialise() : null);
		json.put(KEY_HD_MEDIA, isHdMedia());
		return json;
	}

	private static JSONArray toJsonArray (final Set<Integer> ints) {
		if (ints == null) return null;
		final JSONArray ja = new JSONArray();
		for (final Integer i : ints) {
			ja.put(i);
		}
		return ja;
	}

	public static Column parseJson (final String json) throws JSONException {
		if (json == null) return null;
		return parseJson((JSONObject) new JSONTokener(json).nextValue());
	}

	public static Column parseJson (final JSONObject json) throws JSONException {
		try {
			final int id = json.optInt(KEY_ID, Integer.MIN_VALUE);
			if (id < 0) throw new JSONException("Column ID must be positive a integer.");
			final String title = json.getString(KEY_TITLE);

			final Set<ColumnFeed> feeds = new LinkedHashSet<ColumnFeed>();
			if (json.has(KEY_RESOURCE)) {
				final String resource = json.getString(KEY_RESOURCE);
				final String account = json.has(KEY_ACCOUNT) ? json.getString(KEY_ACCOUNT) : null;
				feeds.add(new ColumnFeed(account, resource));
			}
			final JSONArray jFeeds = json.optJSONArray(KEY_FEEDS);
			if (jFeeds != null) {
				for (int i = 0; i < jFeeds.length(); i++) {
					feeds.add(ColumnFeed.parseJson(jFeeds.getJSONObject(i)));
				}
			}

			boolean hasAccount = false;
			for (final ColumnFeed cf : feeds) {
				if (cf.getAccountId() != null) {
					hasAccount = true;
					break;
				}
			}

			final String refreshRaw = json.optString(KEY_REFRESH, null);
			final int refreshIntervalMins = parseFeedRefreshInterval(refreshRaw, hasAccount, title);
			final Set<Integer> excludeColumnIds = parseFeedExcludeColumns(json, title);
			final boolean hideRetweets = json.optBoolean(KEY_HIDE_RT, false);
			final NotificationStyle notificationStyle = NotificationStyle.parseJson(json.opt(KEY_NOTIFY));
			final InlineMediaStyle inlineMedia = InlineMediaStyle.parseJson(json.opt(KEY_INLINE_MEDIA));
			final boolean hdMedia = json.optBoolean(KEY_HD_MEDIA, false);
			return new Column(id, title, feeds, refreshIntervalMins, excludeColumnIds, hideRetweets, notificationStyle, inlineMedia, hdMedia);
		}
		catch (final JSONException e) {
			throw new JSONException("Failed to parse column: " + json.toString() + " > " + ExcpetionHelper.causeTrace(e, " > "));
		}
	}

	private static int parseFeedRefreshInterval (final String refreshRaw, final boolean hasAccount, final String title) {
		final int refreshIntervalMins = TimeParser.parseDuration(refreshRaw);
		if (refreshIntervalMins < 0 && hasAccount) LOG.w("Column '%s' has invalid refresh interval: '%s'.", title, refreshRaw);
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

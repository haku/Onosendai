package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.Titleable;

public class ColumnFeed implements Titleable {

	private static final String KEY_ACCOUNT = "account";
	private static final String KEY_RESOURCE = "resource";

	private final String accountId;
	private final String resource;
	private final String title;

	public ColumnFeed (final String accountId, final String resource) {
		this(accountId, resource, resource);
	}

	public ColumnFeed (final String accountId, final String resource, final String title) {
		this.accountId = accountId;
		this.resource = resource;
		this.title = title;
	}

	public String getAccountId () {
		return this.accountId;
	}

	public String getResource () {
		return this.resource;
	}

	@Override
	public String getUiTitle () {
		return this.title;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.accountId == null) ? 0 : this.accountId.hashCode());
		result = prime * result + ((this.resource == null) ? 0 : this.resource.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof ColumnFeed)) return false;
		final ColumnFeed that = (ColumnFeed) o;
		return EqualHelper.equal(this.accountId, that.accountId) &&
				EqualHelper.equal(this.resource, that.resource);
	}

	@Override
	public String toString () {
		return new StringBuilder("Feed{").append(this.accountId)
				.append(",").append(this.resource)
				.append("}").toString();
	}

	public JSONObject toJson () throws JSONException {
		final JSONObject json = new JSONObject();
		json.put(KEY_ACCOUNT, getAccountId());
		json.put(KEY_RESOURCE, getResource());
		return json;
	}

	public static ColumnFeed parseJson (final JSONObject json) throws JSONException {
		final String account = json.has(KEY_ACCOUNT) ? json.getString(KEY_ACCOUNT) : null;
		final String resource = json.getString(KEY_RESOURCE);
		return new ColumnFeed(account, resource);
	}

	/**
	 * Does not return null.
	 */
	public static Set<String> uniqAccountIds (final Collection<ColumnFeed> feeds) {
		if (feeds == null || feeds.size() < 1) return Collections.emptySet();
		final Set<String> ret = new LinkedHashSet<String>();
		for (final ColumnFeed cf : feeds) {
			if (!StringHelper.isEmpty(cf.getAccountId())) ret.add(cf.getAccountId());
		}
		return ret;
	}

	public static List<ColumnFeed> mixInAccountTitles (final Collection<ColumnFeed> feeds, final Config conf) {
		final List<ColumnFeed> ret = new ArrayList<ColumnFeed>();
		for (final ColumnFeed cf : feeds) {
			final Account act = conf.getAccount(cf.getAccountId());
			final String title = (act != null && !StringHelper.isEmpty(act.getTitle()) ? act.getTitle() + " / " : "") + cf.getResource();
			ret.add(new ColumnFeed(cf.getAccountId(), cf.getResource(), title));
		}
		return ret;
	}

}

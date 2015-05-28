package com.vaguehope.onosendai.config;

import org.json.JSONException;
import org.json.JSONObject;

import com.vaguehope.onosendai.util.EqualHelper;

public class ColumnFeed {

	private static final String KEY_ACCOUNT = "account";
	private static final String KEY_RESOURCE = "resource";

	private final String accountId;
	private final String resource;

	public ColumnFeed (final String accountId, final String resource) {
		this.accountId = accountId;
		this.resource = resource;
	}

	public String getAccountId () {
		return this.accountId;
	}

	public String getResource () {
		return this.resource;
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

	public static ColumnFeed parseJson (final JSONObject json) throws JSONException {
		final String account = json.has(KEY_ACCOUNT) ? json.getString(KEY_ACCOUNT) : null;
		final String resource = json.getString(KEY_RESOURCE);
		return new ColumnFeed(account, resource);
	}

}

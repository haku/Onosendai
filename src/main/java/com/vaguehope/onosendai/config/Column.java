package com.vaguehope.onosendai.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vaguehope.onosendai.util.EqualHelper;

public class Column {

	private final int id;
	private final String title;
	private final String accountId;
	private final String resource;
	private final int refreshIntervalMins;

	public Column (final int id, final String title, final String accountId, final String resource, final int refreshIntervalMins) {
		this.id = id;
		this.title = title;
		this.accountId = accountId;
		this.resource = resource;
		this.refreshIntervalMins = refreshIntervalMins;
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

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.id)
				.append(",").append(this.title)
				.append(",").append(this.accountId)
				.append(",").append(this.resource)
				.append(",").append(this.refreshIntervalMins)
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

	public static List<String> titles (final Collection<Column> columns) {
		if (columns == null) return null;
		List<String> ret = new ArrayList<String>(columns.size());
		for (Column col : columns) {
			ret.add(col.getTitle());
		}
		return ret;
	}

}

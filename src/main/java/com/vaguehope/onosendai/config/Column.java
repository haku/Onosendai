package com.vaguehope.onosendai.config;

import com.vaguehope.onosendai.util.EqualHelper;

public class Column {

	public final int id;
	public final String title;
	public final String accountId;
	public final String resource;
	public final String refresh;

	public Column (final int id, final String title, final String accountId, final String resource, final String refresh) {
		this.id = id;
		this.title = title;
		this.accountId = accountId;
		this.resource = resource;
		this.refresh = refresh;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.accountId == null) ? 0 : this.accountId.hashCode());
		result = prime * result + this.id;
		result = prime * result + ((this.refresh == null) ? 0 : this.refresh.hashCode());
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
				EqualHelper.equal(this.refresh, that.refresh);
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.id)
				.append(",").append(this.title)
				.append(",").append(this.accountId)
				.append(",").append(this.resource)
				.append(",").append(this.refresh)
				.append("}");
		return s.toString();
	}

}

package com.vaguehope.onosendai.config;

import com.vaguehope.onosendai.util.EqualHelper;

public class Column {

	private final int id;
	private final String title;
	private final String accountId;
	private final String resource;
	private final String refresh;

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
		result = prime * result + ((this.getAccountId() == null) ? 0 : this.getAccountId().hashCode());
		result = prime * result + this.getId();
		result = prime * result + ((this.getRefresh() == null) ? 0 : this.getRefresh().hashCode());
		result = prime * result + ((this.getResource() == null) ? 0 : this.getResource().hashCode());
		result = prime * result + ((this.getTitle() == null) ? 0 : this.getTitle().hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Column)) return false;
		Column that = (Column) o;
		return EqualHelper.equal(this.getId(), that.getId()) &&
				EqualHelper.equal(this.getTitle(), that.getTitle()) &&
				EqualHelper.equal(this.getAccountId(), that.getAccountId()) &&
				EqualHelper.equal(this.getResource(), that.getResource()) &&
				EqualHelper.equal(this.getRefresh(), that.getRefresh());
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.getId())
				.append(",").append(this.getTitle())
				.append(",").append(this.getAccountId())
				.append(",").append(this.getResource())
				.append(",").append(this.getRefresh())
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

	public String getRefresh () {
		return this.refresh;
	}

}

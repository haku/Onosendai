package com.vaguehope.onosendai.config;

public class Column {

	public final int index;
	public final String title;
	public final String accountId;
	public final String resource;
	public final String refresh;

	public Column (final int index, final String title, final String accountId, final String resource, final String refresh) {
		this.index = index;
		this.title = title;
		this.accountId = accountId;
		this.resource = resource;
		this.refresh = refresh;
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Column{").append(this.index)
				.append(",").append(this.title)
				.append(",").append(this.accountId)
				.append(",").append(this.resource)
				.append(",").append(this.refresh)
				.append("}");
		return s.toString();
	}

}
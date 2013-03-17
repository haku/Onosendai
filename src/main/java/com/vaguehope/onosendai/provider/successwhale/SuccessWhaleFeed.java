package com.vaguehope.onosendai.provider.successwhale;

import com.vaguehope.onosendai.config.Column;

public class SuccessWhaleFeed {

	private final Column column;

	public SuccessWhaleFeed (final Column column) {
		// TODO Validate resource.  Convert from a nicer format?  Throw if not valid.
		this.column = column;
	}

	public String getSources () {
		return this.column.resource;
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("SWFeed{c{").append(this.column.id)
				.append(",").append(this.column.title)
				.append(",").append(this.column.accountId)
				.append("}}").toString();
	}

}

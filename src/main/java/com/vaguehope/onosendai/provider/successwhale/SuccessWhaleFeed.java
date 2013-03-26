package com.vaguehope.onosendai.provider.successwhale;

import com.vaguehope.onosendai.config.Column;

public class SuccessWhaleFeed {

	private final Column column;

	public SuccessWhaleFeed (final Column column) {
		// TODO Validate resource.  Convert from a nicer format?  Throw if not valid.
		this.column = column;
	}

	public String getSources () {
		return this.column.getResource();
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("SWFeed{c{").append(this.column.getId())
				.append(",").append(this.column.getTitle())
				.append(",").append(this.column.getAccountId())
				.append("}}").toString();
	}

}

package com.vaguehope.onosendai.provider.successwhale;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;

public class SuccessWhaleFeed {

	private final Column column;
	private final ColumnFeed columnFeed;

	public SuccessWhaleFeed (final Column column, final ColumnFeed columnFeed) {
		// TODO Validate resource.  Convert from a nicer format?  Throw if not valid.
		this.column = column;
		this.columnFeed = columnFeed;
	}

	public String getSources () {
		return this.columnFeed.getResource();
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("SWFeed{c{").append(this.column.getId())
				.append(",").append(this.column.getTitle())
				.append(",").append(this.columnFeed.getAccountId())
				.append(",").append(this.columnFeed.getResource())
				.append("}}").toString();
	}

}

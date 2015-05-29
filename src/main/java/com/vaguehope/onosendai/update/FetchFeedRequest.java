package com.vaguehope.onosendai.update;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.Titleable;

class FetchFeedRequest implements Titleable {

	public final Column column;
	public final ColumnFeed feed;
	public final Account account;

	public FetchFeedRequest (final Column column, final ColumnFeed feed, final Account account) {
		this.column = column;
		this.feed = feed;
		this.account = account;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.column == null) ? 0 : this.column.hashCode());
		result = prime * result + ((this.feed == null) ? 0 : this.feed.hashCode());
		result = prime * result + ((this.account == null) ? 0 : this.account.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof FetchFeedRequest)) return false;
		final FetchFeedRequest that = (FetchFeedRequest) o;
		return EqualHelper.equal(this.column, that.column) &&
				EqualHelper.equal(this.feed, that.feed) &&
				EqualHelper.equal(this.account, that.account);
	}

	@Override
	public String toString () {
		return new StringBuilder("FetchFeedRequest{").append(this.column != null ? this.column.getId() : "-")
				.append(",").append(this.feed)
				.append(",").append(this.account != null ? this.account.getId() : "-")
				.append("}").toString();
	}

	@Override
	public String getUiTitle () {
		return String.format("%s:%s", this.column != null ? this.column.getTitle() : "-", this.feed);
	}

}

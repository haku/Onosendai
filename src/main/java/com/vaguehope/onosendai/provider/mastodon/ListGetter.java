package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.MastodonLists;
import com.vaguehope.onosendai.model.SinceIdType;

public class ListGetter implements MastodonFeedGetter {

	private final long listId;

	private MastodonLists lists;

	public ListGetter (final long listId) {
		this.listId = listId;
	}

	@Override
	public void setClient (final MastodonClient client) {
		this.lists = new MastodonLists(client);
	}

	@Override
	public GetterResponse<?> makeRequest (final Range range, final boolean manualRefresh) throws Mastodon4jRequestException {
		if (this.lists == null) throw new IllegalStateException("setClient() not called.");
		final Pageable<Status> pageable = this.lists.getListTimeLine(this.listId, range).execute();
		return new GetterResponse.StatusGetterResponse(pageable);
	}

	@Override
	public SinceIdType getSinceIdType () {
		return SinceIdType.SID;
	}

	@Override
	public String toString () {
		return "ListGetter(" + this.listId + ")";
	}

}

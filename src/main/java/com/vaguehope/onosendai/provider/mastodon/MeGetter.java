package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Accounts;
import com.vaguehope.onosendai.model.SinceIdType;

public class MeGetter implements MastodonFeedGetter {

	private final long myId;

	private Accounts accounts;

	public MeGetter (final long myId) {
		this.myId = myId;
	}

	@Override
	public void setClient (final MastodonClient client) {
		this.accounts = new Accounts(client);
	}

	@Override
	public GetterResponse<?> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.accounts == null) throw new IllegalStateException("setClient() not called.");
		final Pageable<Status> pageable = this.accounts.getStatuses(
				this.myId,
				/* onlyMedia= */false,
				/* excludeReplies= */false,
				/* pinned= */false,
				range).execute();
		return new GetterResponse.StatusGetterResponse(pageable);
	}

	@Override
	public SinceIdType getSinceIdType () {
		return SinceIdType.SID;
	}

	@Override
	public String toString () {
		return "MeGetter(" + this.myId + ")";
	}

}

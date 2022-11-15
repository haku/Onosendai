package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Accounts;

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
	public MastodonRequest<Pageable<Status>> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.accounts == null) throw new IllegalStateException("setClient() not called.");
		return this.accounts.getStatuses(
				this.myId,
				/* onlyMedia= */false,
				/* excludeReplies= */false,
				/* pinned= */false,
				range);
	}

}

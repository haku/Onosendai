package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Timelines;

public class TimelineGetter implements MastodonFeedGetter {

	private Timelines timelines;

	@Override
	public void setClient (final MastodonClient client) {
		this.timelines = new Timelines(client);
	}

	@Override
	public MastodonRequest<Pageable<Status>> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.timelines == null) throw new IllegalStateException("setClient() not called.");
		return this.timelines.getHome(range);
	}

}

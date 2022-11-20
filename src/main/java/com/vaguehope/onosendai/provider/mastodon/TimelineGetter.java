package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Timelines;
import com.vaguehope.onosendai.model.SinceIdType;

public class TimelineGetter implements MastodonFeedGetter {

	private Timelines timelines;

	@Override
	public void setClient (final MastodonClient client) {
		this.timelines = new Timelines(client);
	}

	@Override
	public GetterResponse<?> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.timelines == null) throw new IllegalStateException("setClient() not called.");
		final Pageable<Status> pageable = this.timelines.getHome(range).execute();
		return new GetterResponse.StatusGetterResponse(pageable);
	}

	@Override
	public SinceIdType getSinceIdType () {
		return SinceIdType.SID;
	}

	@Override
	public String toString () {
		return "TimelineGetter()";
	}

}

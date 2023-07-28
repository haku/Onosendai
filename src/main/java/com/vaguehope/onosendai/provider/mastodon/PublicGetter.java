package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Public;
import com.vaguehope.onosendai.model.SinceIdType;

public class PublicGetter implements MastodonFeedGetter {

	public static enum PublicType {
		INSTANCE_LOCAL,
		FEDERATED,
	}

	private final PublicType publicType;
	private Public public_;

	public PublicGetter (final PublicType publicType) {
		this.publicType = publicType;
	}

	@Override
	public void setClient (final MastodonClient client) {
		this.public_ = new Public(client);
	}

	@Override
	public GetterResponse<?> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.public_ == null) throw new IllegalStateException("setClient() not called.");

		final Pageable<Status> pageable;
		switch (this.publicType) {
			case INSTANCE_LOCAL:
				pageable = this.public_.getLocalPublic(range).execute();
				break;
			case FEDERATED:
				pageable = this.public_.getFederatedPublic(range).execute();
				break;
			default:
				throw new IllegalArgumentException("Do not know how to fetch: " + this.publicType);
		}

		return new GetterResponse.StatusGetterResponse(pageable);
	}

	@Override
	public SinceIdType getSinceIdType () {
		return SinceIdType.SID;
	}

	@Override
	public String toString () {
		return "PublicGetter()";
	}

}

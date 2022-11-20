package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.vaguehope.onosendai.model.SinceIdType;

public interface MastodonFeedGetter {

	void setClient (MastodonClient client);

	GetterResponse<?> makeRequest (Range range) throws Mastodon4jRequestException;

	SinceIdType getSinceIdType();

}

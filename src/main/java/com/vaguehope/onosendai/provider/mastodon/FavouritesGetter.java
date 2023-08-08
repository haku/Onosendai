package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Favourites;
import com.vaguehope.onosendai.model.SinceIdType;

public class FavouritesGetter implements MastodonFeedGetter {

	private Favourites favourites;

	@Override
	public void setClient (final MastodonClient client) {
		this.favourites = new Favourites(client);
	}

	@Override
	public GetterResponse<?> makeRequest (final Range range, final boolean manualRefresh) throws Mastodon4jRequestException {
		if (this.favourites == null) throw new IllegalStateException("setClient() not called.");
		final Range r = manualRefresh ? null : range;
		final Pageable<Status> pageable = this.favourites.getFavourites(r).execute();
		return new GetterResponse.StatusGetterResponse(pageable);
	}

	@Override
	public SinceIdType getSinceIdType () {
		return SinceIdType.SID;
	}

	@Override
	public String toString () {
		return "FavouritesGetter()";
	}

}

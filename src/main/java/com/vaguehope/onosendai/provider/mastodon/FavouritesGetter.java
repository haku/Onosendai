package com.vaguehope.onosendai.provider.mastodon;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Favourites;

public class FavouritesGetter implements MastodonFeedGetter {

	private Favourites favourites;

	@Override
	public void setClient (final MastodonClient client) {
		this.favourites = new Favourites(client);
	}

	@Override
	public MastodonRequest<Pageable<Status>> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.favourites == null) throw new IllegalStateException("setClient() not called.");
		return this.favourites.getFavourites(range);
	}

	@Override
	public String toString () {
		return "FavouritesGetter()";
	}

}

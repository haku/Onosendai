package com.vaguehope.onosendai.provider;

import com.vaguehope.onosendai.provider.hosaka.HosakaProvider;
import com.vaguehope.onosendai.provider.mastodon.MastodonProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.KvStore;

public class ProviderMgr {

	public interface ProviderMgrProvider {
		ProviderMgr getProviderMgr();
	}

	private final TwitterProvider twitterProvider;
	private final MastodonProvider mastodonProvider;
	private final HosakaProvider hosakaProvider;

	public ProviderMgr (final KvStore kvStore) {
		this.twitterProvider = new TwitterProvider();
		this.mastodonProvider = new MastodonProvider();
		this.hosakaProvider = new HosakaProvider();
	}

	public TwitterProvider getTwitterProvider () {
		return this.twitterProvider;
	}

	public MastodonProvider getMastodonProvider () {
		return this.mastodonProvider;
	}

	public HosakaProvider getHosakaProvider () {
		return this.hosakaProvider;
	}

	/**
	 * TODO: tidy this up.
	 */
	public void shutdown () {
		try {
			this.twitterProvider.shutdown();
		}
		finally {
			this.hosakaProvider.shutdown();
		}
	}

}

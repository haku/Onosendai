package com.vaguehope.onosendai.provider;

import com.vaguehope.onosendai.provider.mastodon.MastodonProvider;
import com.vaguehope.onosendai.storage.KvStore;

public class ProviderMgr {

	public interface ProviderMgrProvider {
		ProviderMgr getProviderMgr();
	}

	private final MastodonProvider mastodonProvider;

	public ProviderMgr (final KvStore kvStore) {
		this.mastodonProvider = new MastodonProvider();
	}

	public MastodonProvider getMastodonProvider () {
		return this.mastodonProvider;
	}

	/**
	 * TODO: tidy this up.
	 */
	public void shutdown () {
	}

}

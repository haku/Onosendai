package com.vaguehope.onosendai.provider;

import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;

public class ProviderMgr {

	private final TwitterProvider twitterProvider;
	private final SuccessWhaleProvider successWhaleProvider;

	public ProviderMgr () {
		this.twitterProvider = new TwitterProvider();
		this.successWhaleProvider = new SuccessWhaleProvider();
	}

	public TwitterProvider getTwitterProvider () {
		return this.twitterProvider;
	}

	public SuccessWhaleProvider getSuccessWhaleProvider () {
		return this.successWhaleProvider;
	}

	public void shutdown () {
		this.twitterProvider.shutdown();
		this.successWhaleProvider.shutdown();
	}

}

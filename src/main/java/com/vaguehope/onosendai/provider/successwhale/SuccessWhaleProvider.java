package com.vaguehope.onosendai.provider.successwhale;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.TweetList;

public class SuccessWhaleProvider {

	private final ConcurrentMap<String, SuccessWhale> accounts;
	private final HttpClientFactory httpClientFactory;

	public SuccessWhaleProvider () {
		this.accounts = new ConcurrentHashMap<String, SuccessWhale>();
		this.httpClientFactory = new HttpClientFactory();
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.id)) return;
		SuccessWhale s = new SuccessWhale(account, this.httpClientFactory);
		this.accounts.putIfAbsent(account.id, s);
	}

	public TweetList getTweets (final SuccessWhaleFeed feed, final Account account) throws SuccessWhaleException {
		SuccessWhale sw = this.accounts.get(account.id);
		if (sw == null) throw new IllegalStateException("Account not configured: '" + account.id + "'.");
		return fetchSuccessWhaleFeed(sw, feed);
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

	private static TweetList fetchSuccessWhaleFeed (final SuccessWhale sw, final SuccessWhaleFeed feed) throws SuccessWhaleException {
		// TODO paging, etc.
		return sw.getFeed(feed);
	}

}

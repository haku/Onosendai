package com.vaguehope.onosendai.provider.successwhale;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.storage.KvStore;
import com.vaguehope.onosendai.util.HttpClientFactory;
import com.vaguehope.onosendai.util.ImageMetadata;

public class SuccessWhaleProvider {

	private static final String TS_PATH = "/successwhale.bks";
	private static final String TS_PASSWORD = "123456";

	private final KvStore kvStore;
	private final ConcurrentMap<String, SuccessWhale> accounts;
	private final HttpClientFactory httpClientFactory;

	public SuccessWhaleProvider (final KvStore kvStore) {
		if (kvStore == null) throw new IllegalArgumentException("kvStore can not be null.");
		this.kvStore = kvStore;
		this.accounts = new ConcurrentHashMap<String, SuccessWhale>();
		this.httpClientFactory = new HttpClientFactory(TS_PATH, TS_PASSWORD);
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.getId())) return;
		SuccessWhale s = new SuccessWhale(this.kvStore, account, this.httpClientFactory);
		this.accounts.putIfAbsent(account.getId(), s);
	}

	private SuccessWhale getAccount (final Account account) {
		final SuccessWhale a = this.accounts.get(account.getId());
		if (a != null) return a;
		addAccount(account);
		return this.accounts.get(account.getId());
	}

	public SuccessWhaleColumns getColumns (final Account account) throws SuccessWhaleException {
		return getAccount(account).getColumns();
	}

	public SuccessWhaleSources getSources (final Account account) throws SuccessWhaleException {
		return getAccount(account).getSources();
	}

	public TweetList getTweets (final SuccessWhaleFeed feed, final Account account) throws SuccessWhaleException {
		return fetchSuccessWhaleFeed(getAccount(account), feed);
	}

	/**
	 *
	 * @param serviceTypeAndUid
	 *            colon separated, e.g. twitter:1234567890
	 */
	public TweetList getThread (final Account account, final String serviceTypeAndUid, final String forSid) throws SuccessWhaleException {
		final int x = serviceTypeAndUid.indexOf(':');
		if (x < 0) throw new IllegalArgumentException("serviceTypeAndUid must contain a colon: '" + serviceTypeAndUid + "'");
		final String type = serviceTypeAndUid.substring(0, x);
		final String uid = serviceTypeAndUid.substring(x + 1);
		return getAccount(account).getThread(type, uid, forSid);
	}

	public List<ServiceRef> getPostToAccounts (final Account account) throws SuccessWhaleException {
		return getAccount(account).getPostToAccounts();
	}

	public List<ServiceRef> getPostToAccountsCached (final Account account) {
		return getAccount(account).getPostToAccountsCached();
	}

	public void post (final Account account, final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid, final ImageMetadata image) throws SuccessWhaleException {
		getAccount(account).post(postToSvc, body, inReplyToSid, image);
	}

	public void itemAction (final Account account, final ServiceRef svc, final String itemSid, final ItemAction itemAction) throws SuccessWhaleException {
		getAccount(account).itemAction(svc, itemSid, itemAction);
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

	private static TweetList fetchSuccessWhaleFeed (final SuccessWhale sw, final SuccessWhaleFeed feed) throws SuccessWhaleException {
		// TODO paging, etc.
		return sw.getFeed(feed);
	}

}

package com.vaguehope.onosendai.provider.mastodon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.MastodonList;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Accounts;
import com.sys1yagi.mastodon4j.api.method.MastodonLists;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

public class MastodonProvider {

	private static final LogWrapper LOG = new LogWrapper("MP");

	private final ConcurrentMap<String, MastodonClient> accounts = new ConcurrentHashMap<String, MastodonClient>();

	private final Lock getOwnIdLock = new ReentrantLock();
	private final Map<String, Long> ownIds = new ConcurrentHashMap<String, Long>();

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.getId())) return;
		final MastodonClient mc = makeMastodonClient(account);
		this.accounts.putIfAbsent(account.getId(), mc);
	}

	private MastodonClient getAccount (final Account account) {
		final MastodonClient a = this.accounts.get(account.getId());
		if (a != null) return a;
		addAccount(account);
		return this.accounts.get(account.getId());
	}

	public void testAccountLogin (final Account account) throws Mastodon4jRequestException {
		final MastodonClient client = getAccount(account);
		final Accounts accounts = new Accounts(client);
		com.sys1yagi.mastodon4j.api.entity.Account myAccount = accounts.getVerifyCredentials().execute();
		LOG.i("Access test: Id %s is %s.", myAccount.getId(), myAccount.getAcct());
	}

	public long getOwnId (final Account account) throws Mastodon4jRequestException {
		this.getOwnIdLock.lock();
		try {
			final Long cached = this.ownIds.get(account.getId());
			if (cached != null) return cached.longValue();

			final MastodonClient client = getAccount(account);
			final Accounts accounts = new Accounts(client);
			long ownId = accounts.getVerifyCredentials().execute().getId();

			this.ownIds.put(account.getId(), Long.valueOf(ownId));
			return ownId;
		}
		finally {
			this.getOwnIdLock.unlock();
		}
	}

	public TweetList getFeed (final String resource, final Account account, final long sinceId) throws Mastodon4jRequestException {
		final MastodonColumnType type = MastodonColumnType.parseResource(resource);
		if (type == null) throw new IllegalArgumentException("Unknown resource: " + resource);

		switch (type) {
			case TIMELINE:
				return getFeed(account, new TimelineGetter(), sinceId, null);
			case LIST:
				final long listId = Long.parseLong(resource.substring(MastodonColumnType.LIST.getResource().length()));
				return getFeed(account, new ListGetter(listId), sinceId, null);
			case ME:
				return getFeed(account, new MeGetter(getOwnId(account)), sinceId, null);
			case FAVORITES:
				return getFeed(account, new FavouritesGetter(), sinceId, null);
			default:
				throw new IllegalArgumentException("Do not know how to fetch: " + type);
		}
	}

	public TweetList getFeed (final Account account, final MastodonFeedGetter getter, final Long sinceId, final Collection<Meta> extraMetas) throws Mastodon4jRequestException {
		final long ownId = getOwnId(account);

		final MastodonClient client = getAccount(account);
		getter.setClient(client);

		final List<Tweet> tweets = new ArrayList<Tweet>();
		final List<Tweet> quotedTweets = new ArrayList<Tweet>();

		final int pageLimit = 40; // TODO make constant.
		final int fetchLimit = 120; // TODO make constant.

		int page = 1; // First page is 1.
		Range range = new Range(null, sinceId, pageLimit);
		while (tweets.size() < fetchLimit) {
			final Pageable<Status> pageable = getter.makeRequest(range).execute();
			final List<Status> timelinePage = pageable.getPart();
			LOG.i("Page %d of Home(sinceId=%s) contains %d items.", page, sinceId, timelinePage.size());
			if (timelinePage.size() < 1) break;

			for (final Status status : timelinePage) {
				tweets.add(MastodonUtils.convertStatusToTweet(account, status, ownId, extraMetas));
			}

			range = pageable.nextRange(pageLimit);
			page++;
		}

		return new TweetList(tweets, quotedTweets);
	}

	public List<MastodonList> getLists(final Account account) throws Mastodon4jRequestException {
		final MastodonClient client = getAccount(account);
		final MastodonLists lists = new MastodonLists(client);
		final Pageable<MastodonList> pageable = lists.getLists().execute();
		return pageable.getPart();
	}

	private MastodonClient makeMastodonClient (final Account account) {
		final String instanceName = account.getConsumerKey();
		final String accessToken = account.getAccessToken();

		if (StringHelper.isEmpty(instanceName)) throw new IllegalArgumentException(String.format(
				"Account %s missind instanceName.", account.getId()));
		if (StringHelper.isEmpty(accessToken)) throw new IllegalArgumentException(String.format(
				"Account %s missind accessToken.", account.getId()));

		return MastodonAuth.makeMastodonClientBuilder(instanceName)
				.accessToken(accessToken)
				.build();
	}

}

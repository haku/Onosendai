package com.vaguehope.onosendai.update;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import twitter4j.TwitterException;

import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.model.Filters;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.instapaper.InstapaperProvider;
import com.vaguehope.onosendai.provider.mastodon.MastodonProvider;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleFeed;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterFeed;
import com.vaguehope.onosendai.provider.twitter.TwitterFeeds;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterUtils;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.ColumnState;
import com.vaguehope.onosendai.storage.DbInterface.DiscardOrder;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchColumn implements Callable<Void> {

	protected static final LogWrapper LOG = new LogWrapper("FC");

	private final DbInterface db;
	private final FetchFeedRequest ffr;
	private final ProviderMgr providerMgr;
	private final Filters filters;
	private final boolean manual;

	public FetchColumn (final DbInterface db, final FetchFeedRequest ffr, final ProviderMgr providerMgr, final Filters filters, final boolean manual) {
		if (db == null) throw new IllegalArgumentException("db can not be null.");
		this.db = db;
		if (ffr == null) throw new IllegalArgumentException("ffr can not be null.");
		if (ffr.column == null) throw new IllegalArgumentException("ffr.column can not be null.");
		if (ffr.account == null) throw new IllegalArgumentException("ffr.account can not be null.");
		this.ffr = ffr;
		if (providerMgr == null) throw new IllegalArgumentException("providerMgr can not be null.");
		this.providerMgr = providerMgr;
		this.filters = filters;
		this.manual = manual;
	}

	@Override
	public Void call () {
		fetchColumn(this.db, this.ffr, this.providerMgr, this.filters, this.manual);
		return null;
	}

	public static void fetchColumn (final DbInterface db, final FetchFeedRequest ffr, final ProviderMgr providerMgr, final Filters filters, final boolean manual) {
		db.notifyTwListenersColumnState(ffr.column.getId(), ColumnState.UPDATE_RUNNING);
		try {
			fetchColumnInner(db, ffr, providerMgr, filters, manual);
		}
		finally {
			db.notifyTwListenersColumnState(ffr.column.getId(), ColumnState.UPDATE_OVER);
		}
	}

	private static void fetchColumnInner (final DbInterface db, final FetchFeedRequest ffr, final ProviderMgr providerMgr, final Filters filters, final boolean manual) {
		switch (ffr.account.getProvider()) {
			case TWITTER:
				fetchTwitterColumn(db, ffr.account, ffr.column, ffr.feed, providerMgr, filters, manual);
				break;
			case MASTODON:
				fetchMastodonColumn(db, ffr.account, ffr.column, ffr.feed, providerMgr, filters);
				break;
			case SUCCESSWHALE:
				fetchSuccessWhaleColumn(db, ffr.account, ffr.column, ffr.feed, providerMgr, filters);
				break;
			case INSTAPAPER:
				pushInstapaperColumn(db, ffr.account, ffr.column, providerMgr);
				break;
			default:
				LOG.e("Unknown account type: %s", ffr.account.getProvider());
		}
	}

	private static void fetchTwitterColumn (final DbInterface db, final Account account, final Column column, final ColumnFeed columnFeed, final ProviderMgr providerMgr, final Filters filters, final boolean manual) {
		final long startTime = System.nanoTime();
		try {
			final TwitterProvider twitterProvider = providerMgr.getTwitterProvider();
			twitterProvider.addAccount(account);
			final TwitterFeed feed = TwitterFeeds.parse(columnFeed.getResource());
			final String sinceIdRaw = readSinceId(db, column, columnFeed);
			final long sinceId = sinceIdRaw != null ? Long.parseLong(sinceIdRaw) : -1;
			final TweetList tweets = twitterProvider.getTweets(feed, account, sinceId, column.isHdMedia(), manual);
			final int filteredCount = filterAndStore(db, column, columnFeed, filters, tweets);
			storeQuoted(db, tweets);
			storeSuccess(db, column);
			final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
			LOG.i("Fetched %d items for '%s' '%s' in %d millis.  %s filtered.", tweets.count(), column.getTitle(), columnFeed.getResource(), durationMillis, filteredCount);
		}
		catch (final TwitterException e) {
			LOG.w("Failed to fetch from Twitter: %s", ExcpetionHelper.causeTrace(e));
			storeError(db, column, TwitterUtils.friendlyExceptionMessage(e));
		}
	}

	private static void fetchMastodonColumn (final DbInterface db, final Account account, final Column column, final ColumnFeed columnFeed, final ProviderMgr providerMgr, final Filters filters) {
		final long startTime = System.nanoTime();
		try {
			final MastodonProvider mastodonProvider = providerMgr.getMastodonProvider();
			mastodonProvider.addAccount(account);
			final String sinceIdRaw = readSinceId(db, column, columnFeed);
			final Long sinceId = sinceIdRaw != null ? Long.parseLong(sinceIdRaw) : null;
			final TweetList tweets = mastodonProvider.getFeed(columnFeed.getResource(), account, sinceId);
			final int filteredCount = filterAndStore(db, column, columnFeed, filters, tweets);
			storeQuoted(db, tweets);
			storeSuccess(db, column);
			final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
			LOG.i("Fetched %d items for '%s' '%s' in %d millis.  %s filtered.", tweets.count(), column.getTitle(), columnFeed.getResource(), durationMillis, filteredCount);
		}
		catch (final Mastodon4jRequestException e) {
			LOG.w("Failed to fetch from Mastodon: %s", ExcpetionHelper.causeTrace(e));
			storeError(db, column, ExcpetionHelper.causeTrace(e)); // TODO make friendly.
		}
	}

	private static void fetchSuccessWhaleColumn (final DbInterface db, final Account account, final Column column, final ColumnFeed columnFeed, final ProviderMgr providerMgr, final Filters filters) {
		final long startTime = System.nanoTime();
		try {
			final SuccessWhaleProvider successWhaleProvider = providerMgr.getSuccessWhaleProvider();
			successWhaleProvider.addAccount(account);
			final SuccessWhaleFeed feed = new SuccessWhaleFeed(column, columnFeed);
			final String sinceId = readSinceId(db, column, columnFeed);
			final TweetList tweets = successWhaleProvider.getTweets(feed, account, sinceId);
			final int filteredCount = filterAndStore(db, column, columnFeed, filters, tweets);
			storeSuccess(db, column);
			final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
			LOG.i("Fetched %d items for '%s' '%s' in %d millis.  %s filtered.", tweets.count(), column.getTitle(), columnFeed.getResource(), durationMillis, filteredCount);
		}
		catch (final SuccessWhaleException e) {
			LOG.w("Failed to fetch from SuccessWhale: %s", ExcpetionHelper.causeTrace(e));
			storeError(db, column, e.friendlyMessage());
		}
	}

	private static String readSinceId (final DbInterface db, final Column column, final ColumnFeed columnFeed) {
		return db.getValue(KvKeys.feedSinceId(column, columnFeed));
	}

	private static int filterAndStore (final DbInterface db, final Column column, final ColumnFeed columnFeed, final Filters filters, final TweetList tweets) {
		int filteredCount = 0;
		if (tweets.count() > 0) {
			final List<Tweet> filteredTweets = filters.matchAndSet(tweets.getTweets());
			filteredCount = Filters.countFiltered(filteredTweets);
			db.storeTweets(column, filteredTweets, DiscardOrder.FIRST_PUBLISHED);
			db.storeValue(KvKeys.feedSinceId(column, columnFeed), tweets.getSinceId());
		}
		return filteredCount;
	}

	/**
	 * Put the quoted tweets in the cache column, for want of a better place to put them.
	 */
	private static void storeQuoted (final DbInterface db, final TweetList tweets) {
		db.storeTweets(Column.ID_CACHED, tweets.getQuotedTweets(), DiscardOrder.FIRST_DOWNLOADED);
	}

	private static void pushInstapaperColumn (final DbInterface db, final Account account, final Column column, final ProviderMgr providerMgr) {
		final long startTime = System.nanoTime();
		try {
			final InstapaperProvider provider = providerMgr.getInstapaperProvider();

			final String lastPushTimeRaw = db.getValue(KvKeys.colLastPushTime(column));
			final long lastPushTime = lastPushTimeRaw != null ? Long.parseLong(lastPushTimeRaw) : 0L;

			LOG.i("Looking for items since t=%s to push...", lastPushTime);
			final List<Tweet> tweets = db.getTweetsSinceTime(column.getId(), lastPushTime, 10); // XXX Arbitrary limit.

			LOG.i("Pushing %s items...", tweets.size());
			for (final Tweet tweet : tweets) {
				final Tweet fullTweet = db.getTweetDetails(column.getId(), tweet);
				provider.add(account, fullTweet);
				db.storeValue(KvKeys.colLastPushTime(column), String.valueOf(tweet.getTime()));
				LOG.i("Pushed item sid=%s.", tweet.getSid());
			}

			storeSuccess(db, column);
			final long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
			LOG.i("Pushed %d items for '%s' in %d millis.", tweets.size(), column.getTitle(), durationMillis);
		}
		catch (final Exception e) {
			LOG.w("Failed to push to Instapaper: %s", ExcpetionHelper.causeTrace(e));
			storeError(db, column, ExcpetionHelper.causeTrace(e));
		}
	}

	private static void storeSuccess (final DbInterface db, final Column column) {
		storeResult(db, column, null);
	}

	private static void storeError (final DbInterface db, final Column column, final String msg) {
		storeResult(db, column, msg);
	}

	public static void storeDismiss (final DbInterface db, final Column column) {
		storeResult(db, column, null);
	}

	private static void storeResult (final DbInterface db, final Column column, final String result) {
		db.storeValue(KvKeys.colLastRefreshError(column), result);
	}

}

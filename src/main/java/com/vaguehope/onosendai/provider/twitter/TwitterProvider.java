package com.vaguehope.onosendai.provider.twitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.conf.ConfigurationBuilder;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.LogWrapper;

public class TwitterProvider {

	private static final LogWrapper LOG = new LogWrapper("TP");

	private final ConcurrentMap<String, Twitter> accounts;

	public TwitterProvider () {
		this.accounts = new ConcurrentHashMap<String, Twitter>();
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.id)) return;
		TwitterFactory tf = makeTwitterFactory(account);
		Twitter t = tf.getInstance();
		this.accounts.putIfAbsent(account.id, t);
	}

	public TweetList getTweets (final TwitterFeed feed, final Account account, final long sinceId) throws TwitterException {
		Twitter t = this.accounts.get(account.id);
		if (t == null) throw new IllegalStateException("Account not configured: '" + account.id + "'.");
		return fetchTwitterFeed(t, feed, sinceId);
	}

	public void shutdown () {
		Iterator<Twitter> itr = this.accounts.values().iterator();
		while (itr.hasNext()) {
			Twitter t = itr.next();
			t.shutdown();
			itr.remove();
		}
	}

	private static TwitterFactory makeTwitterFactory (final Account account) {
		ConfigurationBuilder cb = new ConfigurationBuilder()
				.setOAuthConsumerKey(account.consumerKey)
				.setOAuthConsumerSecret(account.consumerSecret)
				.setOAuthAccessToken(account.accessToken)
				.setOAuthAccessTokenSecret(account.accessSecret);
		return new TwitterFactory(cb.build());
	}

	/*
	 * Paging: https://dev.twitter.com/docs/working-with-timelines
	 * http://twitter4j.org/en/code-examples.html
	 */

	private static TweetList fetchTwitterFeed (final Twitter t, final TwitterFeed feed, final long sinceId) throws TwitterException {
		List<Tweet> tweets = new ArrayList<Tweet>();
		int minCount = feed.recommendedFetchCount();
		int pageSize = Math.min(minCount, C.TWEET_FETCH_PAGE_SIZE);
		int page = 1; // First page is 1.
		long minId = -1;
		while (tweets.size() < minCount) {
			Paging paging = new Paging(page, pageSize);
			if (sinceId > 0) paging.setSinceId(sinceId);
			if (minId > 0) paging.setMaxId(minId);
			ResponseList<Status> timelinePage = feed.getTweets(t, paging);
			if (timelinePage.size() < 1) break;
			addTweetsToList(tweets, timelinePage);
			if (timelinePage.size() < pageSize) break;
			minId = minIdOf(minId, timelinePage);
			page++;
		}
		return new TweetList(tweets);
	}

	private static void addTweetsToList (final List<Tweet> list, final ResponseList<Status> tweets) {
		for (Status status : tweets) {
			Tweet tweet = convertTweet(status);
			list.add(tweet);
		}
	}

	private static Tweet convertTweet (final Status s) {
		String text = expandUrls(s);
		// TODO process s.getMediaEntities().
		return new Tweet(s.getId(), s.getUser().getScreenName(), text, s.getCreatedAt().getTime() / 1000L);
	}

	private static String expandUrls (final Status s) {
		final URLEntity[] urls = s.getURLEntities();
		final String text = s.getText();
		if (urls == null || urls.length < 1) return text;

		final StringBuilder bld = new StringBuilder();
		for (int i = 0; i < urls.length; i++) {
			final URLEntity url = urls[i];
			if (url.getStart() < 0 || url.getEnd() > text.length()) return text; // All bets are off.
			bld.append(text.substring(i == 0 ? 0 : urls[i - 1].getEnd(), url.getStart()))
					.append(url.getExpandedURL() != null ? url.getExpandedURL() : url.getURL());
		}
		bld.append(text.substring(urls[urls.length - 1].getEnd()));
		String expandedText = bld.toString();
		LOG.i("Expanded '%s' --> '%s'.", text, expandedText);
		return expandedText;
	}

	private static long minIdOf (final long statingMin, final ResponseList<Status> tweets) {
		long min = statingMin;
		for (Status status : tweets) {
			min = Math.min(min, status.getId());
		}
		return min;
	}

}

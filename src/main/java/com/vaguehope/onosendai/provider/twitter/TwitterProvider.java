package com.vaguehope.onosendai.provider.twitter;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;

public class TwitterProvider {

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

	public TweetList getTweets (final TwitteResource resource, final Account account) throws TwitterException {
		Twitter t = this.accounts.get(account.id);
		if (t == null) throw new IllegalStateException("Account not configured: '" + account.id + "'.");
		switch (resource) {
			case TIMELINE:
				return fetchTwitterFeed(t, TwitterFeeds.HOME_TIMELINE, 100); // FIXME extract count.
			case MENTIONS:
				return fetchTwitterFeed(t, TwitterFeeds.MENTIONS, 20); // FIXME extract count.
			case ME:
				return fetchTwitterFeed(t, TwitterFeeds.ME, 20); // FIXME extract count.
			default:
				throw new IllegalStateException("Unknown resource type: " + resource);
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

	private static TweetList fetchTwitterFeed (final Twitter t, final TwitterFeed feed, final int minCount) throws TwitterException {
		ArrayList<Tweet> tweets = new ArrayList<Tweet>();
		int pageSize = Math.min(minCount, C.TWEET_FETCH_PAGE_SIZE);
		int page = 1; // First page is 1.
		while (tweets.size() < minCount) {
			Paging paging = new Paging(page, pageSize);
			ResponseList<Status> timelinePage = feed.getTweets(t, paging);
			if (timelinePage.size() < 1) break;
			addTweetsToList(tweets, timelinePage);
			page++;
		}
		return new TweetList(tweets);
	}

	private static void addTweetsToList (final ArrayList<Tweet> list, final ResponseList<Status> tweets) {
		for (Status status : tweets) {
			Tweet tweet = convertTweet(status);
			list.add(tweet);
		}
	}

	private static Tweet convertTweet (final Status s) {
		return new Tweet(s.getId(), s.getUser().getScreenName(), s.getText(), s.getCreatedAt().getTime() / 1000L);
	}

}

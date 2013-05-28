package com.vaguehope.onosendai.provider.twitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import twitter4j.ResponseList;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserList;
import twitter4j.conf.ConfigurationBuilder;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;

public class TwitterProvider {

	private final ConcurrentMap<String, Twitter> accounts;

	public TwitterProvider () {
		this.accounts = new ConcurrentHashMap<String, Twitter>();
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.getId())) return;
		final TwitterFactory tf = makeTwitterFactory(account);
		final Twitter t = tf.getInstance();
		if (this.accounts.putIfAbsent(account.getId(), t) != null) {
			t.shutdown();
		}
	}

	private Twitter getTwitter (final Account account) {
		final Twitter t = this.accounts.get(account.getId());
		if (t != null) return t;
		addAccount(account);
		return this.accounts.get(account.getId());
	}

	public void shutdown () {
		final Iterator<Twitter> itr = this.accounts.values().iterator();
		while (itr.hasNext()) {
			final Twitter t = itr.next();
			t.shutdown();
			itr.remove();
		}
	}

	/**
	 * TODO use a call back to return tweets progressively.
	 */
	public TweetList getTweets (final TwitterFeed feed, final Account account, final long sinceId) throws TwitterException {
		return feed.getTweets(account, getTwitter(account), sinceId);
	}

	public Tweet getTweet (final Account account, final long id) throws TwitterException {
		return TwitterUtils.convertTweet(account, getTwitter(account).showStatus(id));
	}

	public void post (final Account account, final String body, final long inReplyTo) throws TwitterException {
		final StatusUpdate s = new StatusUpdate(body);
		if (inReplyTo > 0) s.setInReplyToStatusId(inReplyTo);
		getTwitter(account).updateStatus(s);
	}

	public void rt (final Account account, final long id) throws TwitterException {
		getTwitter(account).retweetStatus(id);
	}

	public List<String> getListSlugs(final Account account) throws TwitterException {
		final Twitter t = getTwitter(account);
		final ResponseList<UserList> lists = t.getUserLists(t.getId());
		final List<String> slugs = new ArrayList<String>();
		for (final UserList list : lists) {
			slugs.add(list.getSlug());
		}
		return slugs;
	}

	private static TwitterFactory makeTwitterFactory (final Account account) {
		final ConfigurationBuilder cb = new ConfigurationBuilder()
				.setUseSSL(true)
				.setOAuthConsumerKey(account.getConsumerKey())
				.setOAuthConsumerSecret(account.getConsumerSecret())
				.setOAuthAccessToken(account.getAccessToken())
				.setOAuthAccessTokenSecret(account.getAccessSecret());
		return new TwitterFactory(cb.build());
	}

}

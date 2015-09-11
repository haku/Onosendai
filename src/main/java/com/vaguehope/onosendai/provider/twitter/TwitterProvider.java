package com.vaguehope.onosendai.provider.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.conf.ConfigurationBuilder;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.StringHelper;

public class TwitterProvider {

	private final ConcurrentMap<String, Twitter> accounts;

	public TwitterProvider () {
		this.accounts = new ConcurrentHashMap<String, Twitter>();
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.getId())) return;
		final TwitterFactory tf = makeTwitterFactory(account);
		final Twitter t = tf.getInstance();
		this.accounts.putIfAbsent(account.getId(), t);
	}

	private Twitter getTwitter (final Account account) {
		final Twitter t = this.accounts.get(account.getId());
		if (t != null) return t;
		addAccount(account);
		return this.accounts.get(account.getId());
	}

	public void shutdown () {
		this.accounts.clear();
	}

	public TweetList getTweets (final TwitterFeed feed, final Account account, final long sinceId, final boolean hdMedia) throws TwitterException {
		return getTweets(feed, account, sinceId, hdMedia, null);
	}

	public TweetList getTweets (final TwitterFeed feed, final Account account, final long sinceId, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		return feed.getTweets(account, getTwitter(account), sinceId, hdMedia, extraMetas);
	}

	public Tweet getTweet (final Account account, final long id, final boolean hdMedia) throws TwitterException {
		return getTweet(account, id, hdMedia, null);
	}

	public Tweet getTweet (final Account account, final long id, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		final Twitter t = getTwitter(account);
		return TwitterUtils.convertTweet(account, t.showStatus(id), t.getId(), hdMedia, extraMetas, null);
	}

	public void post (final Account account, final String body, final long inReplyTo, final ImageMetadata media) throws TwitterException, IOException {
		InputStream attachmentIs = null;
		try {
			final StatusUpdate s = new StatusUpdate(body);
			if (inReplyTo > 0) s.setInReplyToStatusId(inReplyTo);
			if (media != null && media.exists()) {
				attachmentIs = media.open();
				s.setMedia(media.getName(), attachmentIs);
			}
			getTwitter(account).updateStatus(s);
		}
		finally {
			IoHelper.closeQuietly(attachmentIs);
		}
	}

	public void rt (final Account account, final long id) throws TwitterException {
		getTwitter(account).retweetStatus(id);
	}

	public void fav (final Account account, final long id) throws TwitterException {
		getTwitter(account).createFavorite(id);
	}

	public void delete (final Account account, final long id) throws TwitterException {
		getTwitter(account).destroyStatus(id);
	}

	public List<String> getListSlugs(final Account account) throws TwitterException {
		return getListSlugs(account, null);
	}

	public List<String> getListSlugs(final Account account, final String ownerScreenName) throws TwitterException {
		final Twitter t = getTwitter(account);
		final ResponseList<UserList> lists;
		if (StringHelper.isEmpty(ownerScreenName)) {
			lists = t.getUserLists(t.getId());
		}
		else {
			lists = t.getUserLists(ownerScreenName);
		}
		final List<String> slugs = new ArrayList<String>();
		for (final UserList list : lists) {
			slugs.add(list.getSlug());
		}
		return slugs;
	}

	public User getUser (final Account account, final String screenName) throws TwitterException {
		return getTwitter(account).showUser(screenName);
	}

	public Relationship getRelationship (final Account account, final User otherUser) throws TwitterException {
		final Twitter t = getTwitter(account);
		return t.showFriendship(t.getId(), otherUser.getId());
	}

	public void follow (final Account account, final User targetUser) throws TwitterException {
		getTwitter(account).createFriendship(targetUser.getId());
	}

	public void unfollow (final Account account, final User targetUser) throws TwitterException {
		getTwitter(account).destroyFriendship(targetUser.getId());
	}

	private static TwitterFactory makeTwitterFactory (final Account account) {
		final ConfigurationBuilder cb = new ConfigurationBuilder()
				.setOAuthConsumerKey(account.getConsumerKey())
				.setOAuthConsumerSecret(account.getConsumerSecret())
				.setOAuthAccessToken(account.getAccessToken())
				.setOAuthAccessTokenSecret(account.getAccessSecret());
		return new TwitterFactory(cb.build());
	}

}

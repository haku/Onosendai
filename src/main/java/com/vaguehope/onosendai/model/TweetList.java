package com.vaguehope.onosendai.model;

import java.util.Collections;
import java.util.List;

public class TweetList {

	private final List<Tweet> tweets;
	private final SinceIdType sinceIdType;
	private final List<Tweet> quotedTweets;

	public TweetList (final List<Tweet> tweets) {
		this(tweets, null, null);
	}

	public TweetList (final List<Tweet> tweets, final SinceIdType sinceIdType, final List<Tweet> quotedTweets) {
		this.sinceIdType = sinceIdType;
		this.tweets = Collections.unmodifiableList(tweets);
		this.quotedTweets = quotedTweets != null ? Collections.unmodifiableList(quotedTweets) : Collections.<Tweet>emptyList();
	}

	public int count () {
		return this.tweets.size();
	}

	public Tweet getTweet (final int index) {
		return this.tweets.get(index);
	}

	public List<Tweet> getTweets () {
		return this.tweets;
	}

	public Tweet getMostRecent () {
		if (this.tweets == null || this.tweets.size() < 1) throw new IllegalStateException("Tweet list has no items.");
		Tweet ret = null;
		for (final Tweet tweet : this.tweets) {
			if (ret == null || (tweet.getTime() >= ret.getTime() && tweet.getSid().compareTo(ret.getSid()) > 0)) ret = tweet;
		}
		return ret;
	}

	public String getSinceId () {
		final Tweet mostRecent = getMostRecent();
		switch (this.sinceIdType) {
			case SID:
				return mostRecent.getSid();
			case NOTIFICAITON_ID_META:
				final Meta m = mostRecent.getFirstMetaOfType(MetaType.NOTIFICAITON_ID);
				return m != null ? m.getData() : null;
			default:
				throw new IllegalStateException("Unknown sinceIdType: " + this.sinceIdType);
		}
	}

	/**
	 * Never null.
	 */
	public List<Tweet> getQuotedTweets () {
		return this.quotedTweets;
	}

}

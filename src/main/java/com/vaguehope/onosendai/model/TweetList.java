package com.vaguehope.onosendai.model;

import java.util.Collections;
import java.util.List;

public class TweetList {

	private final List<Tweet> tweets;

	public TweetList (final List<Tweet> tweets) {
		this.tweets = Collections.unmodifiableList(tweets);
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

}

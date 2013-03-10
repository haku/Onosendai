package com.vaguehope.onosendai.model;

import java.util.List;

public class TweetList {

	private final List<Tweet> tweets;

	public TweetList (final List<Tweet> tweets) {
		this.tweets = tweets;
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

}

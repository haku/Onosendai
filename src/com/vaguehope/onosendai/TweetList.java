package com.vaguehope.onosendai;

import java.util.ArrayList;

public class TweetList {

	private final ArrayList<Tweet> tweets;

	public TweetList (ArrayList<Tweet> tweets) {
		this.tweets = tweets;
	}

	public int count () {
		return this.tweets.size();
	}

	public Tweet getTweet (int index) {
		return this.tweets.get(index);
	}

}

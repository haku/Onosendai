package com.vaguehope.onosendai.model;

public class TweetBuilder {

	private long id;
	private String username;
	private String body;
	private long unitTimeSeconds;
	private String avatarUrl;

	public TweetBuilder () {
		reset();
	}

	public void reset () {
		this.id = 0L;
		this.username = null;
		this.body = null;
		this.unitTimeSeconds = 0L;
		this.avatarUrl = null;
	}

	public TweetBuilder id (final long v) {
		this.id = v;
		return this;
	}

	public TweetBuilder username (final String v) {
		this.username = v;
		return this;
	}

	public TweetBuilder body (final String v) {
		this.body = v;
		return this;
	}

	public TweetBuilder unitTimeSeconds (final long v) {
		this.unitTimeSeconds = v;
		return this;
	}


	public TweetBuilder avatarUrl (final String v) {
		this.avatarUrl = v;
		return this;
	}

	public Tweet build () {
		Tweet t = new Tweet(this.id, this.username, this.body, this.unitTimeSeconds, this.avatarUrl);
		reset();
		return t;
	}

}

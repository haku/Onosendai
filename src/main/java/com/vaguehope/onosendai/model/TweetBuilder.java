package com.vaguehope.onosendai.model;

public class TweetBuilder {

	private String id;
	private String username;
	private String body;
	private long unitTimeSeconds;
	private String avatarUrl;
	// TODO include metas?

	public TweetBuilder () {
		reset();
	}

	public final void reset () {
		this.id = null;
		this.username = null;
		this.body = null;
		this.unitTimeSeconds = 0L;
		this.avatarUrl = null;
	}

	public TweetBuilder id (final String v) {
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

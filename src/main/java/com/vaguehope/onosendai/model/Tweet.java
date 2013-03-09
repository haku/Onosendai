package com.vaguehope.onosendai.model;

public class Tweet {

	private final long id;
	private final String username;
	private final String body;
	private final long time;

	public Tweet (final long id, final String username, final String body, final long unitTimeSeconds) {
		this.id = id;
		this.username = username;
		this.body = body;
		this.time = unitTimeSeconds;
	}

	public long getId () {
		return this.id;
	}

	public String getUsername () {
		return this.username;
	}

	public String getBody () {
		return this.body;
	}

	public long getTime () {
		return this.time;
	}

}

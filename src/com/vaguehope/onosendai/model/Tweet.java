package com.vaguehope.onosendai.model;

public class Tweet {

	private final String username;
	private final String body;
	private final long time;

	public Tweet (String username, String body, long time) {
		this.username = username;
		this.body = body;
		this.time = time;
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

package com.vaguehope.onosendai.model;

public class Tweet {

	private final String username;
	private final String body;

	public Tweet (String username, String body) {
		this.username = username;
		this.body = body;
	}

	public String getUsername () {
		return this.username;
	}

	public String getBody () {
		return this.body;
	}

}

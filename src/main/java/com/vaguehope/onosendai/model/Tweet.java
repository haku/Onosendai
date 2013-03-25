package com.vaguehope.onosendai.model;

import java.util.List;

public class Tweet {

	private final long id;
	private final String username;
	private final String body;
	private final long time;
	private final String avatarUrl;
	private final List<Meta> metas;

	public Tweet (final long id, final String username, final String body, final long unitTimeSeconds, final String avatarUrl) {
		this(id, username, body, unitTimeSeconds, avatarUrl, null);
	}

	public Tweet (final long id, final String username, final String body, final long unitTimeSeconds, final String avatarUrl, final List<Meta> metas) {
		this.id = id;
		this.username = username;
		this.body = body;
		this.time = unitTimeSeconds;
		this.avatarUrl = avatarUrl;
		this.metas = metas;
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

	public String getAvatarUrl () {
		return this.avatarUrl;
	}

	public List<Meta> getMetas() {
		return this.metas;
	}

}

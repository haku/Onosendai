package com.vaguehope.onosendai.model;

import java.util.List;

import org.json.JSONException;

public class Tweet {

	private final long id;
	private final String username;
	private final String body;
	private final long time;
	private final String avatarUrl;
	private final String meta;

	public Tweet (final long id, final String username, final String body, final long unitTimeSeconds, final String avatarUrl) {
		this(id, username, body, unitTimeSeconds, avatarUrl, (String)null);
	}

	public Tweet (final long id, final String username, final String body, final long unitTimeSeconds, final String avatarUrl, final MetaBuilder metaBuilder) {
		this(id, username, body, unitTimeSeconds, avatarUrl, metaBuilder.build());
	}

	public Tweet (final long id, final String username, final String body, final long unitTimeSeconds, final String avatarUrl, final String meta) {
		this.id = id;
		this.username = username;
		this.body = body;
		this.time = unitTimeSeconds;
		this.avatarUrl = avatarUrl;
		this.meta = meta;
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

	public String getMeta() {
		return this.meta;
	}

	public List<Meta> parseMeta () throws JSONException {
		if (this.meta == null) return null;
		return MetaBuilder.parseMeta(this.meta);
	}

}

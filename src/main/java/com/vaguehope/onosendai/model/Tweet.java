package com.vaguehope.onosendai.model;

import java.util.List;

public class Tweet {

	private final long uid;
	private final String sid;
	private final String username;
	private final String body;
	private final long time;
	private final String avatarUrl;
	private final List<Meta> metas;

	Tweet (final String sid, final String username, final String body, final long unitTimeSeconds, final String avatarUrl) {
		this(-1L, sid, username, body, unitTimeSeconds, avatarUrl, null);
	}

	public Tweet (final String sid, final String username, final String body, final long unitTimeSeconds, final String avatarUrl, final List<Meta> metas) {
		this(-1L, sid, username, body, unitTimeSeconds, avatarUrl, metas);
	}

	public Tweet (final long uid, final String sid, final String username, final String body, final long unitTimeSeconds, final String avatarUrl, final List<Meta> metas) {
		this.uid = uid;
		this.sid = sid;
		this.username = username;
		this.body = body;
		this.time = unitTimeSeconds;
		this.avatarUrl = avatarUrl;
		this.metas = metas;
	}

	/**
	 * Local unique ID provided by the DB.
	 * May be -1L for objects not served from DB.
	 */
	public long getUid () {
		return this.uid;
	}

	/**
	 * ID provided by service (T, FB, etc.)
	 */
	public String getSid () {
		return this.sid;
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

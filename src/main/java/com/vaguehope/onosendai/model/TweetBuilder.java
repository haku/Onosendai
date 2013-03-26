package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.List;

public class TweetBuilder {

	private String id;
	private String username;
	private String body;
	private long unitTimeSeconds;
	private String avatarUrl;
	private List<Meta> metas;

	public TweetBuilder () {
		reset();
	}

	public final void reset () {
		this.id = null;
		this.username = null;
		this.body = null;
		this.unitTimeSeconds = 0L;
		this.avatarUrl = null;
		this.metas = null;
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

	public TweetBuilder meta (final Meta v) {
		if (this.metas == null) this.metas = new ArrayList<Meta>();
		this.metas.add(v);
		return this;
	}

	public TweetBuilder meta (final MetaType type, final String data) {
		return meta(new Meta(type, data));
	}

	public Tweet build () {
		Tweet t = new Tweet(this.id, this.username, this.body, this.unitTimeSeconds, this.avatarUrl, this.metas);
		reset();
		return t;
	}

}

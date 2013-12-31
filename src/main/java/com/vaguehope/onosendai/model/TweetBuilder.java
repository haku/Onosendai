package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.List;

public class TweetBuilder {

	private String id;
	private String username;
	private String fullname;
	private String body;
	private long unitTimeSeconds;
	private String avatarUrl;
	private String inlineMediaUrl;
	private List<Meta> metas;

	private String replyToId;
	private StringBuilder subTitles;

	public TweetBuilder () {
		reset();
	}

	public final void reset () {
		this.id = null;
		this.username = null;
		this.fullname = null;
		this.body = null;
		this.unitTimeSeconds = 0L;
		this.avatarUrl = null;
		this.inlineMediaUrl = null;
		this.metas = null;
		this.replyToId = null;
		this.subTitles = null;
	}

	public TweetBuilder id (final String v) {
		this.id = v;
		return this;
	}

	public TweetBuilder username (final String v) {
		this.username = v;
		return this;
	}

	public TweetBuilder fullname (final String v) {
		this.fullname = v;
		return this;
	}

	public TweetBuilder body (final String v) {
		this.body = v;
		return this;
	}

	public TweetBuilder bodyIfAbsent (final String v) {
		if (this.body == null || this.body.isEmpty()) this.body = v;
		return this;
	}

	public TweetBuilder unitTimeSeconds (final long v) {
		this.unitTimeSeconds = v;
		return this;
	}

	public TweetBuilder inlineMediaUrl (final String v) {
		this.inlineMediaUrl = v;
		return this;
	}

	public TweetBuilder avatarUrl (final String v) {
		this.avatarUrl = v;
		return this;
	}

	public void replyToId (final String v) {
		this.replyToId = v;
	}

	public TweetBuilder meta (final Meta v) {
		if (this.metas == null) this.metas = new ArrayList<Meta>();
		this.metas.add(v);
		return this;
	}

	public TweetBuilder meta (final MetaType type, final String data) {
		return meta(type, data, null);
	}

	public TweetBuilder meta (final MetaType type, final String data, final String title) {
		if (type == MetaType.MEDIA && this.inlineMediaUrl == null) this.inlineMediaUrl = data;
		return meta(new Meta(type, data, title));
	}

	public TweetBuilder subtitle (final String subTitle) {
		if (this.subTitles == null) {
			this.subTitles = new StringBuilder(subTitle);
		}
		else {
			this.subTitles.append(", ").append(subTitle);
		}
		return this;
	}

	public Tweet build () {
		if (this.replyToId != null && !this.replyToId.equals(this.id)) meta(MetaType.REPLYTO, this.replyToId);
		Tweet t = new Tweet(this.id, this.username,
				this.subTitles != null ? this.fullname + "\n" + this.subTitles.toString() : this.fullname,
				this.body, this.unitTimeSeconds, this.avatarUrl, this.inlineMediaUrl, this.metas);
		reset();
		return t;
	}

}

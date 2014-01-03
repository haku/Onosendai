package com.vaguehope.onosendai.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vaguehope.onosendai.util.EqualHelper;

public class Tweet {

	private final long uid;
	private final String sid;
	private final String username;
	private final String fullname;
	private final String body;
	private final long time;
	private final String avatarUrl;
	private final String inlineMediaUrl;
	private final List<Meta> metas;

	public Tweet (final String sid, final String username, final String fullname, final String body, final long unitTimeSeconds, final String avatarUrl, final String inlineMediaUrl, final List<Meta> metas) {
		this(-1L, sid, username, fullname, body, unitTimeSeconds, avatarUrl, inlineMediaUrl, metas);
	}

	public Tweet (final long uid, final String sid, final String username, final String fullname, final String body, final long unitTimeSeconds, final String avatarUrl, final String inlineMediaUrl, final List<Meta> metas) {
		this.uid = uid;
		this.sid = sid;
		this.username = username;
		this.fullname = fullname;
		this.body = body;
		this.time = unitTimeSeconds;
		this.avatarUrl = avatarUrl;
		this.inlineMediaUrl = inlineMediaUrl;
		this.metas = metas;
	}

	/**
	 * Local unique ID provided by the DB. May be -1L for objects not served
	 * from DB.
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

	public String getFullname () {
		return this.fullname;
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

	public String getInlineMediaUrl () {
		return this.inlineMediaUrl;
	}

	public List<Meta> getMetas () {
		return this.metas;
	}

	public Meta getFirstMetaOfType (final MetaType type) {
		if (this.metas == null) return null;
		for (Meta meta : this.metas) {
			if (type == meta.getType()) return meta;
		}
		return null;
	}

	public Tweet cloneWithCurrentTimestamp() {
		final long utime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final List<Meta> newMetas = new ArrayList<Meta>(this.metas);
		newMetas.add(new Meta(MetaType.POST_TIME, String.valueOf(this.time)));
		return new Tweet(this.uid, this.sid, this.username, this.fullname, this.body, utime, this.avatarUrl, this.inlineMediaUrl, newMetas);
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("Tweet{").append(this.uid)
				.append(",").append(this.sid)
				.append("}").toString();
	}

	public String toFullString () {
		return new StringBuilder()
				.append("Tweet{").append(this.uid)
				.append(",").append(this.sid)
				.append(",").append(this.username)
				.append(",").append(this.fullname)
				.append(",").append(this.body)
				.append(",").append(this.time)
				.append(",").append(this.avatarUrl)
				.append(",").append(this.inlineMediaUrl)
				.append(",").append(this.metas)
				.append("}").toString();
	}

	@Override
	public int hashCode () {
		return Arrays.hashCode(new Object[] {
				this.uid, this.sid, this.username, this.fullname,
				this.body, this.time, this.avatarUrl, this.inlineMediaUrl, this.metas });
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Tweet)) return false;
		return equalToTweet((Tweet) o);
	}

	private boolean equalToTweet (final Tweet that) {
		return this.uid == that.uid // NOSONAR is not too complex, its an equals method.
				&& EqualHelper.equal(this.sid, that.sid)
				&& EqualHelper.equal(this.username, that.username)
				&& EqualHelper.equal(this.fullname, that.fullname)
				&& EqualHelper.equal(this.body, that.body)
				&& this.time == that.time
				&& EqualHelper.equal(this.avatarUrl, that.avatarUrl)
				&& EqualHelper.equal(this.inlineMediaUrl, that.inlineMediaUrl)
				&& EqualHelper.equal(this.metas, that.metas);
	}

}

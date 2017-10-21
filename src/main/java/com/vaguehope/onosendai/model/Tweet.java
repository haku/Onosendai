package com.vaguehope.onosendai.model;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.StringHelper;

public class Tweet {

	private final long uid;
	private final String sid;
	private final String username;
	private final String fullname;
	private final String userSubtitle;
	private final String fullSubtitle;
	private final String ownerUsername;
	private final String body;
	private final long time;
	private final String avatarUrl;
	private final String inlineMediaUrl;
	private final String quotedSid;
	private final List<Meta> metas;
	private final boolean filtered;

	public Tweet (final String sid, final String username, final String fullname, final String userSubtitle, final String fullSubtitle, final String ownerUsername, final String body, final long unitTimeSeconds, final String avatarUrl, final String inlineMediaUrl, final String quotedSid, final List<Meta> metas) {
		this(-1L, sid, username, fullname, userSubtitle, fullSubtitle, ownerUsername, body, unitTimeSeconds, avatarUrl, inlineMediaUrl, quotedSid, metas, false);
	}

	public Tweet (final long uid, final String sid, final String username, final String fullname, final String userSubtitle, final String fullSubtitle, final String ownerUsername, final String body, final long unitTimeSeconds, final String avatarUrl, final String inlineMediaUrl, final String quotedSid, final List<Meta> metas, final boolean filtered) {
		this.uid = uid;
		this.sid = sid;
		this.username = username;
		this.fullname = fullname;
		this.userSubtitle = userSubtitle;
		this.fullSubtitle = fullSubtitle;
		this.ownerUsername = ownerUsername;
		this.body = body;
		this.time = unitTimeSeconds;
		this.avatarUrl = avatarUrl;
		this.inlineMediaUrl = inlineMediaUrl;
		this.quotedSid = quotedSid;
		this.metas = metas;
		this.filtered = filtered;
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

	public String getUsernameWithSubtitle () {
		if (this.username == null) return null;
		if (this.userSubtitle == null) return this.username;
		return String.format("%s\n%s", this.username, this.userSubtitle);
	}

	public String getFullname () {
		return this.fullname;
	}

	public String getFullnameWithSubtitle () {
		if (this.fullname == null) return null;
		if (this.fullSubtitle == null) return this.fullname;
		return String.format("%s\n%s", this.fullname, this.fullSubtitle);
	}

	public String getUserSubtitle () {
		return this.userSubtitle;
	}

	public String getFullSubtitle () {
		return this.fullSubtitle;
	}

	public String getOwnerUsername () {
		return this.ownerUsername;
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

	public String getQuotedSid () {
		return this.quotedSid;
	}

	public List<Meta> getMetas () {
		return this.metas;
	}

	public Meta getFirstMetaOfType (final MetaType type) {
		if (this.metas == null) return null;
		for (final Meta meta : this.metas) {
			if (type == meta.getType()) return meta;
		}
		return null;
	}

	public boolean isFiltered () {
		return this.filtered;
	}

	public Tweet withCurrentTimestamp () {
		final long utime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final List<Meta> newMetas = new ArrayList<Meta>(this.metas);
		if (getFirstMetaOfType(MetaType.POST_TIME) == null) newMetas.add(new Meta(MetaType.POST_TIME, String.valueOf(this.time)));
		return new Tweet(this.uid, this.sid, this.username, this.fullname, this.userSubtitle, this.fullSubtitle, this.ownerUsername, this.body, utime, this.avatarUrl, this.inlineMediaUrl, this.quotedSid, newMetas, this.filtered);
	}

	public Tweet withFiltered (final boolean newFiltered) {
		if (newFiltered == this.filtered) return this;
		return new Tweet(this.uid, this.sid, this.username, this.fullname, this.userSubtitle, this.fullSubtitle, this.ownerUsername, this.body, this.time, this.avatarUrl, this.inlineMediaUrl, this.quotedSid, this.metas, newFiltered);
	}

	public String toHumanLine () {
		return new StringBuilder().append("\"").append(this.body).append("\" ").append(StringHelper.firstLine(this.fullname)).toString();
	}

	public String toHumanParagraph () {
		final StringBuilder s = new StringBuilder().append("\"").append(this.body).append("\"\n").append(StringHelper.firstLine(this.fullname));
		if (!StringHelper.isEmpty(this.username)) s.append(" (").append(StringHelper.firstLine(this.username)).append(")");
		s.append("\n").append(DateFormat.getDateTimeInstance().format(new Date(TimeUnit.SECONDS.toMillis(this.time))));
		return s.toString();
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
				.append(",").append(this.userSubtitle)
				.append(",").append(this.fullSubtitle)
				.append(",").append(this.ownerUsername)
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
				this.userSubtitle, this.fullSubtitle, this.ownerUsername,
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
				&& EqualHelper.equal(this.userSubtitle, that.userSubtitle)
				&& EqualHelper.equal(this.fullSubtitle, that.fullSubtitle)
				&& EqualHelper.equal(this.ownerUsername, that.ownerUsername)
				&& EqualHelper.equal(this.body, that.body)
				&& this.time == that.time
				&& EqualHelper.equal(this.avatarUrl, that.avatarUrl)
				&& EqualHelper.equal(this.inlineMediaUrl, that.inlineMediaUrl)
				&& EqualHelper.equal(this.metas, that.metas);
	}

}

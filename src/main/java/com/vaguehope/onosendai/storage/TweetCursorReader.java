package com.vaguehope.onosendai.storage;

import android.database.Cursor;

public class TweetCursorReader {

	private int colUid = -1;
	private int colSid = -1;
	private int colTime = -1;
	private int colUsername = -1;
	private int colFullname = -1;
	private int colUserSubtitle = -1;
	private int colFullSubtitle = -1;
	private int colBody = -1;
	private int colAvatar = -1;
	private int colInlineMedia = -1;
	private int colFiltered = -1;

	public long readUid (final Cursor c) {
		if (c == null) return -1;
		if (this.colUid < 0) this.colUid = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_ID);
		return c.getLong(this.colUid);
	}

	public String readSid (final Cursor c) {
		if (c == null) return null;
		if (this.colSid < 0) this.colSid = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_SID);
		return c.getString(this.colSid);
	}

	public long readTime (final Cursor c) {
		if (c == null) return -1;
		if (this.colTime < 0) this.colTime = c.getColumnIndex(DbAdapter.TBL_TW_TIME);
		return c.getLong(this.colTime);
	}

	public String readUsername (final Cursor c) {
		if (c == null) return null;
		if (this.colUsername < 0) this.colUsername = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_USERNAME);
		return c.getString(this.colUsername);
	}

	public String readUserSubtitle (final Cursor c) {
		if (c == null) return null;
		if (this.colUserSubtitle < 0) this.colUserSubtitle = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_USERSUBTITLE);
		return c.getString(this.colUserSubtitle);
	}

	public String readUsernameWithSubtitle (final Cursor c) {
		if (c == null) return null;

		final String username = readUsername(c);
		if (username == null) return null;

		final String userSubtitle = readUserSubtitle(c);
		if (userSubtitle == null) return username;

		return String.format("%s\n%s", username, userSubtitle);
	}

	public String readFullname (final Cursor c) {
		if (c == null) return null;
		if (this.colFullname < 0) this.colFullname = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_FULLNAME);
		return c.getString(this.colFullname);
	}

	public String readFullSubtitle (final Cursor c) {
		if (c == null) return null;
		if (this.colFullSubtitle < 0) this.colFullSubtitle = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_FULLSUBTITLE);
		return c.getString(this.colFullSubtitle);
	}

	public String readFullnameWithSubtitle (final Cursor c) {
		if (c == null) return null;

		final String fullname = readFullname(c);
		if (fullname == null) return null;

		final String fullSubtitle = readFullSubtitle(c);
		if (fullSubtitle == null) return fullname;

		return String.format("%s\n%s", fullname, fullSubtitle);
	}

	public String readBody (final Cursor c) {
		if (c == null) return null;
		if (this.colBody < 0) this.colBody = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_BODY);
		return c.getString(this.colBody);
	}

	public String readAvatar (final Cursor c) {
		if (c == null) return null;
		if (this.colAvatar < 0) this.colAvatar = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_AVATAR);
		return c.getString(this.colAvatar);
	}

	public String readInlineMedia (final Cursor c) {
		if (c == null) return null;
		if (this.colInlineMedia < 0) this.colInlineMedia = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_INLINEMEDIA);
		return c.getString(this.colInlineMedia);
	}

	public boolean readFiltered (final Cursor c) {
		if (c == null) return false;
		if (this.colFiltered < 0) this.colFiltered = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_FILTERED);
		return !c.isNull(this.colFiltered);
	}

}

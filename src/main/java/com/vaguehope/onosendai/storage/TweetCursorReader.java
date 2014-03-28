package com.vaguehope.onosendai.storage;

import android.database.Cursor;

public class TweetCursorReader {

	private int colSid = -1;
	private int colTime = -1;
	private int colUsername = -1;
	private int colFullname = -1;
	private int colBody = -1;
	private int colAvatar = -1;
	private int colInlineMedia = -1;

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

	public String readFullname (final Cursor c) {
		if (c == null) return null;
		if (this.colFullname < 0) this.colFullname = c.getColumnIndexOrThrow(DbAdapter.TBL_TW_FULLNAME);
		return c.getString(this.colFullname);
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

}

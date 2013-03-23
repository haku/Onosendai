package com.vaguehope.onosendai.storage;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.LogWrapper;

public class DbAdapter implements DbInterface {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DB_NAME = "tweets";
	private static final int DB_VERSION = 5;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final LogWrapper log = new LogWrapper("DB");
	private final Context mCtx;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final List<Runnable> twUpdateActions = new ArrayList<Runnable>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final LogWrapper log = new LogWrapper("DBH");

		DatabaseHelper (final Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate (final SQLiteDatabase db) {
			db.execSQL(TBL_TW_CREATE);
			db.execSQL(TBL_SC_CREATE);
		}

		@Override
		public void onUpgrade (final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			this.log.w("Upgrading database from version %d to %d, which will destroy all old data.", oldVersion, newVersion);
			if (oldVersion < 5) {
				db.execSQL("ALTER TABLE " + TBL_TW + " ADD COLUMN " + TBL_TW_AVATAR + " text;");
			}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public DbAdapter (final Context ctx) {
		this.mCtx = ctx;
	}

	public void open () {
		this.mDbHelper = new DatabaseHelper(this.mCtx);
		this.mDb = this.mDbHelper.getWritableDatabase();
	}

	public void close () {
		this.mDb.close();
		this.mDbHelper.close();
	}

	public boolean checkDbOpen () {
		if (this.mDb == null) {
			this.log.e("aborting because mDb==null.");
			return false;
		}

		if (!this.mDb.isOpen()) {
			this.log.d("mDb was not open; opeing it...");
			open();
		}

		return true;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tweets.

	private static final String TBL_TW = "tw";
	private static final String TBL_TW_ID = "_id";
	private static final String TBL_TW_COLID = "colid";
	private static final String TBL_TW_SID = "sid";
	private static final String TBL_TW_TIME = "time";
	private static final String TBL_TW_NAME = "name";
	private static final String TBL_TW_BODY = "body";
	private static final String TBL_TW_AVATAR = "avatar";
	private static final String TBL_TW_META = "meta";

	private static final String TBL_TW_CREATE = "create table " + TBL_TW + " ("
			+ TBL_TW_ID + " integer primary key autoincrement,"
			+ TBL_TW_COLID + " integer,"
			+ TBL_TW_SID + " integer,"
			+ TBL_TW_TIME + " integer,"
			+ TBL_TW_NAME + " text,"
			+ TBL_TW_BODY + " text,"
			+ TBL_TW_AVATAR + " text,"
			+ TBL_TW_META + " text,"
			+ "UNIQUE(" + TBL_TW_COLID + ", " + TBL_TW_SID + ") ON CONFLICT REPLACE" +
			");";

	// TODO add table index by time?

	@Override
	public void storeTweets (final Column column, final List<Tweet> tweets) {
		// Clear old data.
		this.mDb.beginTransaction();
		try {
			int n = this.mDb.delete(TBL_TW,
					TBL_TW_COLID + "=? AND " + TBL_TW_ID + " NOT IN (SELECT " + TBL_TW_ID + " FROM " + TBL_TW +
							" WHERE " + TBL_TW_COLID + "=?" +
							" ORDER BY " + TBL_TW_TIME +
							" DESC LIMIT " + C.DATA_TW_MAX_COL_ENTRIES + ")",
					new String[] { String.valueOf(column.id), String.valueOf(column.id) });

			this.log.d("Deleted %d rows from %s column %d.", n, TBL_TW, column.id);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		this.mDb.beginTransaction();
		try {
			for (Tweet tweet : tweets) {
				ContentValues values = new ContentValues();
				values.put(TBL_TW_COLID, column.id);
				values.put(TBL_TW_SID, tweet.getId());
				values.put(TBL_TW_TIME, tweet.getTime());
				values.put(TBL_TW_NAME, tweet.getUsername());
				values.put(TBL_TW_BODY, tweet.getBody());
				values.put(TBL_TW_AVATAR, tweet.getAvatarUrl());
				values.put(TBL_TW_META, tweet.getMeta());
				this.mDb.insertWithOnConflict(TBL_TW, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		notifyTwListeners(); // TODO include column id.
	}

	@Override
	public void deleteTweet (final Column column, final Tweet tweet) {
		this.mDb.beginTransaction();
		try {
			this.mDb.delete(TBL_TW, TBL_TW_COLID + "=? AND " + TBL_TW_SID + "=?",
					new String[] { String.valueOf(column.id), String.valueOf(tweet.getId()) });
			this.log.d("Deleted tweet %d from %s column %d.", tweet.getId(), TBL_TW, column.id);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		notifyTwListeners(); // TODO include column id.
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf) {
		if (!checkDbOpen()) return null;
		List<Tweet> ret = new ArrayList<Tweet>();
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_TW,
					new String[] { TBL_TW_SID, TBL_TW_NAME, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR },
					TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) },
					null, null,
					TBL_TW_TIME + " desc", String.valueOf(numberOf));

			if (c != null && c.moveToFirst()) {
				int colSid = c.getColumnIndex(TBL_TW_SID);
				int colName = c.getColumnIndex(TBL_TW_NAME);
				int colBody = c.getColumnIndex(TBL_TW_BODY);
				int colTime = c.getColumnIndex(TBL_TW_TIME);
				int colAvatar = c.getColumnIndex(TBL_TW_AVATAR);

				ret = new ArrayList<Tweet>();
				do {
					long sid = c.getLong(colSid);
					String name = c.getString(colName);
					String body = c.getString(colBody);
					long time = c.getLong(colTime);
					String avatar = c.getString(colAvatar);
					ret.add(new Tweet(sid, name, body, time, avatar));
				}
				while (c.moveToNext());
			}
		}
		finally {
			if (c != null) c.close();
		}
		return ret;
	}

	@Override
	public Tweet getTweetDetails (final int columnId, final Tweet tweet) {
		if (!checkDbOpen()) return null;
		Tweet ret = null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_TW,
					new String[] { TBL_TW_SID, TBL_TW_NAME, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR, TBL_TW_META },
					TBL_TW_COLID + "=? AND " + TBL_TW_SID + "=?",
					new String[] { String.valueOf(columnId), String.valueOf(tweet.getId()) },
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				int colSid = c.getColumnIndex(TBL_TW_SID);
				int colName = c.getColumnIndex(TBL_TW_NAME);
				int colBody = c.getColumnIndex(TBL_TW_BODY);
				int colTime = c.getColumnIndex(TBL_TW_TIME);
				int colAvatar = c.getColumnIndex(TBL_TW_AVATAR);
				int colMeta = c.getColumnIndex(TBL_TW_META);

				long sid = c.getLong(colSid);
				String name = c.getString(colName);
				String body = c.getString(colBody);
				long time = c.getLong(colTime);
				String meta = c.getString(colMeta);
				String avatar = c.getString(colAvatar);
				ret = new Tweet(sid, name, body, time, avatar, meta);
			}
		}
		finally {
			if (c != null) c.close();
		}
		return ret;
	}

	private void notifyTwListeners () {
		for (Runnable r : this.twUpdateActions) {
			r.run();
		}
	}

	@Override
	public void addTwUpdateListener (final Runnable action) {
		this.twUpdateActions.add(action);
	}

	@Override
	public void removeTwUpdateListener (final Runnable action) {
		this.twUpdateActions.remove(action);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Scrolls.

	private static final String TBL_SC = "sc";
	private static final String TBL_SC_ID = "_id";
	private static final String TBL_SC_COLID = "colid";
	private static final String TBL_SC_ITEMID = "itemid";
	private static final String TBL_SC_TOP = "top";

	private static final String TBL_SC_CREATE = "create table " + TBL_SC + " ("
			+ TBL_SC_ID + " integer primary key autoincrement,"
			+ TBL_SC_COLID + " integer,"
			+ TBL_SC_ITEMID + " integer,"
			+ TBL_SC_TOP + " integer,"
			+ "UNIQUE(" + TBL_SC_COLID + ") ON CONFLICT REPLACE" +
			");";

	@Override
	public void storeScroll (final int columnId, final ScrollState state) {
		if (state == null) return;

		this.mDb.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(TBL_SC_COLID, columnId);
			values.put(TBL_SC_ITEMID, state.itemId);
			values.put(TBL_SC_TOP, state.top);
			this.mDb.insertWithOnConflict(TBL_SC, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.log.d("Stored scroll for col %d: %s", columnId, state);
	}

	@Override
	public ScrollState getScroll (final int columnId) {
		if (!checkDbOpen()) return null;
		ScrollState ret = null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_SC,
					new String[] { TBL_SC_ITEMID, TBL_SC_TOP },
					TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) },
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				int colItemId = c.getColumnIndex(TBL_SC_ITEMID);
				int colTop = c.getColumnIndex(TBL_SC_TOP);

				long itemId = c.getLong(colItemId);
				int top = c.getInt(colTop);
				ret = new ScrollState(itemId, top);
			}
		}
		finally {
			if (c != null) c.close();
		}

		this.log.d("Read scroll for col %d: %s", columnId, ret);
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

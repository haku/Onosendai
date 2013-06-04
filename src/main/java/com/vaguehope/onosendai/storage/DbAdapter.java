package com.vaguehope.onosendai.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class DbAdapter implements DbInterface {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DB_NAME = "tweets";
	private static final int DB_VERSION = 11;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final LogWrapper log = new LogWrapper("DB"); // TODO make static?
	private final Context mCtx;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final List<TwUpdateListener> twUpdateListeners = new ArrayList<TwUpdateListener>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final LogWrapper log = new LogWrapper("DBH");

		DatabaseHelper (final Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate (final SQLiteDatabase db) {
			db.execSQL(TBL_TW_CREATE);
			db.execSQL(TBL_TW_CREATE_INDEX);
			db.execSQL(TBL_TM_CREATE);
			db.execSQL(TBL_TM_CREATE_INDEX);
			db.execSQL(TBL_SC_CREATE);
			db.execSQL(TBL_KV_CREATE);
			db.execSQL(TBL_KV_CREATE_INDEX);
		}

		@Override
		public void onUpgrade (final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			if (oldVersion < 8) { // NOSONAR not a magic number.
				this.log.w("Upgrading database from version %d to %d, which will destroy all old data.", oldVersion, newVersion);
				db.execSQL("DROP INDEX IF EXISTS " + TBL_TM_INDEX);
				db.execSQL("DROP TABLE IF EXISTS " + TBL_TM);
				db.execSQL("DROP INDEX IF EXISTS " + TBL_TW_INDEX);
				db.execSQL("DROP TABLE IF EXISTS " + TBL_TW);
				db.execSQL("DROP TABLE IF EXISTS " + TBL_SC);
				onCreate(db);
			}
			else {
				this.log.w("Upgrading database from version %d to %d...", oldVersion, newVersion);
				if (oldVersion < 9) { // NOSONAR not a magic number.
					this.log.w("Adding column %s...", TBL_TM_TITLE);
					db.execSQL("ALTER TABLE " + TBL_TM + " ADD COLUMN " + TBL_TM_TITLE + " text;");
				}
				if (oldVersion < 10) { // NOSONAR not a magic number.
					this.log.w("Creating table %s...", TBL_KV);
					db.execSQL(TBL_KV_CREATE);
					this.log.w("Creating index %s...", TBL_KV_INDEX);
					db.execSQL(TBL_KV_CREATE_INDEX);
				}
				if (oldVersion < 11) { // NOSONAR not a magic number.
					this.log.w("Adding column %s...", TBL_SC_TIME);
					db.execSQL("ALTER TABLE " + TBL_SC + " ADD COLUMN " + TBL_SC_TIME + " integer;");
				}
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
	private static final String TBL_TW_USERNAME = "uname";
	private static final String TBL_TW_FULLNAME = "fname";
	private static final String TBL_TW_BODY = "body";
	private static final String TBL_TW_AVATAR = "avatar";

	private static final String TBL_TW_CREATE = "create table " + TBL_TW + " ("
			+ TBL_TW_ID + " integer primary key autoincrement,"
			+ TBL_TW_COLID + " integer,"
			+ TBL_TW_SID + " text,"
			+ TBL_TW_TIME + " integer,"
			+ TBL_TW_USERNAME + " text,"
			+ TBL_TW_FULLNAME + " text,"
			+ TBL_TW_BODY + " text,"
			+ TBL_TW_AVATAR + " text,"
			+ "UNIQUE(" + TBL_TW_COLID + ", " + TBL_TW_SID + ") ON CONFLICT REPLACE"
			+ ");";

	private static final String TBL_TW_INDEX = TBL_TW + "_idx";
	private static final String TBL_TW_CREATE_INDEX = "CREATE INDEX " + TBL_TW_INDEX + " ON " + TBL_TW + "(" + TBL_TW_SID + "," + TBL_TW_TIME + ");";

	private static final String TBL_TM = "tm";
	private static final String TBL_TM_ID = "_id";
	private static final String TBL_TM_TWID = "twid";
	private static final String TBL_TM_TYPE = "type";
	private static final String TBL_TM_DATA = "data";
	private static final String TBL_TM_TITLE = "title";

	private static final String TBL_TM_CREATE = "create table " + TBL_TM + " ("
			+ TBL_TM_ID + " integer primary key autoincrement,"
			+ TBL_TM_TWID + " integer,"
			+ TBL_TM_TYPE + " integer,"
			+ TBL_TM_DATA + " text,"
			+ TBL_TM_TITLE + " text,"
			+ "FOREIGN KEY (" + TBL_TM_TWID + ") REFERENCES " + TBL_TW + " (" + TBL_TW_ID + ") ON DELETE CASCADE,"
			+ "UNIQUE(" + TBL_TM_TWID + ", " + TBL_TM_TYPE + "," + TBL_TM_DATA + "," + TBL_TM_TITLE + ") ON CONFLICT IGNORE"
			+ ");";

	private static final String TBL_TM_INDEX = TBL_TM + "_idx";
	private static final String TBL_TM_CREATE_INDEX = "CREATE INDEX " + TBL_TM_INDEX + " ON " + TBL_TM + "(" + TBL_TM_TWID + ");";

	@Override
	public void storeTweets (final Column column, final List<Tweet> tweets) {
		// Clear old data.
		this.mDb.beginTransaction();
		try {
			final int n = this.mDb.delete(TBL_TW,
					TBL_TW_COLID + "=? AND " + TBL_TW_ID + " NOT IN (SELECT " + TBL_TW_ID + " FROM " + TBL_TW +
							" WHERE " + TBL_TW_COLID + "=?" +
							" ORDER BY " + TBL_TW_TIME +
							" DESC LIMIT " + C.DATA_TW_MAX_COL_ENTRIES + ")",
					new String[] { String.valueOf(column.getId()), String.valueOf(column.getId()) });

			this.log.d("Deleted %d rows from %s column %d.", n, TBL_TW, column.getId());
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			for (final Tweet tweet : tweets) {
				values.clear();
				values.put(TBL_TW_COLID, column.getId());
				values.put(TBL_TW_SID, tweet.getSid());
				values.put(TBL_TW_TIME, tweet.getTime());
				values.put(TBL_TW_USERNAME, tweet.getUsername());
				values.put(TBL_TW_FULLNAME, tweet.getFullname());
				values.put(TBL_TW_BODY, tweet.getBody());
				values.put(TBL_TW_AVATAR, tweet.getAvatarUrl());
				final long uid = this.mDb.insertWithOnConflict(TBL_TW, null, values, SQLiteDatabase.CONFLICT_REPLACE);

				final List<Meta> metas = tweet.getMetas();
				if (metas != null) {
					for (final Meta meta : metas) {
						values.clear();
						values.put(TBL_TM_TWID, uid);
						values.put(TBL_TM_TYPE, meta.getType().getId());
						values.put(TBL_TM_DATA, meta.getData());
						if (meta.getTitle() != null) values.put(TBL_TM_TITLE, meta.getTitle());
						this.mDb.insertWithOnConflict(TBL_TM, null, values, SQLiteDatabase.CONFLICT_REPLACE);
					}
				}
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		notifyTwListeners(column.getId());
	}

	@Override
	public void deleteTweet (final Column column, final Tweet tweet) {
		this.mDb.beginTransaction();
		try {
			this.mDb.delete(TBL_TW, TBL_TW_COLID + "=? AND " + TBL_TW_SID + "=?",
					new String[] { String.valueOf(column.getId()), String.valueOf(tweet.getSid()) });
			this.log.d("Deleted tweet %s from %s column %d.", tweet.getSid(), TBL_TW, column.getId());
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		notifyTwListeners(column.getId());
	}

	@Override
	public void deleteTweets (final Column column) {
		this.mDb.beginTransaction();
		try {
			this.mDb.delete(TBL_TW, TBL_TW_COLID + "=?", new String[] { String.valueOf(column.getId()) });
			this.log.d("Deleted tweets from %s column %d.", TBL_TW, column.getId());
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		notifyTwListeners(column.getId());
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf) {
		return getTweets(TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) }, numberOf);
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf, final Set<Integer> excludeColumnIds) {
		if (excludeColumnIds == null || excludeColumnIds.size() < 1) return getTweets(columnId, numberOf);

		final StringBuilder where = new StringBuilder()
				.append(TBL_TW_COLID).append("=?")
				.append(" AND ").append(TBL_TW_SID)
				.append(" NOT IN (SELECT ").append(TBL_TW_SID)
				.append(" FROM ").append(TBL_TW)
				.append(" WHERE ");
		final String[] whereArgs = new String[1 + excludeColumnIds.size()];
		whereArgs[0] = String.valueOf(columnId);

		int i = 0;
		for (Integer id : excludeColumnIds) {
			if (i > 0) where.append(" OR ");
			where.append(TBL_TW_COLID).append("=?");
			whereArgs[1 + i] = String.valueOf(id);
			i++;
		}
		where.append(")");
		return getTweets(where.toString(), whereArgs, numberOf);
	}

	private List<Tweet> getTweets (final String where, final String[] whereArgs, final int numberOf) {
		if (!checkDbOpen()) return null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_TW,
					new String[] { TBL_TW_ID, TBL_TW_SID, TBL_TW_USERNAME, TBL_TW_FULLNAME, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR },
					where, whereArgs,
					null, null,
					TBL_TW_TIME + " desc", String.valueOf(numberOf));
			return readTweets(c);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	private static List<Tweet> readTweets (final Cursor c) {
		if (c != null && c.moveToFirst()) {
			final int colId = c.getColumnIndex(TBL_TW_ID);
			final int colSid = c.getColumnIndex(TBL_TW_SID);
			final int colUesrname = c.getColumnIndex(TBL_TW_USERNAME);
			final int colFullname = c.getColumnIndex(TBL_TW_FULLNAME);
			final int colBody = c.getColumnIndex(TBL_TW_BODY);
			final int colTime = c.getColumnIndex(TBL_TW_TIME);
			final int colAvatar = c.getColumnIndex(TBL_TW_AVATAR);

			List<Tweet> ret = new ArrayList<Tweet>();
			do {
				final long uid = c.getLong(colId);
				final String sid = c.getString(colSid);
				final String username = c.getString(colUesrname);
				final String fullname = c.getString(colFullname);
				final String body = c.getString(colBody);
				final long time = c.getLong(colTime);
				final String avatar = c.getString(colAvatar);
				ret.add(new Tweet(uid, sid, username, fullname, body, time, avatar, null));
			}
			while (c.moveToNext());
			return ret;
		}
		return Collections.EMPTY_LIST;
	}

	@Override
	public List<Tweet> findTweetsWithMeta (final MetaType metaType, final String data, final int numberOf) {
		if (!checkDbOpen()) return null;
		Cursor c = null;
		try {
			final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(TBL_TW + " INNER JOIN " + TBL_TM + " ON " + TBL_TW + "." + TBL_TW_ID + " = " + TBL_TM_TWID);
			qb.setDistinct(true);
			c = qb.query(this.mDb,
					new String[] { TBL_TW + "." + TBL_TW_ID, TBL_TW_SID, TBL_TW_USERNAME, TBL_TW_FULLNAME, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR },
					TBL_TW + "." + TBL_TW_ID + "=" + TBL_TM_TWID + " AND " + TBL_TM_TYPE + "=" + metaType.getId() + " AND " + TBL_TM_DATA + "=?",
					new String[] { data },
					TBL_TW_SID, null, TBL_TW_TIME + " desc", String.valueOf(numberOf));
			return readTweets(c);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public Tweet getTweetDetails (final String tweetSid) {
		return getTweetDetails(TBL_TW_SID + "=?", new String[] { tweetSid });
	}

	@Override
	public Tweet getTweetDetails (final int columnId, final Tweet tweet) {
		return getTweetDetails(columnId, tweet.getSid());
	}

	@Override
	public Tweet getTweetDetails (final int columnId, final String tweetSid) {
		return getTweetDetails(TBL_TW_COLID + "=? AND " + TBL_TW_SID + "=?",
				new String[] { String.valueOf(columnId), tweetSid });
	}

	@Override
	public Tweet getTweetDetails (final long tweetUid) {
		return getTweetDetails(TBL_TW_ID + "=?", new String[] { String.valueOf(tweetUid) });
	}

	private Tweet getTweetDetails (final String selection, final String[] selectionArgs) {
		if (!checkDbOpen()) return null;
		Tweet ret = null;
		Cursor c = null;
		Cursor d = null;
		try {
			c = this.mDb.query(true, TBL_TW,
					new String[] { TBL_TW_ID, TBL_TW_SID, TBL_TW_USERNAME, TBL_TW_FULLNAME, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR },
					selection, selectionArgs,
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				final int colId = c.getColumnIndex(TBL_TW_ID);
				final int colSid = c.getColumnIndex(TBL_TW_SID);
				final int colUesrname = c.getColumnIndex(TBL_TW_USERNAME);
				final int colFullname = c.getColumnIndex(TBL_TW_FULLNAME);
				final int colBody = c.getColumnIndex(TBL_TW_BODY);
				final int colTime = c.getColumnIndex(TBL_TW_TIME);
				final int colAvatar = c.getColumnIndex(TBL_TW_AVATAR);

				final long uid = c.getLong(colId);
				final String sid = c.getString(colSid);
				final String username = c.getString(colUesrname);
				final String fullname = c.getString(colFullname);
				final String body = c.getString(colBody);
				final long time = c.getLong(colTime);
				final String avatar = c.getString(colAvatar);

				List<Meta> metas = null;
				try {
					d = this.mDb.query(true, TBL_TM,
							new String[] { TBL_TM_TYPE, TBL_TM_DATA, TBL_TM_TITLE },
							TBL_TM_TWID + "=?",
							new String[] { String.valueOf(uid) },
							null, null, null, null);

					if (d != null && d.moveToFirst()) {
						final int colType = d.getColumnIndex(TBL_TM_TYPE);
						final int colData = d.getColumnIndex(TBL_TM_DATA);
						final int colTitle = d.getColumnIndex(TBL_TM_TITLE);

						metas = new ArrayList<Meta>();
						do {
							final int typeId = d.getInt(colType);
							final String data = d.getString(colData);
							final String title = d.getString(colTitle);
							metas.add(new Meta(MetaType.parseId(typeId), data, title));
						}
						while (d.moveToNext());
					}
				}
				finally {
					IoHelper.closeQuietly(d);
				}

				ret = new Tweet(uid, sid, username, fullname, body, time, avatar, metas);
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}
		return ret;
	}

	@Override
	public int getScrollUpCount (final Column column) {
		return getScrollUpCount(column.getId(), column.getExcludeColumnIds(), null);
	}

	@Override
	public int getScrollUpCount (final int columnId, final Set<Integer> excludeColumnIds, final ScrollState scroll) {
		if (!checkDbOpen()) return -1;

		final StringBuilder where = new StringBuilder()
				.append(TBL_TW_COLID).append("=?")
				.append(" AND ").append(TBL_TW_TIME).append(">?");

		final String[] whereArgs = new String[2 + (excludeColumnIds != null ? excludeColumnIds.size() : 0)];
		whereArgs[0] = String.valueOf(columnId);

		// TODO integrate into query?
		final ScrollState fscroll = scroll != null ? scroll : getScroll(columnId);
		if (fscroll == null) return 0; // Columns is probably empty.
		final Tweet tweet = getTweetDetails(fscroll.getItemId());
		if (tweet == null) return 0; // Columns is probably empty.
		whereArgs[1] = String.valueOf(tweet.getTime());

		if (excludeColumnIds != null && excludeColumnIds.size() > 0) {
			where.append(" AND ").append(TBL_TW_SID)
					.append(" NOT IN (SELECT ").append(TBL_TW_SID)
					.append(" FROM ").append(TBL_TW)
					.append(" WHERE ");

			int i = 0;
			for (Integer id : excludeColumnIds) {
				if (i > 0) where.append(" OR ");
				where.append(TBL_TW_COLID).append("=?");
				whereArgs[2 + i] = String.valueOf(id);
				i++;
			}
			where.append(")");
		}

		return (int) DatabaseUtils.queryNumEntries(this.mDb, TBL_TW, where.toString(), whereArgs);
	}

	private void notifyTwListeners (final int columnId) {
		for (final TwUpdateListener l : this.twUpdateListeners) {
			l.columnChanged(columnId);
		}
	}

	@Override
	public void addTwUpdateListener (final TwUpdateListener listener) {
		this.twUpdateListeners.add(listener);
	}

	@Override
	public void removeTwUpdateListener (final TwUpdateListener listener) {
		this.twUpdateListeners.remove(listener);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Scrolls.

	private static final String TBL_SC = "sc";
	private static final String TBL_SC_ID = "_id";
	private static final String TBL_SC_COLID = "colid";
	private static final String TBL_SC_ITEMID = "itemid";
	private static final String TBL_SC_TOP = "top";
	private static final String TBL_SC_TIME = "time";

	private static final String TBL_SC_CREATE = "create table " + TBL_SC + " ("
			+ TBL_SC_ID + " integer primary key autoincrement,"
			+ TBL_SC_COLID + " integer,"
			+ TBL_SC_ITEMID + " integer,"
			+ TBL_SC_TOP + " integer,"
			+ TBL_SC_TIME + " integer,"
			+ "UNIQUE(" + TBL_SC_COLID + ") ON CONFLICT REPLACE" +
			");";

	@Override
	public void storeScroll (final int columnId, final ScrollState state) {
		if (state == null) return;

		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			values.put(TBL_SC_COLID, columnId);
			values.put(TBL_SC_ITEMID, state.getItemId());
			values.put(TBL_SC_TOP, state.getTop());
			values.put(TBL_SC_TIME, state.getItemTime());
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
					new String[] { TBL_SC_ITEMID, TBL_SC_TOP, TBL_SC_TIME },
					TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) },
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				final int colItemId = c.getColumnIndex(TBL_SC_ITEMID);
				final int colTop = c.getColumnIndex(TBL_SC_TOP);
				final int colTime = c.getColumnIndex(TBL_SC_TIME);

				final long itemId = c.getLong(colItemId);
				final int top = c.getInt(colTop);
				final long time = c.getLong(colTime);
				ret = new ScrollState(itemId, top, time);
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}

		this.log.d("Read scroll for col %d: %s", columnId, ret);
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String TBL_KV = "kv";
	private static final String TBL_KV_ID = "_id";
	private static final String TBL_KV_KEY = "key";
	private static final String TBL_KV_VAL = "val";

	private static final String TBL_KV_CREATE = "create table " + TBL_KV + " ("
			+ TBL_KV_ID + " integer primary key autoincrement,"
			+ TBL_KV_KEY + " text,"
			+ TBL_KV_VAL + " text,"
			+ "UNIQUE(" + TBL_KV_KEY + ") ON CONFLICT REPLACE" +
			");";

	private static final String TBL_KV_INDEX = TBL_KV + "_idx";
	private static final String TBL_KV_CREATE_INDEX = "CREATE INDEX " + TBL_KV_INDEX + " ON " + TBL_KV + "(" + TBL_KV_KEY + ");";

	@Override
	public void storeValue (final String key, final String value) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			values.put(TBL_KV_KEY, key);
			values.put(TBL_KV_VAL, value);
			this.mDb.insertWithOnConflict(TBL_KV, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		if (value == null) {
			this.log.d("Stored KV: '%s' = null.", key);
		}
		else {
			this.log.d("Stored KV: '%s' = '%s'.", key, value);
		}
	}

	@Override
	public String getValue (final String key) {
		if (!checkDbOpen()) return null;
		String ret = null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_KV,
					new String[] { TBL_KV_VAL },
					TBL_KV_KEY + "=?", new String[] { key },
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				final int colVal = c.getColumnIndex(TBL_KV_VAL);
				ret = c.getString(colVal);
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}
		if (ret == null) {
			this.log.d("Read KV: '%s' = null.", key);
		}
		else {
			this.log.d("Read KV: '%s' = '%s'.", key, ret);
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

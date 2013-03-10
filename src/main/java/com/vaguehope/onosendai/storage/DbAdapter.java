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
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.LogWrapper;

public class DbAdapter implements DbInterface {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DB_NAME = "tweets";
	private static final int DB_VERSION = 2;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final LogWrapper log = new LogWrapper();
	private final Context mCtx;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final List<Runnable> twUpdateActions = new ArrayList<Runnable>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final LogWrapper log = new LogWrapper();

		DatabaseHelper (final Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate (final SQLiteDatabase db) {
			db.execSQL(TBL_TW_CREATE);
		}

		@Override
		public void onUpgrade (final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			this.log.w("Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS " + TBL_TW);
			onCreate(db);
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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity.

	private static final String TBL_TW = "tw";
	private static final String TBL_TW_ID = "_id";
	private static final String TBL_TW_COLID = "colid";
	private static final String TBL_TW_SID = "sid";
	private static final String TBL_TW_TIME = "time";
	private static final String TBL_TW_NAME = "name";
	private static final String TBL_TW_BODY = "body";

	private static final String TBL_TW_CREATE = "create table " + TBL_TW + " ("
			+ TBL_TW_ID + " integer primary key autoincrement,"
			+ TBL_TW_COLID + " integer,"
			+ TBL_TW_SID + " integer,"
			+ TBL_TW_TIME + " integer,"
			+ TBL_TW_NAME + " text,"
			+ TBL_TW_BODY + " text,"
			+ "UNIQUE(" + TBL_TW_COLID + ", " + TBL_TW_SID + ") ON CONFLICT REPLACE" +
			");";

	// TODO add table index by time?

	@Override
	public void storeTweets (final Column column, final List<Tweet> tweets) {
		// Clear old data.
		this.mDb.beginTransaction();
		try {
//			int n = this.mDb.delete(TBL_TW,
//					"date('now', '?') > datetime(" + TBL_TW_TIME + ", 'unixepoch')",
//					new String[] { C.DATA_TW_MAX_AGE_DAYS });

			int n = this.mDb.delete(TBL_TW,
					TBL_TW_COLID + "=? AND " + TBL_TW_ID + " NOT IN (SELECT " + TBL_TW_ID + " FROM " + TBL_TW +
							" WHERE " + TBL_TW_COLID + "=?" +
							" ORDER BY " + TBL_TW_TIME +
							" DESC LIMIT " + C.DATA_TW_MAX_COL_ENTRIES + ")",
					new String[] { String.valueOf(column.index), String.valueOf(column.index) });

			this.log.i("Deleted " + n + " rows from " + TBL_TW + " column " + column.index + ".");
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		this.mDb.beginTransaction();
		try {
			for (Tweet tweet : tweets) {
				ContentValues values = new ContentValues();
				values.put(TBL_TW_COLID, column.index);
				values.put(TBL_TW_SID, tweet.getId());
				values.put(TBL_TW_TIME, tweet.getTime());
				values.put(TBL_TW_NAME, tweet.getUsername());
				values.put(TBL_TW_BODY, tweet.getBody());
				this.mDb.insertWithOnConflict(TBL_TW, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			}
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}

		notifyTwListeners();
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf) {
		if (this.mDb == null) {
			this.log.e("aborting because mDb==null.");
			return null;
		}

		if (!this.mDb.isOpen()) {
			this.log.d("mDb was not open; opeing it...");
			open();
		}

		List<Tweet> ret = new ArrayList<Tweet>();

		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_TW,
					new String[] { TBL_TW_SID, TBL_TW_NAME, TBL_TW_BODY, TBL_TW_TIME },
					TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) },
					null, null,
					TBL_TW_TIME + " desc", String.valueOf(numberOf));

			if (c != null && c.moveToFirst()) {
				int col_sid = c.getColumnIndex(TBL_TW_SID);
				int col_name = c.getColumnIndex(TBL_TW_NAME);
				int col_body = c.getColumnIndex(TBL_TW_BODY);
				int col_time = c.getColumnIndex(TBL_TW_TIME);

				ret = new ArrayList<Tweet>();
				do {
					long sid = c.getLong(col_sid);
					String name = c.getString(col_name);
					String body = c.getString(col_body);
					long time = c.getLong(col_time);
					ret.add(new Tweet(sid, name, body, time));
				}
				while (c.moveToNext());
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
}

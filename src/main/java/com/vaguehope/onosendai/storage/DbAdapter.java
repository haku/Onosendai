package com.vaguehope.onosendai.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxAction;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class DbAdapter implements DbInterface {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DB_NAME = "tweets";
	private static final int DB_VERSION = 18;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final LogWrapper log = new LogWrapper("DB"); // TODO make static?
	private final Context mCtx;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private final Map<Integer, ColumnState> columnStates = new ConcurrentHashMap<Integer, ColumnState>();
	private final List<TwUpdateListener> twUpdateListeners = new ArrayList<TwUpdateListener>();
	private final List<OutboxListener> outboxListeners = new ArrayList<OutboxListener>();

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
			db.execSQL(TBL_OB_CREATE);
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
				if (oldVersion < 12) { // NOSONAR not a magic number.
					this.log.w("Adding column %s...", TBL_SC_UNREAD);
					db.execSQL("ALTER TABLE " + TBL_SC + " ADD COLUMN " + TBL_SC_UNREAD + " integer;");
				}
				// 13 and 14 got merged into 15.
				if (oldVersion < 15) { // NOSONAR not a magic number.
					this.log.w("Creating table %s...", TBL_OB);
					if (!isTableExists(db, TBL_OB)) db.execSQL(TBL_OB_CREATE);
				}
				if (oldVersion < 16) { // NOSONAR not a magic number.
					this.log.w("Adding column %s...", TBL_TW_INLINEMEDIA);
					db.execSQL("ALTER TABLE " + TBL_TW + " ADD COLUMN " + TBL_TW_INLINEMEDIA + " text;");
				}
				if (oldVersion < 17) { // NOSONAR not a magic number.
					this.log.w("Adding column %s...", TBL_OB_ACTION);
					db.execSQL("ALTER TABLE " + TBL_OB + " ADD COLUMN " + TBL_OB_ACTION + " integer;");
				}
				if (oldVersion < 18) { // NOSONAR not a magic number.
					this.log.w("Adding column %s...", TBL_TW_USERSUBTITLE);
					db.execSQL("ALTER TABLE " + TBL_TW + " ADD COLUMN " + TBL_TW_USERSUBTITLE + " text;");
					this.log.w("Adding column %s...", TBL_TW_FULLSUBTITLE);
					db.execSQL("ALTER TABLE " + TBL_TW + " ADD COLUMN " + TBL_TW_FULLSUBTITLE + " text;");
				}
			}
		}

		@Override
		public void onOpen (final SQLiteDatabase db) {
			super.onOpen(db);
			if (!db.isReadOnly()) {
				db.execSQL("PRAGMA foreign_keys=ON;");
				this.log.i("foreign_keys=ON");
			}
		}

		private static boolean isTableExists (final SQLiteDatabase db, final String tableName) {
			Cursor c = null;
			try {
				c = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name=?", new String[] { tableName });
				if (c.getCount() > 0) return true;
				return false;
			}
			finally {
				IoHelper.closeQuietly(c);
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
	protected static final String TBL_TW_SID = "sid";
	protected static final String TBL_TW_TIME = "time";
	protected static final String TBL_TW_USERNAME = "uname";
	protected static final String TBL_TW_FULLNAME = "fname";
	protected static final String TBL_TW_USERSUBTITLE = "usub";
	protected static final String TBL_TW_FULLSUBTITLE = "fsub";
	protected static final String TBL_TW_BODY = "body";
	protected static final String TBL_TW_AVATAR = "avatar";
	protected static final String TBL_TW_INLINEMEDIA = "imedia";

	private static final String TBL_TW_CREATE = "create table " + TBL_TW + " ("
			+ TBL_TW_ID + " integer primary key autoincrement,"
			+ TBL_TW_COLID + " integer,"
			+ TBL_TW_SID + " text,"
			+ TBL_TW_TIME + " integer,"
			+ TBL_TW_USERNAME + " text,"
			+ TBL_TW_FULLNAME + " text,"
			+ TBL_TW_USERSUBTITLE + " text,"
			+ TBL_TW_FULLSUBTITLE + " text,"
			+ TBL_TW_BODY + " text,"
			+ TBL_TW_AVATAR + " text,"
			+ TBL_TW_INLINEMEDIA + " text,"
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
		storeTweets(column.getId(), tweets);
	}

	@Override
	public void storeTweets (final int columnId, final List<Tweet> tweets) {
		// Clear old data.
		this.mDb.beginTransaction();
		try {
			final int n = this.mDb.delete(TBL_TW,
					TBL_TW_COLID + "=? AND " + TBL_TW_ID + " NOT IN (SELECT " + TBL_TW_ID + " FROM " + TBL_TW +
							" WHERE " + TBL_TW_COLID + "=?" +
							" ORDER BY " + TBL_TW_TIME +
							" DESC LIMIT " + C.DATA_TW_MAX_COL_ENTRIES + ")",
					new String[] { String.valueOf(columnId), String.valueOf(columnId) });

			this.log.d("Deleted %d rows from %s column %d.", n, TBL_TW, columnId);
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
				values.put(TBL_TW_COLID, columnId);
				values.put(TBL_TW_SID, tweet.getSid());
				values.put(TBL_TW_TIME, tweet.getTime());
				values.put(TBL_TW_USERNAME, tweet.getUsername());
				values.put(TBL_TW_FULLNAME, tweet.getFullname());
				values.put(TBL_TW_USERSUBTITLE, tweet.getUserSubtitle());
				values.put(TBL_TW_FULLSUBTITLE, tweet.getFullSubtitle());
				values.put(TBL_TW_BODY, tweet.getBody());
				values.put(TBL_TW_AVATAR, tweet.getAvatarUrl());
				values.put(TBL_TW_INLINEMEDIA, tweet.getInlineMediaUrl());
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

		notifyTwListenersColumnChanged(columnId);
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
		notifyTwListenersColumnChanged(column.getId());
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
		notifyTwListenersColumnChanged(column.getId());
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf) {
		return getTweets(TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) }, TBL_TW_TIME + " desc", numberOf, false);
	}

	@Override
	public List<Tweet> getTweets (final int columnId, final int numberOf, final Set<Integer> excludeColumnIds) {
		if (excludeColumnIds == null || excludeColumnIds.size() < 1) return getTweets(columnId, numberOf);
		final Cursor c = getTweetsCursor(columnId, excludeColumnIds, false, numberOf);
		try {
			return readTweets(c, false);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public Cursor getTweetsCursor (final int columnId) {
		return getTweetsCursor(TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) }, TBL_TW_TIME + " desc", -1);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId, final boolean withInlineMediaOnly) {
		if (!withInlineMediaOnly) return getTweetsCursor(columnId);
		return getTweetsCursor(TBL_TW_COLID + "=? AND " + TBL_TW_INLINEMEDIA + " NOT NULL", new String[] { String.valueOf(columnId) }, TBL_TW_TIME + " desc", -1);
	}

	@Override
	public Cursor getTweetsCursor (final int columnId, final Set<Integer> excludeColumnIds, final boolean withInlineMediaOnly) {
		if (excludeColumnIds == null || excludeColumnIds.size() < 1) return getTweetsCursor(columnId, withInlineMediaOnly);
		return getTweetsCursor(columnId, excludeColumnIds, withInlineMediaOnly, -1);
	}

	private Cursor getTweetsCursor (final int columnId, final Set<Integer> excludeColumnIds, final boolean withInlineMediaOnly, final int numberOf) {
		final StringBuilder where = new StringBuilder()
				.append(TBL_TW_COLID).append("=?");

		if (withInlineMediaOnly) where
				.append(" AND ").append(TBL_TW_INLINEMEDIA).append(" NOT NULL");

		where.append(" AND ").append(TBL_TW_SID)
				.append(" NOT IN (SELECT ").append(TBL_TW_SID)
				.append(" FROM ").append(TBL_TW)
				.append(" WHERE ");
		final String[] whereArgs = new String[1 + excludeColumnIds.size()];
		whereArgs[0] = String.valueOf(columnId);

		int i = 0;
		for (final Integer id : excludeColumnIds) {
			if (i > 0) where.append(" OR ");
			where.append(TBL_TW_COLID).append("=?");
			whereArgs[1 + i] = String.valueOf(id);
			i++;
		}
		where.append(")");
		return getTweetsCursor(where.toString(), whereArgs, TBL_TW_TIME + " desc", numberOf);
	}

	@Override
	public List<Tweet> getTweetsSinceTime (final int columnId, final long earliestTime, final int numberOf) {
		return getTweets(new StringBuilder()
				.append(TBL_TW_COLID).append("=?")
				.append(" AND ").append(TBL_TW_TIME).append(">?").toString(),
				new String[] { String.valueOf(columnId), String.valueOf(earliestTime) },
				TBL_TW_TIME + " asc",
				numberOf, false);
	}

	private List<Tweet> getTweets (final String where, final String[] whereArgs, final String orderBy, final int numberOf, final boolean addColumMeta) {
		final Cursor c = getTweetsCursor(where, whereArgs, orderBy, numberOf);
		try {
			return readTweets(c, addColumMeta);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	private Cursor getTweetsCursor (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		if (!checkDbOpen()) return null;
		return this.mDb.query(true, TBL_TW,
				new String[] { TBL_TW_ID, TBL_TW_SID, TBL_TW_USERNAME, TBL_TW_FULLNAME, TBL_TW_USERSUBTITLE, TBL_TW_FULLSUBTITLE, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR, TBL_TW_INLINEMEDIA, TBL_TW_COLID },
				where, whereArgs,
				null, null,
				orderBy,
				numberOf > 0 ? String.valueOf(numberOf) : null);
	}

	private static List<Tweet> readTweets (final Cursor c, final boolean addColumMeta) {
		if (c != null && c.moveToFirst()) {
			final int colId = c.getColumnIndex(TBL_TW_ID);
			final int colSid = c.getColumnIndex(TBL_TW_SID);
			final int colUsername = c.getColumnIndex(TBL_TW_USERNAME);
			final int colFullname = c.getColumnIndex(TBL_TW_FULLNAME);
			final int colUserSubtitle = c.getColumnIndex(TBL_TW_USERSUBTITLE);
			final int colFullSubtitle = c.getColumnIndex(TBL_TW_FULLSUBTITLE);
			final int colBody = c.getColumnIndex(TBL_TW_BODY);
			final int colTime = c.getColumnIndex(TBL_TW_TIME);
			final int colAvatar = c.getColumnIndex(TBL_TW_AVATAR);
			final int colInlineMedia = c.getColumnIndex(TBL_TW_INLINEMEDIA);
			final int colColId = c.getColumnIndex(TBL_TW_COLID);

			final List<Tweet> ret = new ArrayList<Tweet>();
			do {
				final long uid = c.getLong(colId);
				final String sid = c.getString(colSid);
				final String username = c.getString(colUsername);
				final String fullname = c.getString(colFullname);
				final String userSubtitle = c.getString(colUserSubtitle);
				final String fullSubtitle = c.getString(colFullSubtitle);
				final String body = c.getString(colBody);
				final long time = c.getLong(colTime);
				final String avatar = c.getString(colAvatar);
				final String inlineMedia = c.getString(colInlineMedia);
				List<Meta> metas = null;
				if (addColumMeta) {
					metas = Collections.singletonList(new Meta(MetaType.COLUMN_ID, String.valueOf(c.getInt(colColId))));
				}
				ret.add(new Tweet(uid, sid, username, fullname, userSubtitle, fullSubtitle, body, time, avatar, inlineMedia, metas));
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
					new String[] { TBL_TW + "." + TBL_TW_ID, TBL_TW_SID, TBL_TW_USERNAME, TBL_TW_FULLNAME, TBL_TW_USERSUBTITLE, TBL_TW_FULLSUBTITLE, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR, TBL_TW_INLINEMEDIA },
					TBL_TW + "." + TBL_TW_ID + "=" + TBL_TM_TWID + " AND " + TBL_TM_TYPE + "=" + metaType.getId() + " AND " + TBL_TM_DATA + "=?",
					new String[] { data },
					TBL_TW_SID, null, TBL_TW_TIME + " desc", String.valueOf(numberOf));
			return readTweets(c, false);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public List<Tweet> searchTweets (final String searchTerm, final List<Column> columns, final int numberOf) {
		// WHERE body LIKE ? ESCAPE ? COLLATE NOCASE
		//   AND (
		//          (colid=? [AND sid NOT IN (SELECT sid FROM tw WHERE colid=? [OR colid=?]]))
		//     [ OR (colid=? [AND sid NOT IN (SELECT sid FROM tw WHERE colid=? [OR colid=?]]))]
		//   )

		final StringBuilder where = new StringBuilder()
				.append(TBL_TW_BODY).append(" LIKE ? ESCAPE ? COLLATE NOCASE");
		final List<String> whereArgs = new ArrayList<String>();
		whereArgs.add("%" + escapeSearch(searchTerm) + "%");
		whereArgs.add(SEARCH_ESC);

		if (columns != null && columns.size() > 0) {
			int columnI = -1;
			for (final Column column : columns) {
				final Set<Integer> excludeColumnIds = column.getExcludeColumnIds();

				columnI++;
				if (columnI == 0) {
					where.append(" AND (");
				}
				else {
					where.append(" OR");
				}

				where.append(" (")
						.append(TBL_TW_COLID).append("=?");
				whereArgs.add(String.valueOf(column.getId()));

				if (excludeColumnIds != null && excludeColumnIds.size() > 0) {
					where.append(" AND ").append(TBL_TW_SID)
					.append(" NOT IN (SELECT ").append(TBL_TW_SID)
					.append(" FROM ").append(TBL_TW)
					.append(" WHERE ");

					int excludeI = 0;
					for (final Integer id : excludeColumnIds) {
						if (excludeI > 0) where.append(" OR ");
						where.append(TBL_TW_COLID).append("=?");
						whereArgs.add(String.valueOf(id));
						excludeI++;
					}
					where.append(")");
				}

				where.append(")");
			}
			if (columnI >= 0) where.append(")");
		}

		//this.log.i("search: %s %s", where, whereArgs);

		return getTweets(where.toString(), whereArgs.toArray(new String[whereArgs.size()]),
				TBL_TW_TIME + " desc", numberOf, true);
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
					new String[] { TBL_TW_ID, TBL_TW_SID, TBL_TW_USERNAME, TBL_TW_FULLNAME, TBL_TW_USERSUBTITLE, TBL_TW_FULLSUBTITLE, TBL_TW_BODY, TBL_TW_TIME, TBL_TW_AVATAR, TBL_TW_INLINEMEDIA },
					selection, selectionArgs,
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				final int colId = c.getColumnIndex(TBL_TW_ID);
				final int colSid = c.getColumnIndex(TBL_TW_SID);
				final int colUesrname = c.getColumnIndex(TBL_TW_USERNAME);
				final int colFullname = c.getColumnIndex(TBL_TW_FULLNAME);
				final int colUserSubtitle = c.getColumnIndex(TBL_TW_USERSUBTITLE);
				final int colFullSubtitle = c.getColumnIndex(TBL_TW_FULLSUBTITLE);
				final int colBody = c.getColumnIndex(TBL_TW_BODY);
				final int colTime = c.getColumnIndex(TBL_TW_TIME);
				final int colAvatar = c.getColumnIndex(TBL_TW_AVATAR);
				final int colInlineMedia = c.getColumnIndex(TBL_TW_INLINEMEDIA);

				final long uid = c.getLong(colId);
				final String sid = c.getString(colSid);
				final String username = c.getString(colUesrname);
				final String fullname = c.getString(colFullname);
				final String userSubtitle = c.getString(colUserSubtitle);
				final String fullSubtitle = c.getString(colFullSubtitle);
				final String body = c.getString(colBody);
				final long time = c.getLong(colTime);
				final String avatar = c.getString(colAvatar);
				final String inlineMedia = c.getString(colInlineMedia);

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

				ret = new Tweet(uid, sid, username, fullname, userSubtitle, fullSubtitle, body, time, avatar, inlineMedia, metas);
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}
		return ret;
	}

	@Override
	public List<String> getUsernames (final int numberOf) {
		return getUsernames(TBL_TW_USERNAME + " NOT NULL", null, TBL_TW_TIME + " desc", numberOf);
	}

	@Override
	public List<String> getUsernames (final String prefix, final int numberOf) {
		return getUsernames(TBL_TW_USERNAME + " LIKE ? ESCAPE ? COLLATE NOCASE",
				new String[] { escapeSearch(prefix).concat("%"), SEARCH_ESC },
				TBL_TW_USERNAME + " asc", numberOf);
	}

	private List<String> getUsernames (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		if (!checkDbOpen()) return null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_TW,
					new String[] { TBL_TW_USERNAME },
					where, whereArgs,
					null, null,
					orderBy, String.valueOf(numberOf));
			return columnToStringList(c, TBL_TW_USERNAME);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public List<String> getHashtags (final String prefix, final int numberOf) {
		return getMetadatas(TBL_TM_TYPE + "=? AND " + TBL_TM_DATA + " LIKE ? ESCAPE ? COLLATE NOCASE",
				new String[] { String.valueOf(MetaType.HASHTAG.getId()), escapeSearch(prefix).concat("%"), SEARCH_ESC },
				TBL_TM_DATA + " asc", numberOf);
	}

	private List<String> getMetadatas (final String where, final String[] whereArgs, final String orderBy, final int numberOf) {
		if (!checkDbOpen()) return null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_TM,
					new String[] { TBL_TM_DATA },
					where, whereArgs,
					null, null,
					orderBy, String.valueOf(numberOf));
			return columnToStringList(c, TBL_TM_DATA);
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	private static List<String> columnToStringList (final Cursor c, final String column) {
		if (c != null && c.moveToFirst()) {
			final int colIndex = c.getColumnIndex(column);
			final List<String> ret = new ArrayList<String>();
			do {
				ret.add(c.getString(colIndex));
			}
			while (c.moveToNext());
			return ret;
		}
		return Collections.EMPTY_LIST;
	}

	@Override
	public int getUnreadCount (final Column column) {
		return getUpCount(UpCountType.UNREAD, column);
	}

	@Override
	public int getUnreadCount (final int columnId, final Set<Integer> excludeColumnIds, final ScrollState scroll) {
		return getUpCount(UpCountType.UNREAD, columnId, excludeColumnIds, false, scroll);
	}

	@Override
	public int getScrollUpCount (final Column column) {
		return getUpCount(UpCountType.SCROLL, column);
	}

	@Override
	public int getScrollUpCount (final int columnId, final Set<Integer> excludeColumnIds, final boolean withInlineMediaOnly, final ScrollState scroll) {
		return getUpCount(UpCountType.SCROLL, columnId, excludeColumnIds, withInlineMediaOnly, scroll);
	}

	private static enum UpCountType {
		UNREAD {
			@Override
			public long getTime (final ScrollState ss) {
				return ss.getUnreadTime();
			}
		},
		SCROLL {
			@Override
			public long getTime (final ScrollState ss) {
				return ss.getItemTime();
			}
		};
		public abstract long getTime (ScrollState ss);
	}

	public int getUpCount (final UpCountType type, final Column column) {
		return getUpCount(type, column.getId(), column.getExcludeColumnIds(), false, null);
	}

	public int getUpCount (final UpCountType type, final int columnId, final Set<Integer> excludeColumnIds, final boolean withInlineMediaOnly, final ScrollState scroll) {
		if (!checkDbOpen()) return -1;

		final StringBuilder where = new StringBuilder()
				.append(TBL_TW_COLID).append("=?");

		if (withInlineMediaOnly) where
				.append(" AND ").append(TBL_TW_INLINEMEDIA).append(" NOT NULL");

		where
				.append(" AND ").append(TBL_TW_TIME).append(">?");

		final String[] whereArgs = new String[2 + (excludeColumnIds != null ? excludeColumnIds.size() : 0)];
		whereArgs[0] = String.valueOf(columnId);

		// TODO integrate into query?
		final ScrollState fscroll = scroll != null ? scroll : getScroll(columnId);
		if (fscroll == null) return 0; // Columns is probably empty.
		final long time = type.getTime(fscroll);
		if (time < 1L) return 0;
		whereArgs[1] = String.valueOf(time);

		if (excludeColumnIds != null && excludeColumnIds.size() > 0) {
			where.append(" AND ").append(TBL_TW_SID)
					.append(" NOT IN (SELECT ").append(TBL_TW_SID)
					.append(" FROM ").append(TBL_TW)
					.append(" WHERE ");

			int i = 0;
			for (final Integer id : excludeColumnIds) {
				if (i > 0) where.append(" OR ");
				where.append(TBL_TW_COLID).append("=?");
				whereArgs[2 + i] = String.valueOf(id);
				i++;
			}
			where.append(")");
		}

		return (int) DatabaseUtils.queryNumEntries(this.mDb, TBL_TW, where.toString(), whereArgs);
	}

	private void notifyTwListenersColumnChanged (final int columnId) {
		for (final TwUpdateListener l : this.twUpdateListeners) {
			l.columnChanged(columnId);
		}
	}

	private void notifyTwListenersUnreadChanged (final int columnId) {
		for (final TwUpdateListener l : this.twUpdateListeners) {
			l.unreadChanged(columnId);
		}
	}

	@Override
	public void notifyTwListenersColumnState (final int columnId, final ColumnState state) {
		this.columnStates.put(Integer.valueOf(columnId), state);
		for (final TwUpdateListener l : this.twUpdateListeners) {
			l.columnStatus(columnId, state);
		}
	}

	@Override
	public void addTwUpdateListener (final TwUpdateListener listener) {
		for (final Entry<Integer, ColumnState> e : this.columnStates.entrySet()) {
			listener.columnStatus(e.getKey().intValue(), e.getValue());
		}
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
	private static final String TBL_SC_UNREAD = "unread";

	private static final String TBL_SC_CREATE = "create table " + TBL_SC + " ("
			+ TBL_SC_ID + " integer primary key autoincrement,"
			+ TBL_SC_COLID + " integer,"
			+ TBL_SC_ITEMID + " integer,"
			+ TBL_SC_TOP + " integer,"
			+ TBL_SC_TIME + " integer,"
			+ TBL_SC_UNREAD + " integer,"
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
			values.put(TBL_SC_UNREAD, state.getUnreadTime());
			this.mDb.insertWithOnConflict(TBL_SC, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.log.d("Stored scroll for col %d: %s", columnId, state);
	}

	@Override
	public void storeUnreadTime (final int columnId, final long unreadTime) {
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			values.put(TBL_SC_UNREAD, unreadTime);
			final int affected = this.mDb.update(TBL_SC, values, TBL_SC_COLID + "=?", new String[] { String.valueOf(columnId) });
			if (affected > 1) throw new IllegalStateException("Updating " + columnId + " unreadTime affected " + affected + " rows, expected 1.");
			if (affected < 1) this.log.w("Updating %s unreadTime to %s affected %s rows, expected 1.", columnId, unreadTime, affected);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.log.d("Stored unreadTime for col %d: %s", columnId, unreadTime);
		notifyTwListenersUnreadChanged(columnId);
	}

	@Override
	public ScrollState getScroll (final int columnId) {
		if (!checkDbOpen()) return null;
		ScrollState ret = null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_SC,
					new String[] { TBL_SC_ITEMID, TBL_SC_TOP, TBL_SC_TIME, TBL_SC_UNREAD },
					TBL_TW_COLID + "=?", new String[] { String.valueOf(columnId) },
					null, null, null, null);

			if (c != null && c.moveToFirst()) {
				final int colItemId = c.getColumnIndex(TBL_SC_ITEMID);
				final int colTop = c.getColumnIndex(TBL_SC_TOP);
				final int colTime = c.getColumnIndex(TBL_SC_TIME);
				final int colUnread = c.getColumnIndex(TBL_SC_UNREAD);

				final long itemId = c.getLong(colItemId);
				final int top = c.getInt(colTop);
				final long time = c.getLong(colTime);
				final long unread = c.getLong(colUnread);
				ret = new ScrollState(itemId, top, time, unread);
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}

		this.log.d("Read scroll for col %d: %s", columnId, ret);
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Outbox.

	private static final String TBL_OB = "ob";
	private static final String TBL_OB_ID = "_id";
	private static final String TBL_OB_ACTION = "action";
	private static final String TBL_OB_ACCOUNT_ID = "actid";
	private static final String TBL_OB_SERVICES = "svcs";
	private static final String TBL_OB_BODY = "body";
	private static final String TBL_OB_IN_REPLY_TO_SID = "repsid";
	private static final String TBL_OB_ATTACHMENT = "atch";
	private static final String TBL_OB_STATUS = "stat";
	private static final String TBL_OB_ATTEMPT_COUNT = "atct";
	private static final String TBL_OB_LAST_ERROR = "err";

	private static final String TBL_OB_CREATE = "create table " + TBL_OB + " ("
			+ TBL_OB_ID + " integer primary key autoincrement,"
			+ TBL_OB_ACTION + " integer,"
			+ TBL_OB_ACCOUNT_ID + " text,"
			+ TBL_OB_SERVICES + " text,"
			+ TBL_OB_BODY + " text,"
			+ TBL_OB_IN_REPLY_TO_SID + " text,"
			+ TBL_OB_ATTACHMENT + " text,"
			+ TBL_OB_STATUS + " integer,"
			+ TBL_OB_ATTEMPT_COUNT + " integer,"
			+ TBL_OB_LAST_ERROR + " text"
			+ ");";

	@Override
	public void addPostToOutput (final OutboxTweet ot) {
		if (ot.getUid() != null) throw new IllegalArgumentException("Can not add entry that is already in DB.");
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			values.put(TBL_OB_ACTION, ot.getAction().getCode());
			values.put(TBL_OB_ACCOUNT_ID, ot.getAccountId());
			values.put(TBL_OB_SERVICES, ot.getSvcMetasStr());
			values.put(TBL_OB_BODY, ot.getBody());
			values.put(TBL_OB_IN_REPLY_TO_SID, ot.getInReplyToSid());
			values.put(TBL_OB_ATTACHMENT, ot.getAttachmentStr());
			values.put(TBL_OB_STATUS, ot.getStatusCode());
			values.put(TBL_OB_ATTEMPT_COUNT, ot.getAttemptCount());
			values.put(TBL_OB_LAST_ERROR, ot.getLastError());
			this.mDb.insert(TBL_OB, null, values);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.log.d("Stored in outbox: %s", ot);
		notifyOutboxListeners();
	}

	@Override
	public void updateOutboxEntry (final OutboxTweet ot) {
		final Long uid = ot.getUid();
		if (uid == null) throw new IllegalArgumentException("Can not update entry that is not already in DB.");
		this.mDb.beginTransaction();
		try {
			final ContentValues values = new ContentValues();
			values.put(TBL_OB_ACTION, ot.getAction().getCode());
			values.put(TBL_OB_ACCOUNT_ID, ot.getAccountId());
			values.put(TBL_OB_SERVICES, ot.getSvcMetasStr());
			values.put(TBL_OB_BODY, ot.getBody());
			values.put(TBL_OB_IN_REPLY_TO_SID, ot.getInReplyToSid());
			values.put(TBL_OB_ATTACHMENT, ot.getAttachmentStr());
			values.put(TBL_OB_STATUS, ot.getStatusCode());
			values.put(TBL_OB_ATTEMPT_COUNT, ot.getAttemptCount());
			values.put(TBL_OB_LAST_ERROR, ot.getLastError());
			final int affected = this.mDb.update(TBL_OB, values, TBL_OB_ID + "=?", new String[] { String.valueOf(uid) });
			if (affected > 1) throw new IllegalStateException("Updating " + ot + " affected " + affected + " rows, expected 1.");
			if (affected < 1) this.log.w("Updating outbox entry %s affected %s rows, expected 1.", ot, affected);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		this.log.d("Updated in outbox: %s", ot);
		notifyOutboxListeners();
	}

	@Override
	public List<OutboxTweet> getOutboxEntries () {
		return getOutboxEntries(null, null);
	}

	@Override
	public List<OutboxTweet> getOutboxEntries (final OutboxTweetStatus status) {
		if (status == null) throw new IllegalArgumentException("status can not be null.");
		return getOutboxEntries(TBL_OB_STATUS + "=?", new String[] { String.valueOf(status.getCode()) });
	}

	private List<OutboxTweet> getOutboxEntries (final String where, final String[] whereArgs) {
		if (!checkDbOpen()) return null;
		Cursor c = null;
		try {
			c = this.mDb.query(true, TBL_OB,
					new String[] { TBL_OB_ID, TBL_OB_ACTION, TBL_OB_ACCOUNT_ID, TBL_OB_SERVICES, TBL_OB_BODY, TBL_OB_IN_REPLY_TO_SID, TBL_OB_ATTACHMENT,
							TBL_OB_STATUS, TBL_OB_ATTEMPT_COUNT, TBL_OB_LAST_ERROR },
					where, whereArgs,
					null, null,
					TBL_OB_ID + " asc", null);

			if (c != null && c.moveToFirst()) {
				final int colId = c.getColumnIndex(TBL_OB_ID);
				final int colAction = c.getColumnIndex(TBL_OB_ACTION);
				final int colAccountId = c.getColumnIndex(TBL_OB_ACCOUNT_ID);
				final int colServices = c.getColumnIndex(TBL_OB_SERVICES);
				final int colBody = c.getColumnIndex(TBL_OB_BODY);
				final int colInReplyToSid = c.getColumnIndex(TBL_OB_IN_REPLY_TO_SID);
				final int colAttachment = c.getColumnIndex(TBL_OB_ATTACHMENT);
				final int colStatus = c.getColumnIndex(TBL_OB_STATUS);
				final int colAttemptCount = c.getColumnIndex(TBL_OB_ATTEMPT_COUNT);
				final int colLastError = c.getColumnIndex(TBL_OB_LAST_ERROR);

				final List<OutboxTweet> ret = new ArrayList<OutboxTweet>();
				do {
					final long uid = c.getLong(colId);
					final OutboxAction action = OutboxAction.parseCode(c.getInt(colAction));
					final String accountId = c.getString(colAccountId);
					final String svcMetas = c.getString(colServices);
					final String body = c.getString(colBody);
					final String inReplyToSid = c.getString(colInReplyToSid);
					final String attachment = c.getString(colAttachment);
					final Integer status = c.getInt(colStatus);
					final Integer attemptCount = c.getInt(colAttemptCount);
					final String lastError = c.getString(colLastError);
					ret.add(new OutboxTweet(uid, action, accountId, svcMetas, body, inReplyToSid, attachment,
							status, attemptCount, lastError));
				}
				while (c.moveToNext());
				return ret;
			}
			return Collections.EMPTY_LIST;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	@Override
	public void deleteFromOutbox (final OutboxTweet ot) {
		final Long uid = ot.getUid();
		if (uid == null) throw new IllegalArgumentException("Must specify UID to delete from outbox.");
		this.mDb.beginTransaction();
		try {
			this.mDb.delete(TBL_OB, TBL_OB_ID + "=?", new String[] { String.valueOf(uid) });
			this.log.i("Deleted OutboxTweet uid=%s from %s.", uid, TBL_OB);
			this.mDb.setTransactionSuccessful();
		}
		finally {
			this.mDb.endTransaction();
		}
		notifyOutboxListeners();
	}

	private void notifyOutboxListeners () {
		for (final OutboxListener l : this.outboxListeners) {
			l.outboxChanged();
		}
	}

	@Override
	public void addOutboxListener (final OutboxListener listener) {
		this.outboxListeners.add(listener);
	}

	@Override
	public void removeOutboxListener (final OutboxListener listener) {
		this.outboxListeners.remove(listener);
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

	@Override
	public void housekeep () {
		if (!checkDbOpen()) return;
		pruneMetadataTable();
		vacuum();
	}

	private void pruneMetadataTable () {
		this.mDb.beginTransaction();
		try {
			this.mDb.execSQL("DELETE FROM " + TBL_TM + " WHERE " + TBL_TM_TWID +
					" NOT IN (SELECT " + TBL_TW + "." + TBL_TW_ID + " FROM " + TBL_TW + ");");
			this.mDb.setTransactionSuccessful();
			this.log.i("Pruned table '%s'.", TBL_TM);
		}
		finally {
			this.mDb.endTransaction();
		}
	}

	private void vacuum () {
		this.mDb.execSQL("VACUUM");
		this.log.i("Vacuumed.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public long getTotalTweetsEverSeen () {
		return DatabaseUtils.longForQuery(this.mDb, "SELECT max(" + TBL_TW_ID + ") FROM " + TBL_TW, null);
	}

	@Override
	public double getTweetsPerHour (final int columnId) {
		final Cursor c = getTweetsCursor(columnId);
		try {
			if (c != null && c.moveToFirst()) {
				final int colTime = c.getColumnIndex(TBL_TW_TIME);
				final long newestTime = c.getLong(colTime);
				c.moveToLast();
				final long oldestTime = c.getLong(colTime);
				return (c.getCount() / (double) (newestTime - oldestTime)) * 60 * 60;
			}
			return -1;
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String SEARCH_ESC = "\\";

	/**
	 * This pairs with SEARCH_ESC.
	 */
	private static String escapeSearch (final String term) {
		String q = term.replace("'", "''");
		q = q.replace("\\", "\\\\");
		q = q.replace("%", "\\%");
		q = q.replace("_", "\\_");
		q = q.replace("*", "%");
		return q;
	}

}

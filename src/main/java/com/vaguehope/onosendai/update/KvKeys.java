package com.vaguehope.onosendai.update;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;

public class KvKeys {

	private static final String KEY_PREFIX_COL_LAST_REFRESH_TIME = "COL_LAST_REFRESH_TIME_";
	private static final String KEY_PREFIX_COL_LAST_REFRESH_ERROR = "COL_LAST_REFRESH_ERROR_";
	private static final String KEY_PREFIX_COL_LAST_PUSH_TIME = "COL_LAST_PUSH_TIME_";

	public static final String FEED_SINCE_ID_PREFIX = "FEED_SINCE_ID_";

	public static final String KEY_HOSAKA_STATUS = "HOSAKA_STATUS";

	public static String colLastRefreshTime (final Column col) {
		return KEY_PREFIX_COL_LAST_REFRESH_TIME + col.getId();
	}

	public static String colLastRefreshError (final Column col) {
		return colLastRefreshError(col.getId());
	}

	public static String colLastRefreshError (final int colId) {
		return KEY_PREFIX_COL_LAST_REFRESH_ERROR + colId;
	}

	public static String colLastPushTime (final Column col) {
		return KEY_PREFIX_COL_LAST_PUSH_TIME + col.getId();
	}

	public static String feedSinceId (final Column col, final ColumnFeed feed) {
		return FEED_SINCE_ID_PREFIX + col.getId() + "_" + feed.feedHash();
	}

}

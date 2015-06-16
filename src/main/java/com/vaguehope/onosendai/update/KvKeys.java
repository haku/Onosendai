package com.vaguehope.onosendai.update;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;

public class KvKeys {

	private static final String KEY_PREFIX_COL_LAST_REFRESH_TIME = "COL_LAST_REFRESH_TIME_";
	private static final String KEY_PREFIX_COL_LAST_REFRESH_ERROR = "COL_LAST_REFRESH_ERROR_";
	private static final String KEY_PREFIX_COL_LAST_PUSH_TIME = "COL_LAST_PUSH_TIME_";

	private static final String FEED_SINCE_ID_PREFIX = "FEED_SINCE_ID_";

	private static final String SW_AUTH_TOKEN_PREFIX = "SW_AUTH_TOKEN_";
	private static final String SW_PTA_PREFIX = "SW_PTA_";

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

	public static String feedSinceId (final ColumnFeed feed) {
		return FEED_SINCE_ID_PREFIX + feed.feedHash();
	}

	public static String swAuthToken (final Account acc) {
		return SW_AUTH_TOKEN_PREFIX + acc.getId();
	}

	public static String swPta (final Account acc) {
		return SW_PTA_PREFIX + acc.getId();
	}

}

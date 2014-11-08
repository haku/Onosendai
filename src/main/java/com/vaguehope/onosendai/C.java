package com.vaguehope.onosendai;

import java.util.concurrent.TimeUnit;

public final class C {

	private C () {
		throw new AssertionError();
	}

	public static final String TAG = "onosendai";
	public static final String CONFIG_FILE_NAME = "deck.conf";

	/**
	 * 20% of 60M heap is 12M.
	 */
	public static final int MAX_MEMORY_IMAGE_CACHE = (int) (Runtime.getRuntime().maxMemory() / 5L);

	/**
	 * Do not let an image use more than 10% of the in memory cache.
	 */
	public static final double MAX_MEMORY_IMAGE_CACHE_ENTRY_SIZE_RATIO = 0.1d; // relative to cache size.

	/**
	 * Do not load images larger than this.
	 * 5% of 60M heap is 3M.
	 */
	public static final int MAX_IMAGE_SIZE_BYTES = (int) (Runtime.getRuntime().maxMemory() / 20L);

	public static final int DB_CONNECT_TIMEOUT_SECONDS = 5;

	public static final int TWEET_FETCH_PAGE_SIZE = 20;
	public static final int TWITTER_TIMELINE_MAX_FETCH = TWEET_FETCH_PAGE_SIZE * 5;
	public static final int TWITTER_ME_MAX_FETCH = TWEET_FETCH_PAGE_SIZE;
	public static final int TWITTER_MENTIONS_MAX_FETCH = TWEET_FETCH_PAGE_SIZE;
	public static final int TWITTER_LIST_MAX_FETCH = TWEET_FETCH_PAGE_SIZE * 5;
	public static final int TWITTER_SEARCH_MAX_FETCH = TWEET_FETCH_PAGE_SIZE * 5;

	public static final int DATA_TW_MAX_COL_ENTRIES = 500;

	// Sending and receiving.
	public static final int UPDATER_MIN_COLUMS_TO_USE_THREADPOOL = 2;
	public static final int UPDATER_MAX_THREADS = 3;
	public static final int SEND_OUTBOX_MAX_THREADS = 1;

	// Form main activity.
	public static final int LOCAL_MAX_THREADS = 1;
	public static final int NET_MAX_THREADS = 2;

	// Tweet Lists.
	public static final long SCROLL_TIME_LABEL_TIMEOUT_MILLIS = 3000L;

	// Updates.
	public static final float MIN_BAT_UPDATE = 0.30f;
	public static final float MIN_BAT_SEND = 0.20f;
	public static final float MIN_BAT_CLEANUP = 0.50f;

	// Disc caches.
	public static final long IMAGE_DISC_CACHE_TOUCH_AFTER_MILLIS = TimeUnit.DAYS.toMillis(5);
	public static final long IMAGE_DISC_CACHE_EXPIRY_MILLIS = TimeUnit.DAYS.toMillis(10);
	public static final long TMP_SCALED_IMG_EXPIRY_MILLIS = TimeUnit.DAYS.toMillis(7);

}

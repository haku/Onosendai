package com.vaguehope.onosendai;

import java.util.concurrent.TimeUnit;


public interface C {

	String TAG = "onosendai";
	String CONFIG_FILE_NAME = "deck.conf";

	int MAX_MEMORY_IMAGE_CACHE = 20 * 1024 * 1024;
	int DB_CONNECT_TIMEOUT_SECONDS = 5;

	int TWEET_FETCH_PAGE_SIZE = 20;
	int TWITTER_TIMELINE_MAX_FETCH = TWEET_FETCH_PAGE_SIZE * 5;
	int TWITTER_ME_MAX_FETCH = TWEET_FETCH_PAGE_SIZE;
	int TWITTER_MENTIONS_MAX_FETCH = TWEET_FETCH_PAGE_SIZE;
	int TWITTER_LIST_MAX_FETCH = TWEET_FETCH_PAGE_SIZE * 5;
	int TWITTER_SEARCH_MAX_FETCH = TWEET_FETCH_PAGE_SIZE * 5;

	String DATA_TW_MAX_COL_ENTRIES = "500";

	// Sending and receiving.
	int UPDATER_MIN_COLUMS_TO_USE_THREADPOOL = 2;
	int UPDATER_MAX_THREADS = 3;
	int SEND_OUTBOX_MAX_THREADS = 1;

	// Form main activity.
	int IMAGE_LOADER_MAX_THREADS = 3;
	int DB_MAX_THREADS = 1;

	// Tweet Lists.
	long SCROLL_TIME_LABEL_TIMEOUT_MILLIS = 3000L;

	// Updates.
	float MIN_BAT_UPDATE = 0.30f;
	float MIN_BAT_SEND = 0.20f;
	float MIN_BAT_CLEANUP = 0.50f;

	// Disc caches.
	long IMAGE_DISC_CACHE_TOUCH_AFTER_MILLIS = TimeUnit.DAYS.toMillis(5);
	long IMAGE_DISC_CACHE_EXPIRY_MILLIS = TimeUnit.DAYS.toMillis(10);
	long TMP_SCALED_IMG_EXPIRY_MILLIS = TimeUnit.DAYS.toMillis(7);
}

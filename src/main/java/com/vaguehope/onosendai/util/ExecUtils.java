package com.vaguehope.onosendai.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ExecUtils {

	private static final long THREAD_LINGER_TIME_SECONDS = 30L;

	private ExecUtils () {
		throw new AssertionError();
	}

	public static ExecutorService newBoundedCachedThreadPool (final int maxThreads) {
		return new ThreadPoolExecutor(0, maxThreads,
				THREAD_LINGER_TIME_SECONDS, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}

}

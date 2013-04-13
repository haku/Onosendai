package com.vaguehope.onosendai.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExecUtils {

	private static final long THREAD_LINGER_TIME_SECONDS = 30L;

	private ExecUtils () {
		throw new AssertionError();
	}

	public static ExecutorService newBoundedCachedThreadPool (final int maxThreads, final LogWrapper log) {
		return new ThreadPoolExecutor(0, maxThreads,
				THREAD_LINGER_TIME_SECONDS, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new LoggingThreadFactory(
						new LoggingThreadGroup(Thread.currentThread().getThreadGroup(), log),
						log
				),
				new LoggingRejectionHandler(log));
	}

	private static class LoggingThreadGroup extends ThreadGroup {

		private final LogWrapper log;

		public LoggingThreadGroup (final ThreadGroup parent, final LogWrapper log) {
			super(parent, "tg-" + log.getPrefix());
			this.log = log;
		}

		@Override
		public void uncaughtException (final Thread t, final Throwable e) {
			this.log.wtf("Thread died: " + t.toString(), e);
		}

	}

	private static class LoggingThreadFactory implements ThreadFactory {

		private final LogWrapper log;
		private final AtomicInteger counter = new AtomicInteger(0);
		private final ThreadGroup group;

		public LoggingThreadFactory (final ThreadGroup group, final LogWrapper log) {
			this.group = group;
			this.log = log;
		}

		@Override
		public Thread newThread (final Runnable r) {
			final Thread t = new Thread(this.group, r,
					"t-" + this.log.getPrefix() + this.counter.getAndIncrement(),
					0);
			if (t.isDaemon()) t.setDaemon(false);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			return t;
		}

	}

	private static class LoggingRejectionHandler implements RejectedExecutionHandler {

		private final LogWrapper log;

		public LoggingRejectionHandler (final LogWrapper log) {
			this.log = log;
		}

		@Override
		public void rejectedExecution (final Runnable r, final ThreadPoolExecutor executor) {
			this.log.w("Rejected execution: '%s'.", r.toString());
		}

	}

}

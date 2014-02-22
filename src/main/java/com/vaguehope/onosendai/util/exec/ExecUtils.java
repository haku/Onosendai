package com.vaguehope.onosendai.util.exec;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.onosendai.util.LogWrapper;


public final class ExecUtils {

	private static final long THREAD_LINGER_TIME_SECONDS = 30L;

	private ExecUtils () {
		throw new AssertionError();
	}

	public static ExecutorService newBoundedCachedThreadPool (final int maxThreads, final LogWrapper log) {
		return newBoundedCachedThreadPool(maxThreads, log, null);
	}

	public static ExecutorService newBoundedCachedThreadPool (final int maxThreads, final LogWrapper log, final ExecutorEventListener eventListener) {
		final ThreadPoolExecutor tpe = new TrackingThreadPoolExecutor(eventListener, log, maxThreads, maxThreads,
				THREAD_LINGER_TIME_SECONDS, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new LoggingThreadFactory(new LoggingThreadGroup(Thread.currentThread().getThreadGroup(), log), log),
				new LoggingRejectionHandler(log));
		tpe.allowCoreThreadTimeOut(true);
		return tpe;
	}

	private static class TrackingThreadPoolExecutor extends ThreadPoolExecutor {

		private final ExecutorEventListener eventListener;
		private final LogWrapper log;

		public TrackingThreadPoolExecutor (final ExecutorEventListener eventListener, final LogWrapper log,
				final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
				final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
			this.eventListener = eventListener;
			this.log = log;
		}

		@Override
		public void execute (final Runnable command) {
			if (this.eventListener == null) {
				super.execute(command);
			}
			else {
				super.execute(new TrackingRunnable(this.eventListener, this.log, command));
			}

		}

		private static class TrackingRunnable implements Runnable {

			private final ExecutorEventListener eventListener;
			private final LogWrapper log;
			private final Runnable command;

			public TrackingRunnable (final ExecutorEventListener eventListener, final LogWrapper log, final Runnable command) {
				this.eventListener = eventListener;
				this.log = log;
				this.command = command;
			}

			@Override
			public void run () {
				this.eventListener.execStart(this.log.getPrefix(), this.command);
				try {
					this.command.run();
				}
				finally {
					this.eventListener.execEnd(this.command);
				}
			}
		}

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

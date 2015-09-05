package com.vaguehope.onosendai.storage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.vaguehope.onosendai.storage.DbInterface.ColumnState;
import com.vaguehope.onosendai.storage.DbInterface.ScrollChangeType;
import com.vaguehope.onosendai.storage.DbInterface.TwUpdateListener;
import com.vaguehope.onosendai.util.LogWrapper;

public class SaveScrollNow {

	protected static final LogWrapper LOG = new LogWrapper("SSN");

	public static void requestAndWaitForUiToSaveScroll (final DbInterface db) {
		final ScrollStoreCountingListener twUpdateListener = new ScrollStoreCountingListener();
		db.addTwUpdateListener(twUpdateListener);
		try {
			final long startTime = now();
			final Set<Integer> requestedColumnIds = db.requestStoreScrollNow();
			if (requestedColumnIds.size() > 0) {
				final boolean storedSuccessfully = twUpdateListener.awaitScrollStores(requestedColumnIds, 5, TimeUnit.SECONDS);
				final long durationMillis = TimeUnit.NANOSECONDS.toMillis(now() - startTime);
				LOG.i("Request UI store %s scrolls success=%s in %d millis.", requestedColumnIds, storedSuccessfully, durationMillis);
			}
		}
		finally {
			db.removeTwUpdateListener(twUpdateListener);
		}
	}

	private static class ScrollStoreCountingListener implements TwUpdateListener {

		private final Set<Integer> storedColumnIds = Collections.synchronizedSet(new HashSet<Integer>());

		public ScrollStoreCountingListener () {}

		public boolean awaitScrollStores (final Set<Integer> waitForColumnIds, final int timeout, final TimeUnit unit) {
			final long timeoutNanos = unit.toNanos(timeout);
			final long startTime = now();
			while (true) {
				synchronized (this.storedColumnIds) {
					if (this.storedColumnIds.containsAll(waitForColumnIds)) return true;
				}
				if (now() - startTime > timeoutNanos) return false;
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					return false;
				}
			}
		}

		@Override
		public void columnChanged (final int columnId) {/* unused */}

		@Override
		public void columnStatus (final int columnId, final ColumnState state) {/* unused */}

		@Override
		public void unreadOrScrollChanged (final int columnId, final ScrollChangeType type) {/* unused */}

		@Override
		public Integer requestStoreScrollStateNow () {
			return null;
		}

		@Override
		public void scrollStored (final int columnId) {
			synchronized (this.storedColumnIds) {
				this.storedColumnIds.add(columnId);
			}
		}

	}

	private static final long NANO_ORIGIN = System.nanoTime();

	protected static long now () {
		return System.nanoTime() - NANO_ORIGIN;
	}

}

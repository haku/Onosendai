package com.vaguehope.onosendai.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncMgr {

	private final Map<String, AtomicInteger> locks = new HashMap<String, AtomicInteger>();

	public Object getSync (final String key) {
		synchronized (this.locks) {
			AtomicInteger c = this.locks.get(key);
			if (c == null) {
				c = new AtomicInteger(0);
				this.locks.put(key, c);
			}
			c.incrementAndGet();
			return c;
		}
	}

	public void returnSync (final String key) {
		synchronized (this.locks) {
			final AtomicInteger c = this.locks.get(key);
			if (c != null) {
				c.decrementAndGet();
				if (c.intValue() < 1) this.locks.remove(key);
			}
		}
	}

}

package com.vaguehope.onosendai.util;

import java.util.HashMap;
import java.util.Map;

public class SyncMgr {

	private final Map<String, MutableInt> locks = new HashMap<String, MutableInt>();

	public Object getSync (final String key) {
		synchronized (this.locks) {
			MutableInt c = this.locks.get(key);
			if (c == null) {
				c = new MutableInt();
				this.locks.put(key, c);
			}
			c.i++;
			return c;
		}
	}

	public void returnSync (final String key) {
		synchronized (this.locks) {
			MutableInt c = this.locks.get(key);
			if (c != null) {
				c.i--;
				if (c.i < 1) this.locks.remove(key);
			}
		}
	}

	private static class MutableInt {

		volatile int i = 0;

		public MutableInt () {}

	}

}

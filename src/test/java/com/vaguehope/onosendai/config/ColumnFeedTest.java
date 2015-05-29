package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ColumnFeedTest {

	@Test
	public void itDoesObjectyThings () throws Exception {
		final ColumnFeed a = new ColumnFeed("a", "b");
		final ColumnFeed b = new ColumnFeed("a", "b");
		assertEquals(a, b);
	}

}

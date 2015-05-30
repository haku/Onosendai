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

	@Test
	public void itHashes () throws Exception {
		assertEquals(null, new ColumnFeed(null, null).feedHash());
		assertEquals("c97a82dc", new ColumnFeed(null, "a").feedHash());
		assertEquals("34397989", new ColumnFeed("a", null).feedHash());
		assertEquals("dcea6d9c", new ColumnFeed("a", "b").feedHash());

	}

}

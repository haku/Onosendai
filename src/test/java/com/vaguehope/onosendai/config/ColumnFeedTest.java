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
		assertEquals("nj9ab64b", new ColumnFeed(null, "a").feedHash());
		assertEquals("63m4iju2", new ColumnFeed("a", null).feedHash());
		assertEquals("psztrxrs", new ColumnFeed("a", "b").feedHash());

	}

}

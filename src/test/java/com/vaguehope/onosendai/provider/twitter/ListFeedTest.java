package com.vaguehope.onosendai.provider.twitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;


public class ListFeedTest {

	@Test
	public void itDoesNotRequireUserName () throws Exception {
		final ListFeed lf = new ListFeed("mylist");
		assertEquals(null, lf.getOwnerScreenName());
		assertEquals("mylist", lf.getSlug());
	}

	@Test
	public void itParsesUserName () throws Exception {
		final ListFeed lf = new ListFeed("user/mylist");
		assertEquals("user", lf.getOwnerScreenName());
		assertEquals("mylist", lf.getSlug());
	}

	@SuppressWarnings("unused")
	@Test
	public void itCanNotStartWithASlash () throws Exception {
		try {
			new ListFeed("/mylist");
			fail("Expected ex.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("ownerScreenNameAndSlug can not start with /: /mylist", e.getMessage());
		}
	}

}

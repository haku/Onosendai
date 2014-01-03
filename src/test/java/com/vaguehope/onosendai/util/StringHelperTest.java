package com.vaguehope.onosendai.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringHelperTest {

	@Test
	public void itFindsFirstLine () throws Exception {
		assertEquals("abc", StringHelper.firstLine("abc"));
		assertEquals("abc", StringHelper.firstLine("abc\n"));
		assertEquals("abc", StringHelper.firstLine("abc\ndef"));
	}

}

package com.vaguehope.onosendai.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringHelperTest {

	@Test
	public void itFindsFirstLine () throws Exception {
		assertEquals("abc", StringHelper.firstLine("abc"));
		assertEquals("abc", StringHelper.firstLine("abc\n"));
		assertEquals("abc", StringHelper.firstLine("abc\ndef"));
	}

	@Test
	public void itTruncatesFront () throws Exception {
		assertEquals("abcdef", StringHelper.maxLengthEnd("abcdef", 6));
		assertEquals("...efg", StringHelper.maxLengthEnd("abcdefg", 6));
		assertEquals("...hijkl", StringHelper.maxLengthEnd("abcdefghijkl", 8));
	}

	@Test
	public void itReplacesFirst () throws Exception {
		assertEquals("foo bqwe bat", StringHelper.replaceOnce("foo bar bat", "ar", "qwe"));
		assertEquals("foo b bat", StringHelper.replaceOnce("foo bar bat", "ar", ""));
		assertEquals(" bar bat", StringHelper.replaceOnce("foo bar bat", "foo", ""));
		assertEquals("foo bar ", StringHelper.replaceOnce("foo bar bat", "bat", ""));
	}

	@Test
	public void itChecksEndsWith () throws Exception {
		assertTrue(StringHelper.endsWith("abc123", "23"));
		assertFalse(StringHelper.endsWith("abc123", "12"));
		assertFalse(StringHelper.endsWith("a", null));
		assertFalse(StringHelper.endsWith(null, "1"));
	}

	@Test
	public void itRemovesSuffex () throws Exception {
		assertEquals("foo ", StringHelper.removeSuffex("foo bar", "bar"));
		assertEquals("foo bar ", StringHelper.removeSuffex("foo bar ", "bar"));
	}

}

package com.vaguehope.onosendai.widget.adaptor;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class UsernameTokenizerTest {

	private UsernameTokenizer undertest;

	@Before
	public void before () throws Exception {
		this.undertest = new UsernameTokenizer();
	}

	@Test
	public void itIgnoresPrefixChar () throws Exception {
		assertEquals(0, this.undertest.findTokenStart("", 0));
		assertEquals(0, this.undertest.findTokenStart("@", 0));
		assertEquals(0, this.undertest.findTokenStart("a", 0));
		assertEquals(1, this.undertest.findTokenStart("@", 1));
		assertEquals(1, this.undertest.findTokenStart("@ ", 1));
		assertEquals(5, this.undertest.findTokenStart("foo @", 5));
		assertEquals(5, this.undertest.findTokenStart("foo @ ", 5));
		assertEquals(5, this.undertest.findTokenStart("foo #", 5));
		assertEquals(5, this.undertest.findTokenStart("foo # ", 5));
	}

	@Test
	public void itFindsTokenStart () throws Exception {
		assertEquals(4, this.undertest.findTokenStart("foo @t", 6));
		assertEquals(4, this.undertest.findTokenStart("foo @t ", 6));
		assertEquals(4, this.undertest.findTokenStart("foo @thing bar", 7));
		assertEquals(4, this.undertest.findTokenStart("foo #t", 6));
		assertEquals(4, this.undertest.findTokenStart("foo #t ", 6));
		assertEquals(4, this.undertest.findTokenStart("foo #thing bar", 7));
		assertEquals(7, this.undertest.findTokenStart("foo thing bar", 7));
		assertEquals(5, this.undertest.findTokenStart("foo t", 5));
	}

	@Test
	public void itFindsTokenEnd () throws Exception {
		assertEquals(10, this.undertest.findTokenEnd("foo @thing bar", 7));
		assertEquals(12, this.undertest.findTokenEnd("foo thingbar", 7));
	}

}

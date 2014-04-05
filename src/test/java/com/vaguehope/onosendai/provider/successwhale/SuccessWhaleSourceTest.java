package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class SuccessWhaleSourceTest {

	@Test
	public void itDoesEqualsAndHashcodeOnOnUrlOnly () throws Exception {
		final SuccessWhaleSource a = new SuccessWhaleSource("title", "url");
		final SuccessWhaleSource same = new SuccessWhaleSource("title 2", "url");
		final SuccessWhaleSource notSame = new SuccessWhaleSource("title", "url 2");

		assertEquals(a, same);
		assertEquals(a.hashCode(), same.hashCode());

		assertNotEquals(a, notSame);
		assertNotEquals(a.hashCode(), notSame.hashCode());
	}

}

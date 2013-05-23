package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccountTest {

	@Test
	public void itRoundTripsTwitter () throws Exception {
		Account a = new Account("1", "t", AccountProvider.TWITTER, "2", "3", "4", "5");
		String j = a.toJson().toString(2);
		Account a1 = Account.parseJson(j);
		assertEquals(a, a1);
	}

	@Test
	public void itRoundTripsSuccessWhale () throws Exception {
		Account a = new Account("1", "t", AccountProvider.SUCCESSWHALE, null, null, "4", "5");
		String j = a.toJson().toString(2);
		Account a1 = Account.parseJson(j);
		assertEquals(a, a1);
	}

	@Test
	public void itRoundTripsBuffer () throws Exception {
		Account a = new Account("1", "t", AccountProvider.BUFFER, null, null, "4", null);
		String j = a.toJson().toString(2);
		Account a1 = Account.parseJson(j);
		assertEquals(a, a1);
	}

}

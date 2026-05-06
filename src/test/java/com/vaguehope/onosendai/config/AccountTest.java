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
	public void itRoundTripsMastodon () throws Exception {
		Account a = new Account("1", "m", AccountProvider.MASTODON, "2", "3", "4", "5");
		String j = a.toJson().toString(2);
		Account a1 = Account.parseJson(j);
		assertEquals(a, a1);
	}

}

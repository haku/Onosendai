package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class NotificationStyleTest {

	@Test
	public void itRoundTripsViaString () throws Exception {
		final NotificationStyle ns = new NotificationStyle(true, false, true, true, true);
		final Object j = ns.toJson().toString(2);
		final NotificationStyle ns1 = NotificationStyle.parseJson(j);
		assertEquals(ns, ns1);
	}

	@Test
	public void itRoundTripsViaJsonObject () throws Exception {
		final NotificationStyle ns = new NotificationStyle(true, false, true, true, true);
		final Object j = ns.toJson();
		final NotificationStyle ns1 = NotificationStyle.parseJson(j);
		assertEquals(ns, ns1);
	}

	@Test
	public void itParsesLegacyBoolean () throws Exception {
		assertEquals(NotificationStyle.DEFAULT, NotificationStyle.parseJson(Boolean.TRUE));
		assertEquals(null, NotificationStyle.parseJson(Boolean.FALSE));
	}

	@Test
	public void itThrowsOnStrangeObject () throws Exception {
		final Object obj = new Object();
		try {
			NotificationStyle.parseJson(obj);
			fail("Expected ex.");
		}
		catch (final IllegalArgumentException e) {
			assertEquals("Unexpected object type " + obj.getClass() + ": " + obj, e.getMessage());
		}
	}

	@Test
	public void itEqualsChecks () throws Exception {
		assertTrue(new NotificationStyle(true, false, false, true, false).equals(new NotificationStyle(true, false, false, true, false)));
		assertTrue(new NotificationStyle(false, true, false, false, true).equals(new NotificationStyle(false, true, false, false, true)));
		assertTrue(new NotificationStyle(false, false, true, true, false).equals(new NotificationStyle(false, false, true, true, false)));
		assertFalse(new NotificationStyle(true, false, false, false, true).equals(new NotificationStyle(false, false, false, false, true)));
		assertFalse(new NotificationStyle(false, true, false, true, false).equals(new NotificationStyle(false, false, false, true, false)));
		assertFalse(new NotificationStyle(false, false, true, false, true).equals(new NotificationStyle(false, false, false, false, true)));
	}

	@Test
	public void itDefaultTitles () throws Exception {
		assertEquals("plain", NotificationStyle.DEFAULT.getUiTitle());
	}

	@Test
	public void itDetailedTitles () throws Exception {
		assertEquals("lights", new NotificationStyle(true, false, false, false, false).getUiTitle());
		assertEquals("vibrate", new NotificationStyle(false, true, false, false, false).getUiTitle());
		assertEquals("sound", new NotificationStyle(false, false, true, false, false).getUiTitle());
		assertEquals("lights, sound", new NotificationStyle(true, false, true, false, false).getUiTitle());
		assertEquals("lights, vibrate, sound", new NotificationStyle(true, true, true, false, false).getUiTitle());
		assertEquals("lights, vibrate, sound, exclude retweets", new NotificationStyle(true, true, true, true, false).getUiTitle());
		assertEquals("lights, vibrate, sound, include own tweets", new NotificationStyle(true, true, true, false, true).getUiTitle());
	}

}

package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class TimeParserTest {

	@Test
	public void itParsesNothingTo0 () throws Exception {
		assertEquals(0, TimeParser.parseDuration(null));
		assertEquals(0, TimeParser.parseDuration(""));
		assertEquals(0, TimeParser.parseDuration(" "));
	}

	@Test
	public void itReturnsNegativeForInvalid () throws Exception {
		assertEquals(-1, TimeParser.parseDuration("a"));
		assertEquals(-1, TimeParser.parseDuration("34598aweosdfk"));
	}

	@Test
	public void itParsesDurationInMinutes () throws Exception {
		assertEquals(15, TimeParser.parseDuration("15min"));
		assertEquals(15, TimeParser.parseDuration("15mins"));
		assertEquals(15, TimeParser.parseDuration("15MIN"));
		assertEquals(15, TimeParser.parseDuration("15MINS"));
	}

	@Test
	public void itParsesDurationInHours () throws Exception {
		assertEquals(60, TimeParser.parseDuration("1hour"));
		assertEquals(120, TimeParser.parseDuration("2hours"));
		assertEquals(60, TimeParser.parseDuration("1HOUR"));
		assertEquals(120, TimeParser.parseDuration("2HOURS"));
	}

	@Test
	public void itParsesDurationInHoursAndMinutes () throws Exception {
		assertEquals(75, TimeParser.parseDuration("1hour15min"));
		assertEquals(150, TimeParser.parseDuration("2hours30mins"));
		assertEquals(75, TimeParser.parseDuration("1HOUR15MINS"));
		assertEquals(210, TimeParser.parseDuration("3HOUR30MINS"));
		assertEquals(255, TimeParser.parseDuration("4hour 15min"));
	}

}

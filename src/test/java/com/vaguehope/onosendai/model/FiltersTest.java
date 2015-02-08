package com.vaguehope.onosendai.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FiltersTest {

	@Test
	public void itFiltersPlainStringCaseInsensitive () throws Exception {
		final Filters f = new Filters("#foo");
		assertTrue(f.matches("some #foo tweet."));
		assertTrue(f.matches("some #Foo tweet."));
		assertFalse(f.matches("some foo tweet."));
	}

	@Test
	public void itFiltersRegex () throws Exception {
		final Filters f = new Filters("/^\\.\\s*@");
		assertTrue(f.matches(". @foo thing."));
		assertTrue(f.matches(".@foo thing."));
		assertFalse(f.matches("@foo . @thing."));
	}

}

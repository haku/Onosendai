package com.vaguehope.onosendai.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.PatternSyntaxException;

import org.junit.Test;

public class FiltersTest {

	@Test
	public void itFiltersPlainStringCaseInsensitive () throws Exception {
		final Filters f = new Filters("#foo");
		testBodyMatch(f, "some #foo tweet.");
		testBodyMatch(f, "some #Foo tweet.");
		testBodyNotMatch(f, "some foo tweet.");
	}

	@Test
	public void itFiltersRegex () throws Exception {
		final Filters f = new Filters("/^\\.\\s*@");
		testBodyMatch(f, ". @foo thing.");
		testBodyMatch(f, ".@foo thing.");
		testBodyNotMatch(f, "@foo . @thing.");
	}

	@Test
	public void itFiltersRegexWithTrailingSlash () throws Exception {
		final Filters f = new Filters("/^\\.\\s*@/");
		testBodyMatch(f, ". @foo thing.");
		testBodyMatch(f, ".@foo thing.");
		testBodyNotMatch(f, "@foo . @thing.");
	}

	@Test
	public void itFiltersRegexCaseInsensitive () throws Exception {
		final Filters f = new Filters("/(fifty|50) tones of colour");
		testBodyMatch(f, "foo fifty tones of colour bar.");
		testBodyMatch(f, "foo fifty Tones of Colour bar.");
		testBodyMatch(f, "foo 50 Tones of Colour bar.");
	}

	@Test
	public void itFiltersRegexAgainstUsername () throws Exception {
		final Filters f = new Filters("/annoyingUser/u");
		assertTrue(f.matches("body", "annoyingUser", null));
		assertTrue(f.matches("body", "annoyinguser", null));
		assertFalse(f.matches("annoyingUser", "otherUser", "annoyingUser"));
	}

	@Test
	public void itFiltersRegexAgainstBodyOrUsername () throws Exception {
		final Filters f = new Filters("/annoyingUser/ub");
		assertTrue(f.matches("body", "annoyingUser", null));
		assertTrue(f.matches("body", "annoyinguser", null));
		assertTrue(f.matches("annoyingUser", "otherUser", null));
		assertFalse(f.matches("user", "otherUser", "yetAnotherUser"));
	}

	@Test
	public void itFiltersRegexAgainstRetweetedBy () throws Exception {
		final Filters f = new Filters("/annoyingUser/r");
		assertTrue(f.matches("body", "otherUser", "annoyingUser"));
		assertTrue(f.matches("body", "otherUser", "annoyinguser"));
		assertFalse(f.matches("annoyingUser", "otherUser", "yetAnotherUser"));
	}

	@Test
	public void itFiltersAnchoredRegexAgainstRetweetedBy () throws Exception {
		final Filters f = new Filters("/^annoyingUser$/r");
		assertTrue(f.matches("body", "otherUser", "annoyingUser"));
		assertTrue(f.matches("body", "otherUser", "annoyinguser"));
		assertFalse(f.matches("annoyingUser", "otherUser", "annoyingUser2"));
	}

	@Test
	public void itUnderstandsOwnerIsNotRetweet () throws Exception {
		final Filters f = new Filters("/someUser/r");
		assertFalse(f.matches("body", "someUser", "someUser"));
	}

	@Test
	public void itUnderstandsNullOwnerIsNotRetweet () throws Exception {
		final Filters f = new Filters("/someUser/r");
		assertFalse(f.matches("body", null, "someUser"));
	}

	@Test
	public void itDoesNotNpe () throws Exception {
		assertFalse(new Filters("f").matches(null, null, null));
		assertFalse(new Filters("/f").matches(null, null, null));
	}

	@Test
	public void itValidatesInvalidRegex () throws Exception {
		try {
			Filters.validateFilter("/[a");
			fail("Expected ex.");
		}
		catch (final PatternSyntaxException e) {
			assertEquals("Unclosed character class near index 1\n"
					+ "[a\n"
					+ " ^", e.getMessage());
		}
	}

	@Test
	public void itValidatesEmptyRegex () throws Exception {
		try {
			Filters.validateFilter("/");
			fail("Expected ex.");
		}
		catch (final PatternSyntaxException e) {
			assertEquals("Empty pattern. near index 0\n"
					+ "\n"
					+ "^", e.getMessage());
		}
	}

	private static void testBodyMatch (final Filters f, final String body) {
		assertTrue(f.matches(body, null, null));
	}

	private static void testBodyNotMatch (final Filters f, final String body) {
		assertFalse(f.matches(body, null, null));
	}

}

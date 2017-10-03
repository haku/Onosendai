package com.vaguehope.onosendai.model;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TweetTest {

	private static final long UTIME = 1234567890L;
	private static final String FORMATTED_UTIME = DateFormat.getDateTimeInstance().format(new Date(TimeUnit.SECONDS.toMillis(UTIME)));

	@Test
	public void itConvertsToNiceLine () throws Exception {
		final Tweet t = new Tweet(null, null, "Their Name", "via foo", "via Foo Bar", null, "The tweet's body.", UTIME, null, null, null, null);
		assertEquals("\"The tweet's body.\" Their Name", t.toHumanLine());
	}

	@Test
	public void itConvertsToNiceParagraph () throws Exception {
		final Tweet t = new Tweet(null, "someone", "Their Name", "via foo", "via Foo Bar", null, "The tweet's body.", UTIME, null, null, null, null);
		assertEquals("\"The tweet's body.\"" +
				"\nTheir Name (someone)" +
				"\n" + FORMATTED_UTIME, t.toHumanParagraph());
	}

	@Test
	public void itIsOnWithoutUserName () throws Exception {
		final Tweet t = new Tweet(null, null, "Their Name", "via foo", "via Foo Bar", null, "The tweet's body.", UTIME, null, null, null, null);
		assertEquals("\"The tweet's body.\"" +
				"\nTheir Name" +
				"\n" + FORMATTED_UTIME, t.toHumanParagraph());
	}

}

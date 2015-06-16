package com.vaguehope.onosendai.model;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

public class TweetListTest {

	@Test
	public void itFindsMostRecent () throws Exception {
		final Tweet exp;
		final TweetList tl = new TweetList(Arrays.asList(
				exp = mockTweet(1234567890L),
				mockTweet(1234567880L)));
		assertSame(exp, tl.getMostRecent());
	}

	@Test
	public void itFindsMostRecentBySidIfSameTime () throws Exception {
		final Tweet exp;
		final TweetList tl = new TweetList(Arrays.asList(
				mockTweet(1234567890L, "505030858433503214"),
				exp = mockTweet(1234567890L, "505030858433503254"),
				mockTweet(1234567890L, "505030858433503234")));
		assertSame(exp, tl.getMostRecent());
	}

	private static Tweet mockTweet (final long time) {
		return mockTweet(time, null);
	}

	private static Tweet mockTweet (final long time, final String sid) {
		final Tweet t = mock(Tweet.class);
		when(t.getTime()).thenReturn(time);
		when(t.getSid()).thenReturn(sid);
		return t;
	}

}

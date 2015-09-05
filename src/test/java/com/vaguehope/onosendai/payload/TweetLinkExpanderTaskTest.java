package com.vaguehope.onosendai.payload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.onosendai.provider.twitter.TwitterUrls;

public class TweetLinkExpanderTaskTest {

	private Pattern tweetPatter;

	@Before
	public void before () throws Exception {
		final Field f = TwitterUrls.class.getDeclaredField("TWEET_URL_PATTERN");
		f.setAccessible(true);
		this.tweetPatter = (Pattern) f.get(null);
	}

	@Test
	public void itMatchesTweetUrl () throws Exception {
		final Matcher m = this.tweetPatter.matcher("http://twitter.com/user/status/448240882710757376");
		assertTrue(m.matches());
		assertEquals("448240882710757376", m.group(2));
	}

	@Test
	public void itMatchesHttpsTweetUrl () throws Exception {
		final Matcher m = this.tweetPatter.matcher("https://twitter.com/user/status/448240882710757376");
		assertTrue(m.matches());
		assertEquals("448240882710757376", m.group(2));
	}

	@Test
	public void itMatchesMobileTweetUrl () throws Exception {
		final Matcher m = this.tweetPatter.matcher("http://mobile.twitter.com/user/status/448240882710757376");
		assertTrue(m.matches());
		assertEquals("448240882710757376", m.group(2));
	}

	@Test
	public void itMatchesTweetUrlWithQuery () throws Exception {
		final Matcher m = this.tweetPatter.matcher("http://twitter.com/user/status/448240882710757376?s=09");
		assertTrue(m.matches());
		assertEquals("448240882710757376", m.group(2));
	}

	@Test
	public void itDoesNotMatchPictureUrl () throws Exception {
		assertFalse(this.tweetPatter.matcher("http://twitter.com/user/status/448240882710757376/photo/1").matches());
	}

}

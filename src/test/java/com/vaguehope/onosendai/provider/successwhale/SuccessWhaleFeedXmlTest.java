package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;

public class SuccessWhaleFeedXmlTest {

	@Test
	public void itParsesTweets () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(getClass().getResourceAsStream("/successwhale_timeline.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(2, tweets.count());

		Tweet t0 = tweets.getTweet(0);
		assertEquals("first tweets message", t0.getBody());
		assertEquals("313781345934852800", t0.getSid());
		assertEquals("some_user_name", t0.getUsername());
		assertEquals(1363646281L, t0.getTime());

		Tweet t1 = tweets.getTweet(1);
		assertEquals("The second tweets message.", t1.getBody());
		assertEquals("313434534579697409", t1.getSid());
		assertEquals("some_other_user", t1.getUsername());
		assertEquals(1363563060L, t1.getTime());
	}

}

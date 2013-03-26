package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;

public class SuccessWhaleFeedXmlTest {

	@Test
	public void itParsesTweets () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(getClass().getResourceAsStream("/successwhale_tweets.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(2, tweets.count());

		Tweet t0 = tweets.getTweet(0);
		assertEquals("first tweets message", t0.getBody());
		assertEquals("313781345934852800", t0.getSid());
		assertEquals("some_user_name", t0.getUsername());
		assertEquals(1363646281L, t0.getTime());
		assertEquals("http://si0.twimg.com/profile_images/2344890405/af462a16c498087e4372cb6ac6aff134_normal.jpeg", t0.getAvatarUrl());

		Tweet t1 = tweets.getTweet(1);
		assertEquals("The second tweets message.", t1.getBody());
		assertEquals("313434534579697409", t1.getSid());
		assertEquals("some_other_user", t1.getUsername());
		assertEquals(1363563060L, t1.getTime());
		assertEquals("http://si0.twimg.com/profile_images/19393922/informal_profile_small2_normal.jpg", t1.getAvatarUrl());
	}

	@Test
	public void itParsesFacebookHomeFeedEntryWithUrl () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(getClass().getResourceAsStream("/successwhale_fb_home.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(3, tweets.count());

		Tweet t0 = tweets.getTweet(0);
		assertEquals("Some User shared a link.", t0.getBody());
		assertEquals("557897893_497789789789963", t0.getSid());
		assertEquals(null, t0.getUsername());
		assertEquals("Some User", t0.getFullname());
		assertEquals(1364295194L, t0.getTime());

		assertEquals(1, t0.getMetas().size());
		Meta t0m0 = t0.getMetas().get(0);
		assertEquals(MetaType.URL, t0m0.getType());
		assertEquals("http://9gag.com/gag/6904527?ref=fb.s", t0m0.getData());
	}

	// TODO conversation entry.

	@Test
	public void itParsesFacebookHomeFeedEntryWithUrlAndNoText () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(getClass().getResourceAsStream("/successwhale_fb_home.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(3, tweets.count());

		Tweet t = tweets.getTweet(2);
		assertEquals("A Link with a Page Title", t.getBody());
		assertEquals("534234215_10923840922345316", t.getSid());
		assertEquals(null, t.getUsername());
		assertEquals("Jon Doe", t.getFullname());
		assertEquals(1364270468L, t.getTime());

		assertEquals(1, t.getMetas().size());
		Meta m0 = t.getMetas().get(0);
		assertEquals(MetaType.URL, m0.getType());
		assertEquals("http://apps.facebook.com/daily_photos_plus/y/the&other&args", m0.getData());
	}

}

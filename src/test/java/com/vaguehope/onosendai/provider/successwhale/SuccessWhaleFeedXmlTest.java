package com.vaguehope.onosendai.provider.successwhale;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;

public class SuccessWhaleFeedXmlTest {

	private static final String ACCOUNT_ID = "ac0";
	private Account account;

	@Before
	public void before () throws Exception {
		this.account = new Account(ACCOUNT_ID, null, null, null, null, null, null);
	}

	@Test
	public void itParsesAllTweets () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_tweets.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(3, tweets.count());
	}

	@Test
	public void itParsesASimpleTweets () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_tweets.xml"));
		TweetList tweets = feed.getTweets();

		Tweet t = tweets.getTweet(0);
		assertEquals("first tweets message", t.getBody());
		assertEquals("313781345934852800", t.getSid());
		assertEquals("some_user_name", t.getUsername());
		assertEquals(1363646281L, t.getTime());
		assertEquals("http://si0.twimg.com/profile_images/2344890405/af462a16c498087e4372cb6ac6aff134_normal.jpeg", t.getAvatarUrl());

		assertEquals(3, t.getMetas().size());
		assertHasMeta(t.getMetas(), new Meta(MetaType.INREPLYTO, "313783459384590224"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.SERVICE, "twitter:09823422"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.ACCOUNT, ACCOUNT_ID));
	}

	@Test
	public void itPareseATweetWithLinkAndPreview () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_tweets.xml"));
		TweetList tweets = feed.getTweets();

		Tweet t = tweets.getTweet(1);
		assertEquals("DreamWorks tops compute-cycle record with 'The Croods' http://t.co/zG68KbAFDX", t.getBody());
		assertEquals("339045390394853920", t.getSid());
		assertEquals("some_other_user", t.getUsername());
		assertEquals(1364374692L, t.getTime());
		assertEquals("http://si0.twimg.com/profile_images/0983453383/e987609785a770b7ceeb476525435a1f_normal.jpeg", t.getAvatarUrl());

		assertEquals(4, t.getMetas().size());
		assertHasMeta(t.getMetas(), new Meta(MetaType.MEDIA, "http://pbs.twimg.com/media/SDFLKsdflkjdfmG.jpg"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.URL,
				"http://www.computerworld.com/s/article/9237880/DreamWorks_tops_compute_cycle_record_with_The_Croods_",
				"computerworld.com/s/article/9237…"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.SERVICE, "twitter:09823422"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.ACCOUNT, ACCOUNT_ID));
	}

	@Test
	public void itParsesARetweet () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_tweets.xml"));
		TweetList tweets = feed.getTweets();

		Tweet t = tweets.getTweet(2);

		assertEquals(4, t.getMetas().size());
		assertHasMeta(t.getMetas(), new Meta(MetaType.MENTION, "johndoe", "RT by @johndoe"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.URL, "http://example.com/cool", "Link Title Goes Here"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.SERVICE, "twitter:09823422"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.ACCOUNT, ACCOUNT_ID));
	}

	@Test
	public void itParsesFacebookHomeFeedEntryWithUrl () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_fb_home.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(3, tweets.count());

		Tweet t = tweets.getTweet(0);
		assertEquals("Some User shared a link.", t.getBody());
		assertEquals("557897893_497789789789963", t.getSid());
		assertEquals(null, t.getUsername());
		assertEquals("Some User", t.getFullname());
		assertEquals(1364295194L, t.getTime());

		assertEquals(4, t.getMetas().size());
		assertHasMeta(t.getMetas(), new Meta(MetaType.MEDIA, "https://s-platform.ak.fbcdn.net/www/app_full_proxy.php?app=350685531728&v=1&size=z&cksum=1e87fdc3550bd207da99c7d631a82ae0&src=http%3A%2F%2Fnews.bbcimg.co.uk%2Fmedia%2Fimages%2F66629000%2Fjpg%2F_66629196_66629188.jpg"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.URL, "http://m.bbc.co.uk/news/world-asia-21950139", "North Korea warns South president"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.SERVICE, "facebook:532423349"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.ACCOUNT, ACCOUNT_ID));
	}

	// TODO conversation entry.

	@Test
	public void itParsesFacebookHomeFeedEntryWithUrlAndNoText () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_fb_home.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(3, tweets.count());

		Tweet t = tweets.getTweet(2);
		assertEquals("A Link with a Page Title", t.getBody());
		assertEquals("534234215_10923840922345316", t.getSid());
		assertEquals(null, t.getUsername());
		assertEquals("Jon Doe", t.getFullname());
		assertEquals(1364270468L, t.getTime());

		assertEquals(3, t.getMetas().size());
		assertHasMeta(t.getMetas(), new Meta(MetaType.URL, "http://apps.facebook.com/daily_photos_plus/y/the&other&args", "A Link with a Page Title"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.SERVICE, "facebook:532423349"));
		assertHasMeta(t.getMetas(), new Meta(MetaType.ACCOUNT, ACCOUNT_ID));
	}

	@Test
	public void itParsesFacebookCommentsThreadInsteadOfPost () throws Exception {
		SuccessWhaleFeedXml feed = new SuccessWhaleFeedXml(this.account, getClass().getResourceAsStream("/successwhale_fb_comments.xml"));
		TweetList tweets = feed.getTweets();
		assertEquals(2, tweets.count());

		Tweet t = tweets.getTweet(0);
		assertEquals("The first comment.", t.getBody());
		assertEquals("523049849_10152161726966490_26840788", t.getSid());
		assertEquals(null, t.getUsername());
		assertEquals("The commenter", t.getFullname());
		assertEquals(1364767585L, t.getTime());
		assertEquals("http://graph.facebook.com/100000239935226/picture", t.getAvatarUrl());

		Tweet t1 = tweets.getTweet(1);
		assertEquals("The second comment.", t1.getBody());
		assertEquals("523049849_10152161726966490_26841688", t1.getSid());
		assertEquals(null, t1.getUsername());
		assertEquals("The Poster", t1.getFullname());
		assertEquals(1364774559L, t1.getTime());
		assertEquals("http://graph.facebook.com/523049849/picture", t1.getAvatarUrl());
	}

	private static void assertHasMeta (final List<Meta> metas, final Meta m) {
		assertThat(metas, hasItem(m));
	}

}

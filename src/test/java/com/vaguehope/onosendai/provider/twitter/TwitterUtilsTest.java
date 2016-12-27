package com.vaguehope.onosendai.provider.twitter;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import twitter4j.ExtendedMediaEntity;
import twitter4j.ExtendedMediaEntity.Variant;
import twitter4j.HttpResponse;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.CollectionHelper;

@RunWith(RobolectricTestRunner.class)
public class TwitterUtilsTest {

	private Account account;

	@Before
	public void before () throws Exception {
		this.account = mock(Account.class);
	}

	@Test
	public void itAddsExtraMetas () throws Exception {
		final Status s = mockTweet("foo");
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false, CollectionHelper.listOf(new Meta(MetaType.FEED_HASH, "abcdefgh")), null);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.FEED_HASH, "abcdefgh")));
		assertThat(t.getMetas(), hasSize(2));
	}

	@Test
	public void itExpandsUrl () {
		final Status s = mockTweet("Got some fancy Nespresso from @noriko_hamada this morning. Approve. \ud83d\ude34\u2615\ufe0f\ud83d\udc4c https://t.co/9JdtE36Djy");

		final URLEntity ue = mock(URLEntity.class);
		when(ue.getExpandedURL()).thenReturn("http://twitter.com/stuarthicks/status/698806076515479552/photo/1");
		when(ue.getURL()).thenReturn("https://t.co/9JdtE36Djy");
		when(ue.getStart()).thenReturn(73);
		when(ue.getEnd()).thenReturn(96);

		when(s.getURLEntities()).thenReturn(new URLEntity[] { ue });

		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);

		assertEquals("Got some fancy Nespresso from @noriko_hamada this morning. Approve. \ud83d\ude34\u2615\ufe0f\ud83d\udc4c http://twitter.com/stuarthicks/status/698806076515479552/photo/1", t.getBody());
	}

	@Test
	public void itExpandsTwitterMedia () throws Exception {
		final Status s = mockTweetWithMedia("https://twitter.com/some*user/status/1235430985/photo/1", "https://pbs.twimg.com/media/BjwsdkfjsAAI-4x.jpg");
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);

		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, "https://pbs.twimg.com/media/BjwsdkfjsAAI-4x.jpg", "https://twitter.com/some*user/status/1235430985/photo/1")));
		assertEquals("media: ", t.getBody());
		assertNoMetaOfType(t, MetaType.URL);
	}

	@Test
	public void itExpandsTwitterTwoMedias () throws Exception {
		final Status s = mockTweet("media.");
		final ExtendedMediaEntity me1 = mockExtendedMediaEntry("https://twitter.com/some*user/status/1235430985/photo/1", "https://pbs.twimg.com/media/BjwsdkfjsAAI-4x.jpg");
		final ExtendedMediaEntity me2 = mockExtendedMediaEntry("https://twitter.com/some*user/status/1235430986/photo/1", "https://pbs.twimg.com/media/BjwsdkfjsAAJ-4y.jpg");
		when(s.getMediaEntities()).thenReturn(new ExtendedMediaEntity[] { me1, me2 });

		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, "https://pbs.twimg.com/media/BjwsdkfjsAAI-4x.jpg", "https://twitter.com/some*user/status/1235430985/photo/1")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, "https://pbs.twimg.com/media/BjwsdkfjsAAJ-4y.jpg", "https://twitter.com/some*user/status/1235430986/photo/1")));
		assertNoMetaOfType(t, MetaType.URL);
		assertEquals("2 pictures", t.getUserSubtitle());
	}

	@Test
	public void itExpandsTwitterGif () throws Exception {
		final Status s = mockTweet("media.");
		final ExtendedMediaEntity me1 = mockExtendedMediaEntry("https://twitter.com/some*user/status/1235430985/photo/1", "https://pbs.twimg.com/tweet_video_thumb/CEphgfeWAAEBurm.png");
		final Variant v1 = mock(Variant.class);
		when(s.getMediaEntities()).thenReturn(new ExtendedMediaEntity[] { me1 });
		when(me1.getType()).thenReturn("animated_gif");
		when(me1.getVideoDurationMillis()).thenReturn(0L);
		when(me1.getVideoVariants()).thenReturn(new Variant[] { v1 });
		when(v1.getContentType()).thenReturn("video/mp4");
		when(v1.getUrl()).thenReturn("https://pbs.twimg.com/tweet_video/CEphgfeWAAEBurm.mp4");

		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, "https://pbs.twimg.com/tweet_video_thumb/CEphgfeWAAEBurm.png", "https://twitter.com/some*user/status/1235430985/photo/1")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.URL, "https://pbs.twimg.com/tweet_video/CEphgfeWAAEBurm.mp4", "video/mp4")));
		assertEquals("gif", t.getUserSubtitle());
	}

	@Test
	public void itExpandsTwitterVideo () throws Exception {
		final Status s = mockTweet("media.");
		final ExtendedMediaEntity me1 = mockExtendedMediaEntry("http://twitter.com/twitter/status/560070183650213889/video/1", "https://pbs.twimg.com/ext_tw_video_thumb/560070131976392705/pu/img/TcG_ep5t-iqdLV5R.jpg");
		final Variant v1 = mock(Variant.class);
		final Variant v2 = mock(Variant.class);
		final Variant v3 = mock(Variant.class);
		final Variant v4 = mock(Variant.class);
		final Variant v5 = mock(Variant.class);
		when(s.getMediaEntities()).thenReturn(new ExtendedMediaEntity[] { me1 });
		when(me1.getType()).thenReturn("video");
		when(me1.getVideoDurationMillis()).thenReturn(30033L);
		when(me1.getVideoVariants()).thenReturn(new Variant[] { v1, v2, v3, v4, v5 });

		when(v1.getContentType()).thenReturn("video/mp4");
		when(v1.getBitrate()).thenReturn(2176000);
		when(v1.getUrl()).thenReturn("https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/1280x720/c4E56sl91ZB7cpYi.mp4");

		when(v2.getContentType()).thenReturn("video/mp4");
		when(v2.getBitrate()).thenReturn(320000);
		when(v2.getUrl()).thenReturn("https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/320x180/nXXsvs7vOhcMivwl.mp4");

		when(v3.getContentType()).thenReturn("video/webm");
		when(v3.getBitrate()).thenReturn(832000);
		when(v3.getUrl()).thenReturn("https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/640x360/vmLr5JlVs2kBLrXS.webm");

		when(v4.getContentType()).thenReturn("video/mp4");
		when(v4.getBitrate()).thenReturn(832000);
		when(v4.getUrl()).thenReturn("https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/640x360/vmLr5JlVs2kBLrXS.mp4");

		when(v5.getContentType()).thenReturn("application/x-mpegURL");
		when(v5.getUrl()).thenReturn("https://video.twimg.com/ext_tw_video/560070131976392705/pu/pl/r1kgzh5PmLgium3-.m3u8");

		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, "https://pbs.twimg.com/ext_tw_video_thumb/560070131976392705/pu/img/TcG_ep5t-iqdLV5R.jpg", "http://twitter.com/twitter/status/560070183650213889/video/1")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.URL, "https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/1280x720/c4E56sl91ZB7cpYi.mp4", "video/mp4 0:30 2.1 MB/s")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.URL, "https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/320x180/nXXsvs7vOhcMivwl.mp4", "video/mp4 0:30 312.5 KB/s")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.URL, "https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/640x360/vmLr5JlVs2kBLrXS.webm", "video/webm 0:30 812.5 KB/s")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.URL, "https://video.twimg.com/ext_tw_video/560070131976392705/pu/vid/640x360/vmLr5JlVs2kBLrXS.mp4", "video/mp4 0:30 812.5 KB/s")));
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.URL, "https://video.twimg.com/ext_tw_video/560070131976392705/pu/pl/r1kgzh5PmLgium3-.m3u8", "application/x-mpegURL 0:30")));
		assertEquals("video", t.getUserSubtitle());
	}

	@Test
	public void itExpandsInstagramUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://instagram.com/p/cT0bSXnMqi", false, "https://instagram.com/p/cT0bSXnMqi/media/?size=m");
		testPictureUrlExpansion("http://instagram.com/p/cT0bSXnMqi/", false, "https://instagram.com/p/cT0bSXnMqi/media/?size=m");
		testPictureUrlExpansion("https://www.instagram.com/p/BFdoIydtZzU/", false, "https://instagram.com/p/BFdoIydtZzU/media/?size=m");
	}

	@Test
	public void itExpandsInstagramUrlsToMediaHd () throws Exception {
		testPictureUrlExpansion("http://instagram.com/p/cT0bSXnMqi/", true, "https://instagram.com/p/cT0bSXnMqi/media/?size=l");
	}

	@Test
	public void itExpandsTwitpicUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://twitpic.com/d53yth", false, "https://twitpic.com/show/thumb/d53yth.jpg");
	}

	// TODO know that http://twitpic.com/photos/dalelane is a gallery (and has no thumb)

	@Test
	public void itExpandsImgurPageUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://imgur.com/oxyFqMy", false, "https://i.imgur.com/oxyFqMyl.jpg");
	}

	@Test
	public void itExpandsImgurPageUrlsToMediaHd () throws Exception {
		testPictureUrlExpansion("http://imgur.com/oxyFqMy", true, "https://i.imgur.com/oxyFqMyh.jpg");
	}

	/**
	 * Needs an Imgur API call to map album to thumb. :(
	 */
	@Test
	public void itExpandsImgurAlbumUrlsToNull () throws Exception {
		testPictureUrlNonExpansion("http://imgur.com/a/8PKUl");
		testPictureUrlNonExpansion("http://imgur.com/r/aww");
		testPictureUrlNonExpansion("http://imgur.com/gallery/FpZwD");
	}

	@Test
	public void itExpandsImgurAlbumEntryUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://imgur.com/r/aww/qGSAGyf", false, "https://i.imgur.com/qGSAGyfl.jpg");
	}

	@Test
	public void itConvertsImgurJpgUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/dhadb0b.jpg", false, "https://i.imgur.com/dhadb0bl.jpg");
	}

	@Test
	public void itConvertsImgurJpgUrlsToMediaHttps () throws Exception {
		testPictureUrlExpansion("https://i.imgur.com/dhadb0b.jpg", false, "https://i.imgur.com/dhadb0bl.jpg");
	}

	@Test
	public void itConvertsImgurPngUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/SOLlJFo.png", false, "https://i.imgur.com/SOLlJFol.jpg");
	}

	@Test
	public void itConvertsImgurGifUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/AyLnEoz.gif", false, "https://i.imgur.com/AyLnEozl.jpg");
	}

	@Test
	public void itConvertsImgurMultiToMediasButOnlyTheFirstFive () throws Exception {
		testPictureUrlExpansion("https://imgur.com/TQcg7B7,okPllv3,zRRz0Zx,IpqDDiZ,GyYMeYy,WeyemzP,8cX3BY0,mXR6EgY,MY4uvJj,3CdRSrX,MkPEr0T", false,
				"https://i.imgur.com/TQcg7B7l.jpg",
				"https://i.imgur.com/okPllv3l.jpg",
				"https://i.imgur.com/zRRz0Zxl.jpg",
				"https://i.imgur.com/IpqDDiZl.jpg",
				"https://i.imgur.com/GyYMeYyl.jpg");
	}

	@Test
	public void itConvertsYfrogUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://yfrog.com/oehccwlqj", false, "http://yfrog.com/oehccwlqj:small");
	}

	@Test
	public void itConvertsYfrogUrlsToMediaHd () throws Exception {
		testPictureUrlExpansion("http://yfrog.com/oehccwlqj", true, "http://yfrog.com/oehccwlqj:medium");
	}

	@Test
	public void itConvertsTwippleUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://p.twipple.jp/4pzli", false, "http://p.twipple.jp/show/large/4pzli");
	}

	@Test
	public void itCanAddHdProfileImages () throws Exception {
		final Status s = mockTweet("foo @twitter desu");
		final Tweet t = TwitterUtils.convertTweet(this.account, s, 201, true);
		assertEquals(s.getUser().getBiggerProfileImageURLHttps(), t.getAvatarUrl());
	}

	@Test
	public void itExtractsMentionsButDoesNotIncludeMentionsOfSelf () throws Exception {
		final Status s = mockTweet("foo @twitter desu");

		final UserMentionEntity ume1 = mockUserMentionEntity("twitter", "Twitter", 100L);
		final UserMentionEntity ume2 = mockUserMentionEntity("self", "Self", 200L);
		when(s.getUserMentionEntities()).thenReturn(new UserMentionEntity[] { ume1, ume2 });

		final Tweet t1 = TwitterUtils.convertTweet(this.account, s, 201, false);
		assertThat(t1.getMetas(), hasItem(new Meta(MetaType.MENTION, "twitter", "Twitter")));
		assertThat(t1.getMetas(), hasItem(new Meta(MetaType.MENTION, "self", "Self")));

		final Tweet t2 = TwitterUtils.convertTweet(this.account, s, 200, false);
		assertThat(t2.getMetas(), hasItem(new Meta(MetaType.MENTION, "twitter", "Twitter")));
		assertThat(t2.getMetas(), not(hasItem(new Meta(MetaType.MENTION, "self", "Self"))));
	}

	@Test
	public void itUsesOrigionalStatusForRetweets () throws Exception {
		final Status s = mockTweet("a thing.", "them", "Them", 202);

		final Status rt = mockTweet("RT @them: a thing.", "friend", "Friend", 201);
		when(rt.isRetweet()).thenReturn(true);
		when(rt.getRetweetedStatus()).thenReturn(s);

		if (s.getCreatedAt().getTime() == rt.getCreatedAt().getTime()) throw new IllegalStateException("Bad mocking: created times are the same.");

		final Tweet t = TwitterUtils.convertTweet(this.account, rt, 200, false);
		assertEquals("a thing.", t.getBody());
		assertEquals("them", t.getUsername());
		assertEquals("via friend", t.getUserSubtitle());
		assertEquals("Them", t.getFullname());
		assertEquals("via Friend", t.getFullSubtitle());
		assertEquals(s.getUser().getProfileImageURLHttps(), t.getAvatarUrl());
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MENTION, "friend", "Friend")));
		assertEquals(rt.getCreatedAt().getTime() / 1000L, t.getTime());
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.POST_TIME, String.valueOf(s.getCreatedAt().getTime() / 1000L))));
	}

	@Test
	public void itDoesNotAddMentionForRetweetsByMe () throws Exception {
		final Status s = mockTweet("a thing.", "them", "Them", 202);

		final Status rt = mockTweet("RT @them: a thing.", "me", "Me", 200);
		when(rt.isRetweet()).thenReturn(true);
		when(rt.getRetweetedStatus()).thenReturn(s);

		if (s.getCreatedAt().getTime() == rt.getCreatedAt().getTime()) throw new IllegalStateException("Bad mocking: created times are the same.");

		final Tweet t = TwitterUtils.convertTweet(this.account, rt, 200, false);
		assertEquals("a thing.", t.getBody());
		assertEquals("them", t.getUsername());
		assertEquals("via me", t.getUserSubtitle());
		assertEquals("Them", t.getFullname());
		assertEquals("via Me", t.getFullSubtitle());
		assertEquals(s.getUser().getProfileImageURLHttps(), t.getAvatarUrl());
		assertThat(t.getMetas(), not(hasItem(new Meta(MetaType.MENTION, "me", "Me"))));
		assertEquals(rt.getCreatedAt().getTime() / 1000L, t.getTime());
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.POST_TIME, String.valueOf(s.getCreatedAt().getTime() / 1000L))));
	}

	@Test
	public void itDoesNotDoubleMentionForRtAtMention () throws Exception {
		final Status s = mockTweet("a thing @bob about a thing.", "them", "Them", 202);
		final UserMentionEntity sUme = mockUserMentionEntity("bob", "Bob", 200L);
		when(s.getUserMentionEntities()).thenReturn(new UserMentionEntity[] { sUme });

		final Status rt = mockTweet("RT @them: a thing @bob about a thing.", "bob", "Bob", 200);
		final UserMentionEntity rtUme = mockUserMentionEntity("bob", "Bob", 200L);
		when(rt.getUserMentionEntities()).thenReturn(new UserMentionEntity[] { rtUme });
		when(rt.isRetweet()).thenReturn(true);
		when(rt.getRetweetedStatus()).thenReturn(s);

		final Tweet t = TwitterUtils.convertTweet(this.account, rt, 100, false);
		assertEquals("a thing @bob about a thing.", t.getBody());
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MENTION, "bob", "Bob")));

		final List<Meta> metasCopy = new ArrayList<Meta>(t.getMetas());
		metasCopy.remove(new Meta(MetaType.MENTION, "bob", "Bob"));
		assertThat(metasCopy, not(hasItem(new Meta(MetaType.MENTION, "bob", "Bob"))));
	}

	private void testPictureUrlExpansion (final String fromUrl, final boolean hdMedia, final String... toUrls) {
		final Status s = mockTweetWithUrl(fromUrl);
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, hdMedia);
		for (final String toUrl : toUrls) {
			assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, toUrl, fromUrl)));
		}
		assertNoMetaOfType(t, MetaType.URL);
	}

	private void testPictureUrlNonExpansion (final String fromUrl) {
		final Status s = mockTweetWithUrl(fromUrl);
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);
		for (final Meta meta : t.getMetas()) {
			if (meta.getType() == MetaType.MEDIA) throw new AssertionError("Unexpected MEDIA in " + t.getMetas());
		}
	}

	private static Status mockTweetWithUrl (final String url) {
		final Status s = mockTweet("link: " + url);

		final URLEntity ue = mock(URLEntity.class);
		when(ue.getExpandedURL()).thenReturn(url);
		when(ue.getURL()).thenReturn(url);
		when(ue.getStart()).thenReturn(6);
		when(ue.getEnd()).thenReturn(6 + url.length());

		when(s.getURLEntities()).thenReturn(new URLEntity[] { ue });

		return s;
	}

	private static Status mockTweetWithMedia (final String mediaPageUrl, final String mediaImgUrl) {
		final Status s = mockTweet("media: " + mediaPageUrl);
		final MediaEntity me = mockMediaEntry(mediaPageUrl, mediaImgUrl);
		when(s.getMediaEntities()).thenReturn(new MediaEntity[] { me });
		return s;
	}

	private static MediaEntity mockMediaEntry (final String mediaPageUrl, final String mediaImgUrl) {
		final MediaEntity me = mock(MediaEntity.class);
		when(me.getURL()).thenReturn(mediaPageUrl);
		when(me.getExpandedURL()).thenReturn(mediaPageUrl);
		when(me.getMediaURLHttps()).thenReturn(mediaImgUrl);
		when(me.getStart()).thenReturn(7);
		when(me.getEnd()).thenReturn(7 + mediaPageUrl.length());
		return me;
	}

	private static ExtendedMediaEntity mockExtendedMediaEntry (final String mediaPageUrl, final String mediaImgUrl) {
		final ExtendedMediaEntity me = mock(ExtendedMediaEntity.class);
		when(me.getURL()).thenReturn(mediaPageUrl);
		when(me.getExpandedURL()).thenReturn(mediaPageUrl);
		when(me.getMediaURLHttps()).thenReturn(mediaImgUrl);
		when(me.getStart()).thenReturn(7);
		when(me.getEnd()).thenReturn(7 + mediaPageUrl.length());
		return me;
	}

	private static Status mockTweet (final String msg) {
		return mockTweet(msg, "screenname", "name", 1234);
	}

	private static long lastMockTime = System.currentTimeMillis();

	private static Status mockTweet (final String msg, final String screenName, final String fullName, final long userId) {
		final Status s = mock(Status.class);
		when(s.getText()).thenReturn(msg);

		final long t = System.currentTimeMillis();
		lastMockTime = t > lastMockTime ? t : lastMockTime + 1;
		when(s.getCreatedAt()).thenReturn(new Date(lastMockTime));

		final User user = mock(User.class);
		when(user.getScreenName()).thenReturn(screenName);
		when(user.getName()).thenReturn(fullName);
		when(user.getId()).thenReturn(userId);
		when(user.getProfileImageURLHttps()).thenReturn("https://profile/" + screenName + "/image");
		when(user.getBiggerProfileImageURLHttps()).thenReturn("https://profile/" + screenName + "/bigger_image");
		when(s.getUser()).thenReturn(user);

		return s;
	}

	private static UserMentionEntity mockUserMentionEntity (final String screenname, final String fullName, final long id) {
		final UserMentionEntity ume = mock(UserMentionEntity.class);
		when(ume.getScreenName()).thenReturn(screenname);
		when(ume.getName()).thenReturn(fullName);
		when(ume.getId()).thenReturn(id);
		return ume;
	}

	private static void assertNoMetaOfType (final Tweet t, final MetaType type) throws AssertionError {
		for (final Meta meta : t.getMetas()) {
			if (meta.getType() == type) throw new AssertionError("Metas should not have any " + type + ": " + t.getMetas());
		}
	}

	@Test
	public void itMakesFriendlyErrorForHostNotFound () throws Exception {
		final UnknownHostException uhe = new UnknownHostException("Unable to resolve host \"api.twitter.com\": No address associated with hostname");
		final TwitterException te = new TwitterException("example", uhe);
		assertEquals("Network error: Unable to resolve host \"api.twitter.com\": No address associated with hostname",
				TwitterUtils.friendlyExceptionMessage(te));
	}

	@Test
	public void itMakesFriendlyErrorForGenericIoException () throws Exception {
		final IOException ioe = new IOException("OMG the network.");
		final TwitterException te = new TwitterException("example", ioe);
		assertEquals("Network error: java.io.IOException: OMG the network.", TwitterUtils.friendlyExceptionMessage(te));
	}

	@Test
	public void itMakesFriendlyErrorForSslConnectionTimeout () throws Exception {
		final SSLException se = new SSLException("Read error: ssl=0x5cbb0ce0: I/O error during system call, Connection timed out");
		final TwitterException te = new TwitterException("example", se);
		assertEquals("Network error: Connection timed out.", TwitterUtils.friendlyExceptionMessage(te));
	}

	@Test
	public void itMakesFriendlyErrorForInternalJsonError () throws Exception {
		final twitter4j.JSONException je = new twitter4j.JSONException("Expected a ',' or '}' at 7733 [character 7734 line 1]");
		final TwitterException te = new TwitterException(je);
		assertEquals("Network error: Invalid or incomplete data received.", TwitterUtils.friendlyExceptionMessage(te));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter64 () throws Exception {
		assertEquals("Your account is suspended and is not permitted to access this feature. :(", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(403, 64)));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter88 () throws Exception {
		assertEquals("Rate limit exceeded.  Please try again in a while.", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(999, 88)));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter89 () throws Exception {
		assertEquals("Invalid or expired token.  Please try reauthenticating.", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(999, 89)));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter130 () throws Exception {
		assertEquals("OMG Twitter is over capacity!", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(503, 130)));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter131 () throws Exception {
		assertEquals("OMG Twitter is down!", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(500, 131)));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter179 () throws Exception {
		assertEquals("You are not authorized to see this status.", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(403, 179)));
	}

	@Test
	public void itMakesFriendlyErrorForTwitter185 () throws Exception {
		assertEquals("You are over daily status update limit.", TwitterUtils.friendlyExceptionMessage(makeTwitterEx(403, 185)));
	}

	private static TwitterException makeTwitterEx (final int httpCode, final int twitterCode) {
		final HttpResponse res = mock(HttpResponse.class);
		when(res.getStatusCode()).thenReturn(httpCode);
		final TwitterException te = new TwitterException("{\"errors\":[{\"message\":\"error\", \"code\": " + twitterCode + "}]}", res);
		return te;
	}

}

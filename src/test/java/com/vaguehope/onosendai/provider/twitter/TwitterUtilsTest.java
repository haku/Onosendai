package com.vaguehope.onosendai.provider.twitter;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;

import javax.net.ssl.SSLException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import twitter4j.internal.http.HttpResponse;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;

@RunWith(RobolectricTestRunner.class)
public class TwitterUtilsTest {

	private Account account;

	@Before
	public void before () throws Exception {
		this.account = mock(Account.class);
	}

	@Test
	public void itExpandsTwitterMedia () throws Exception {
		final Status s = mockTweetWithMedia("https://twitter.com/some*user/status/1235430985/photo/1", "https://pbs.twimg.com/media/BjwsdkfjsAAI-4x.jpg");
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, false);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, "https://pbs.twimg.com/media/BjwsdkfjsAAI-4x.jpg", "https://twitter.com/some*user/status/1235430985/photo/1")));
		assertNoMetaOfType(t, MetaType.URL);
	}

	@Test
	public void itExpandsInstagramUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://instagram.com/p/cT0bSXnMqi/", false, "http://instagram.com/p/cT0bSXnMqi/media/?size=m");
	}

	@Test
	public void itExpandsInstagramUrlsToMediaHd () throws Exception {
		testPictureUrlExpansion("http://instagram.com/p/cT0bSXnMqi/", true, "http://instagram.com/p/cT0bSXnMqi/media/?size=l");
	}

	@Test
	public void itExpandsTwitpicUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://twitpic.com/d53yth", false, "http://twitpic.com/show/thumb/d53yth.jpg");
	}

	@Test
	public void itExpandsImgurPageUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://imgur.com/oxyFqMy", false, "http://i.imgur.com/oxyFqMyl.jpg");
	}

	@Test
	public void itExpandsImgurPageUrlsToMediaHd () throws Exception {
		testPictureUrlExpansion("http://imgur.com/oxyFqMy", true, "http://i.imgur.com/oxyFqMyh.jpg");
	}

	/**
	 * Needs an Imgur API call to map album to thumb. :(
	 */
	@Test
	public void itExpandsImgurAlbumUrlsToNull () throws Exception {
		testPictureUrlNonExpansion("http://imgur.com/a/8PKUl");
		testPictureUrlNonExpansion("http://imgur.com/gallery/FpZwD");
	}

	@Test
	public void itConvertsImgurJpgUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/dhadb0b.jpg", false, "http://i.imgur.com/dhadb0bl.jpg");
	}

	@Test
	public void itConvertsImgurPngUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/SOLlJFo.png", false, "http://i.imgur.com/SOLlJFol.jpg");
	}

	@Test
	public void itConvertsImgurGifUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/AyLnEoz.gif", false, "http://i.imgur.com/AyLnEozl.jpg");
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
		assertEquals("Them", t.getFullname());
		assertEquals(s.getUser().getProfileImageURLHttps(), t.getAvatarUrl());
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MENTION, "friend", "RT by Friend")));
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
		assertEquals("Them", t.getFullname());
		assertEquals(s.getUser().getProfileImageURLHttps(), t.getAvatarUrl());
		assertThat(t.getMetas(), not(hasItem(new Meta(MetaType.MENTION, "me", "RT by Me"))));
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
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MENTION, "bob", "RT by Bob")));
		assertThat(t.getMetas(), not(hasItem(new Meta(MetaType.MENTION, "bob", "Bob"))));
	}

	private void testPictureUrlExpansion (final String fromUrl, final boolean hdMedia, final String toUrl) {
		final Status s = mockTweetWithUrl(fromUrl);
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L, hdMedia);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, toUrl, fromUrl)));
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

		final MediaEntity me = mock(MediaEntity.class);
		when(me.getURL()).thenReturn(mediaPageUrl);
		when(me.getExpandedURL()).thenReturn(mediaPageUrl);
		when(me.getMediaURLHttps()).thenReturn(mediaImgUrl);
		when(me.getStart()).thenReturn(7);
		when(me.getEnd()).thenReturn(7 + mediaPageUrl.length());

		when(s.getMediaEntities()).thenReturn(new MediaEntity[] { me });

		return s;
	}

	private static Status mockTweet (final String msg) {
		return mockTweet(msg, "screenname", "name", 1234);
	}

	private static long lastMockTime = System.currentTimeMillis();

	private static Status mockTweet (final String msg, final String screenName, final String fullName, final long userId) {
		final Status s = mock(Status.class);
		when(s.getText()).thenReturn(msg);

		long t = System.currentTimeMillis();
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
		for (Meta meta : t.getMetas()) {
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
		twitter4j.internal.org.json.JSONException je = new twitter4j.internal.org.json.JSONException("Expected a ',' or '}' at 7733 [character 7734 line 1]");
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

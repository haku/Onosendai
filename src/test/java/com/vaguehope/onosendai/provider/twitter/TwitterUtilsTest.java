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

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

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
	public void itExpandsInstagramUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://instagram.com/p/cT0bSXnMqi/", "http://instagram.com/p/cT0bSXnMqi/media/");
	}

	@Test
	public void itExpandsTwitpicUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://twitpic.com/d53yth", "http://twitpic.com/show/thumb/d53yth.jpg");
	}

	@Test
	public void itExpandsImgurPageUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://imgur.com/oxyFqMy", "http://i.imgur.com/oxyFqMyl.jpg");
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
		testPictureUrlExpansion("http://i.imgur.com/dhadb0b.jpg", "http://i.imgur.com/dhadb0bl.jpg");
	}

	@Test
	public void itConvertsImgurPngUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/SOLlJFo.png", "http://i.imgur.com/SOLlJFol.jpg");
	}

	@Test
	public void itConvertsImgurGifUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://i.imgur.com/AyLnEoz.gif", "http://i.imgur.com/AyLnEozl.jpg");
	}

	@Test
	public void itConvertsYfrogUrlsToMedia () throws Exception {
		testPictureUrlExpansion("http://yfrog.com/oehccwlqj", "http://yfrog.com/oehccwlqj:small");
	}

	@Test
	public void itExtractsMentionsButDoesNotIncludeMentionsOfSelf () throws Exception {
		final Status s = mockTweet("foo @twitter desu");

		final UserMentionEntity ume1 = mockUserMentionEntity("twitter", 100L);
		final UserMentionEntity ume2 = mockUserMentionEntity("self", 200L);
		when(s.getUserMentionEntities()).thenReturn(new UserMentionEntity[] { ume1, ume2 });

		final Tweet t1 = TwitterUtils.convertTweet(this.account, s, 201);
		assertThat(t1.getMetas(), hasItem(new Meta(MetaType.MENTION, "twitter")));
		assertThat(t1.getMetas(), hasItem(new Meta(MetaType.MENTION, "self")));

		final Tweet t2 = TwitterUtils.convertTweet(this.account, s, 200);
		assertThat(t2.getMetas(), hasItem(new Meta(MetaType.MENTION, "twitter")));
		assertThat(t2.getMetas(), not(hasItem(new Meta(MetaType.MENTION, "self"))));
	}

	private void testPictureUrlExpansion (final String fromUrl, final String toUrl) {
		final Status s = mockTweetWithUrl(fromUrl);
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, toUrl)));
	}

	private void testPictureUrlNonExpansion (final String fromUrl) {
		final Status s = mockTweetWithUrl(fromUrl);
		final Tweet t = TwitterUtils.convertTweet(this.account, s, -1L);
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

	private static Status mockTweet (final String msg) {
		final Status s = mock(Status.class);
		when(s.getText()).thenReturn(msg);
		when(s.getCreatedAt()).thenReturn(new Date());

		final User user = mock(User.class);
		when(user.getName()).thenReturn("name");
		when(user.getScreenName()).thenReturn("screenname");
		when(user.getProfileImageURLHttps()).thenReturn("https://profile/image");
		when(s.getUser()).thenReturn(user);

		return s;
	}

	private static UserMentionEntity mockUserMentionEntity (final String screenname, final long id) {
		final UserMentionEntity ume = mock(UserMentionEntity.class);
		when(ume.getScreenName()).thenReturn(screenname);
		when(ume.getId()).thenReturn(id);
		return ume;
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

}

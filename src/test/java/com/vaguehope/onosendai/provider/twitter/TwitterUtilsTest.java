package com.vaguehope.onosendai.provider.twitter;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.xtremelabs.robolectric.RobolectricTestRunner;

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

	private void testPictureUrlExpansion (final String fromUrl, final String toUrl) {
		final Status s = mockTweetWithUrl(fromUrl);
		final Tweet t = TwitterUtils.convertTweet(this.account, s);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA, toUrl)));
	}

	private static Status mockTweetWithUrl (final String url) {
		final Status s = mock(Status.class);
		when(s.getText()).thenReturn("link: " + url);

		final User user = mock(User.class);
		when(user.getName()).thenReturn("name");
		when(user.getScreenName()).thenReturn("screenname");
		when(user.getProfileImageURLHttps()).thenReturn("https://profile/image");
		when(s.getUser()).thenReturn(user);

		when(s.getCreatedAt()).thenReturn(new Date());

		final URLEntity ue = mock(URLEntity.class);
		when(ue.getExpandedURL()).thenReturn(url);
		when(ue.getURL()).thenReturn(url);
		when(ue.getStart()).thenReturn(6);
		when(ue.getEnd()).thenReturn(6 + url.length());

		when(s.getURLEntities()).thenReturn(new URLEntity[] { ue });

		return s;
	}

}

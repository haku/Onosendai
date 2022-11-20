package com.vaguehope.onosendai.provider.mastodon;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sys1yagi.mastodon4j.api.entity.Attachment;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ Status.class, com.sys1yagi.mastodon4j.api.entity.Account.class })
public class MastodonUtilsTest {

	private Account osAccount;

	@Before
	public void before () throws Exception {
		this.osAccount = mock(Account.class);
	}

	@Test
	public void itConvertsAToot() throws Exception {
		final Status s = mockToot("<p>foo</p>", "user@example.com", "A B", 456);
		final Tweet t = MastodonUtils.convertStatusToTweet(this.osAccount, s, 123, null);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.OWNER_NAME, "user@example.com", "A B")));
		assertEquals("foo", t.getBody());
	}

	@Test
	public void itPrependsSpoilerText () throws Exception {
		final Status s = mockToot("<p>foo</p>", "user@example.com", "A B", 456);
		when(s.getSpoilerText()).thenReturn("Spoilers for toot...");
		final Tweet t = MastodonUtils.convertStatusToTweet(this.osAccount, s, 123, null);
		assertEquals("Spoilers for toot...\n\nfoo", t.getBody());
	}

	@Test
	public void itConvertsATootWithAudio () throws Exception {
		final Status s = mockToot("<p>foo</p>", "user@example.com", "A B", 456);

		List<Attachment> ma = new ArrayList<Attachment>();
		ma.add(new Attachment(987, "audio",
				"http://mastodon.example.com/cache/audio.mp3",
				"http://home-server.example.com/media/audio.mp3",
				"http://mastodon.example.com/media/audio.jpg",
				null));
		when(s.getMediaAttachments()).thenReturn(ma);

		final Tweet t = MastodonUtils.convertStatusToTweet(this.osAccount, s, 123, null);
		assertThat(t.getMetas(), hasItem(new Meta(MetaType.MEDIA,
				"http://mastodon.example.com/media/audio.jpg",
				"http://home-server.example.com/media/audio.mp3")));
		assertThat(t.getUsernameWithSubtitle(), containsString("audio"));
	}

	@Test
	public void itConvertsATootWithALink() throws Exception {
		String html = "<p>foo "
				+ "<a href=\"https://example.com/something\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">"
				+ "<span class=\"invisible\">https://</span>"
				+ "<span class=\"ellipsis\">example.com/some</span>"
				+ "<span class=\"invisible\">thing</span></a>"
				+ "</p>";
		String plain = "foo https://example.com/something";
		testHtmlParsing(html, plain);
	}

	@Test
	public void itConvertsATootWithAMention() throws Exception {
		String html = "<p>bar! "
				+ "<span class=\"h-card\">"
				+ "<a href=\"https://example.com/@some_user_name\" class=\"u-url mention\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">"
				+ "@<span>some_user_name</span>"
				+ "</a></span> is bat</p>";
		String plain = "bar! @some_user_name is bat";
		testHtmlParsing(html, plain);
	}

	@Test
	public void itConvertsATootWithATag() throws Exception {
		String html = "<p>thing "
				+ "<a href=\"https://example.com/tags/MyTag\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">"
				+ "#<span>MyTag</span></a> tail</p>";
		String plain = "thing #MyTag tail";
		testHtmlParsing(html, plain);
	}

	@Test
	public void itConvertsATootWithMultipleLinesAndEscapedChars() throws Exception {
		String html = "<p><span class=\"h-card\"><a href=\"https://example.com/@person\" class=\"u-url mention\">@<span>person</span></a></span> bzz:</p>"
				+ "<p>&lt;tag&gt;</p>";
		String plain = "@person bzz:\n<tag>";
		testHtmlParsing(html, plain);
	}

	@Test
	public void itConvertsATootWithLineBreaks() throws Exception {
		String html = "<p>line one?</p>"
				+ "<p>another line.</p>"
				+ "<p>Line 3:<br>"
				+ "<br>  fo "
				+ "after break</p>"
				+ "<p>line 5:<br>"
				+ "sixth line.</p>"
				+ "<p>And <a href=\"https://example.com/tags/TheBestTag\" class=\"mention hashtag\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">"
				+ "#<span>TheBestTag</span></a> after tag.</p>";
		String plain = "line one?\n"
				+ "another line.\n"
				+ "Line 3:\n"
				+ "\n  fo "
				+ "after break\n"
				+ "line 5:\n"
				+ "sixth line.\n"
				+ "And #TheBestTag after tag.";
		testHtmlParsing(html, plain);
	}

	private void testHtmlParsing (final String html, final String plain) {
		final Status s = mockToot(html, "user@example.com", "A B", 456);
		final Tweet t = MastodonUtils.convertStatusToTweet(this.osAccount, s, 123, null);
		assertEquals(plain, t.getBody());
	}

	private static Status mockToot(final String msg, final String acct, final String fullName, final long userId) {
		final Status s = PowerMockito.mock(Status.class);
		when(s.getSpoilerText()).thenReturn("");
		when(s.getContent()).thenReturn(msg);

		when(s.getCreatedAt()).thenReturn("2019-11-26T23:27:31.000Z");

		final com.sys1yagi.mastodon4j.api.entity.Account a = PowerMockito.mock(com.sys1yagi.mastodon4j.api.entity.Account.class);
		when(s.getAccount()).thenReturn(a);
		when(a.getId()).thenReturn(userId);
		when(a.getAcct()).thenReturn(acct);
		when(a.getDisplayName()).thenReturn(fullName);

		return s;
	}

}

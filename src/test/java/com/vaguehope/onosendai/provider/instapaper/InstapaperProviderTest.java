package com.vaguehope.onosendai.provider.instapaper;

import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Instapaper.class, InstapaperProvider.class })
public class InstapaperProviderTest {

	@Mock private Instapaper mockInstapaper;
	@Mock private Account account;

	private InstapaperProvider undertest;

	@Before
	public void before () throws Exception {
		MockitoAnnotations.initMocks(this);
		PowerMockito.whenNew(Instapaper.class).withAnyArguments().thenReturn(this.mockInstapaper);

		this.undertest = new InstapaperProvider();
	}

	@Test
	public void itConvertsBasicTweet () throws Exception {
		final List<Meta> metas = new ArrayList<Meta>();
		final Tweet tweet = new Tweet("sid", "username", "fullname", "body", System.currentTimeMillis(), "avatarUrl", "inlineMediaUrl", metas);

		this.undertest.add(this.account, tweet);

		verify(this.mockInstapaper).add("https://twitter.com/username/status/sid",
				"Tweet by fullname (@username)", "body");
	}

	@Test
	public void itConvertsFacebookUpdate () throws Exception {
		final List<Meta> metas = new ArrayList<Meta>();
		metas.add(new Meta(MetaType.SERVICE, "facebook:561234569"));
		final Tweet tweet = new Tweet("123455200_12345678029520201", null, "fullname\n1like 2 comments", "body", System.currentTimeMillis(), "avatarUrl", "inlineMediaUrl", metas);

		this.undertest.add(this.account, tweet);

		verify(this.mockInstapaper).add("https://www.facebook.com/123455200/posts/12345678029520201",
				"Post by fullname", "body");
	}

	@Test
	public void itConvertsTweetWithLink () throws Exception {
		final List<Meta> metas = new ArrayList<Meta>();
		metas.add(new Meta(MetaType.URL, "http://example.com/thing", "Thing!"));
		final Tweet tweet = new Tweet("sid", "username", "fullname", "body", System.currentTimeMillis(), "avatarUrl", "inlineMediaUrl", metas);

		this.undertest.add(this.account, tweet);

		verify(this.mockInstapaper).add("http://example.com/thing",
				"Thing! via fullname (@username)", "body\n[https://twitter.com/username/status/sid]");
	}

}

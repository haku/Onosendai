package com.vaguehope.onosendai.provider.successwhale;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class SourcesXmlTest {

	@Test
	public void itParsesAllSources () throws Exception {
		final SourcesXml cx = new SourcesXml(getClass().getResourceAsStream("/successwhale_sources.xml"));
		final SuccessWhaleSources s = cx.getSources();

		final List<SuccessWhaleSource> sources = s.getSources();
		assertEquals(9, sources.size());

		assertSource(sources.get(0), "@exampleuser's Home Timeline", "twitter/12365487/statuses/home_timeline");
		assertSource(sources.get(1), "@exampleuser's Own Tweets", "twitter/12365487/statuses/user_timeline");
		assertSource(sources.get(2), "@exampleuser's Mentions", "twitter/12365487/statuses/mentions");
		assertSource(sources.get(3), "@exampleuser's Direct Messages", "twitter/12365487/direct_messages");
		assertSource(sources.get(4), "@exampleuser's Sent Messages", "twitter/12365487/sent_messages");
		assertSource(sources.get(5), "Mx Smith's Home Feed", "facebook/983412343/me/home");
		assertSource(sources.get(6), "Mx Smith's Wall", "facebook/983412343/me");
		assertSource(sources.get(7), "Mx Smith's Events", "facebook/983412343/me/events");
		assertSource(sources.get(8), "Mx Smith's Notifications", "facebook/983412343/me/notifications");
	}

	private static void assertSource (final SuccessWhaleSource s, final String title, final String fullurl) {
		assertEquals(title, s.getUiTitle());
		assertEquals(fullurl, s.getFullurl());
	}

}

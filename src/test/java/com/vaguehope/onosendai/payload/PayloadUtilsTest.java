package com.vaguehope.onosendai.payload;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.vaguehope.onosendai.model.Tweet;

public class PayloadUtilsTest {

	@Test
	public void itDoesNotCrashOnNullOrBlank () throws Exception {
		textLinkExtraction(null, new String[] {});
		textLinkExtraction("", new String[] {});
	}

	@Test
	public void itParsesALinkFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link http://example.com/foo123.", "http://example.com/foo123");
	}

	@Test
	public void itParsesMultipleLinksFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link http://example.com/foo123.  And another www.example.com/bar123...",
				"http://example.com/foo123", "www.example.com/bar123");
	}

	@Test
	public void itParsesAnHttpsLinkFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link https://example.com/foo123.", "https://example.com/foo123");
	}

	@Test
	public void itParsesALinkInBacketsFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link (http://example.com/foo123).", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkInSquareBacketsFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link [http://example.com/foo123].", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkInQuotesFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link \"http://example.com/foo123\".", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkWithoutSchemeButWwwFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link www.example.com/foo123.", "www.example.com/foo123");
	}

	@Test
	public void itParsesALinkWithAFragmentFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link http://example.com/foo123#abc.", "http://example.com/foo123#abc");
	}

	@Ignore("Not sure how to support this one.")
	@Test
	public void itParsesALinkWithoutSchemeOrWwwFromTweet () throws Exception {
		textLinkExtraction("A tweet with a link example.com/foo123.", "example.com/foo123");
	}

	private static void textLinkExtraction (final String body, final String... expectedUrls) {
		Tweet tweet = new Tweet(0L, "", body, 0L);
		PayloadList payloadList = PayloadUtils.extractPayload(tweet);
		assertEquals(expectedUrls.length, payloadList.size());
		for (int i = 0; i < expectedUrls.length; i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.LINK, payload.getType());
			assertEquals(expectedUrls[i], ((LinkPayload) payload).getUrl());
		}
	}

}

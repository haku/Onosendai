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
	public void itParsesALinkInAtTheStartOfTheTweet () throws Exception {
		textLinkExtraction("http://example.com/foo123 A tweet with a link.", "http://example.com/foo123");
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

	@Test
	public void itParsesASimpleHashTag () throws Exception {
		textHashTagExtraction("A tweet with a tag #foo123.", "#foo123");
	}

	@Test
	public void itParsesAHashTagAtStart () throws Exception {
		textHashTagExtraction("#foo123 A tweet with a tag.", "#foo123");
	}

	@Ignore("Not sure how to support this one.")
	@Test
	public void itDoesNotExtractNonTags () throws Exception {
		textHashTagExtraction("On the #16 bus", new String[] {});
		textHashTagExtraction("#0", new String[] {});
	}

	@Test
	public void itExtractKana () throws Exception {
		textHashTagExtraction("this #カタカナ is a hashtag", "#カタカナ");
		textHashTagExtraction("this #ひらがな is a hashtag", "#ひらがな");
		textHashTagExtraction("this ＃ひらがな is a hashtag", "＃ひらがな");
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

	private static void textHashTagExtraction (final String body, final String... expectedTags) {
		Tweet tweet = new Tweet(0L, "", body, 0L);
		PayloadList payloadList = PayloadUtils.extractPayload(tweet);
		assertEquals(expectedTags.length, payloadList.size());
		for (int i = 0; i < expectedTags.length; i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.HASHTAG, payload.getType());
			assertEquals(expectedTags[i], ((HashTagPayload) payload).getHashtag());
		}
	}

}

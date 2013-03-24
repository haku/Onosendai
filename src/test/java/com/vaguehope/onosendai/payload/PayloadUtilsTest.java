package com.vaguehope.onosendai.payload;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetBuilder;

public class PayloadUtilsTest {

	@Test
	public void itDoesNotCrashOnNullOrBlank () throws Exception {
		testLinkExtraction(null, new String[] {});
		testLinkExtraction("", new String[] {});
	}

	@Test
	public void itParsesALinkFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link http://example.com/foo123.", "http://example.com/foo123");
	}

	@Test
	public void itParsesMultipleLinksFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link http://example.com/foo123.  And another www.example.com/bar123...",
				"http://example.com/foo123", "www.example.com/bar123");
	}

	@Test
	public void itParsesAnHttpsLinkFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link https://example.com/foo123.", "https://example.com/foo123");
	}

	@Test
	public void itParsesALinkInAtTheStartOfTheTweet () throws Exception {
		testLinkExtraction("http://example.com/foo123 A tweet with a link.", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkInBacketsFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link (http://example.com/foo123).", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkInSquareBacketsFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link [http://example.com/foo123].", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkInQuotesFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link \"http://example.com/foo123\".", "http://example.com/foo123");
	}

	@Test
	public void itParsesALinkWithoutSchemeButWwwFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link www.example.com/foo123.", "www.example.com/foo123");
	}

	@Test
	public void itParsesALinkWithAFragmentFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link http://example.com/foo123#abc.", "http://example.com/foo123#abc");
	}

	@Ignore("Not sure how to support this one.")
	@Test
	public void itParsesALinkWithoutSchemeOrWwwFromTweet () throws Exception {
		testLinkExtraction("A tweet with a link example.com/foo123.", "example.com/foo123");
	}

	@Test
	public void itParsesASimpleHashTag () throws Exception {
		testHashTagExtraction("A tweet with a tag #foo123.", "#foo123");
	}

	@Test
	public void itParsesAHashTagAtStart () throws Exception {
		testHashTagExtraction("#foo123 A tweet with a tag.", "#foo123");
	}

	@Ignore("Not sure how to support this one.")
	@Test
	public void itDoesNotExtractNonTags () throws Exception {
		testHashTagExtraction("On the #16 bus", new String[] {});
		testHashTagExtraction("#0", new String[] {});
	}

	@Test
	public void itExtractsKana () throws Exception {
		testHashTagExtraction("this #カタカナ is a hashtag", "#カタカナ");
		testHashTagExtraction("this #ひらがな is a hashtag", "#ひらがな");
		testHashTagExtraction("this ＃ひらがな is a hashtag", "＃ひらがな");
	}

	@Test
	public void itExtractsMention () throws Exception {
		testMentionExtraction("@auser how are you?", "@auser");
	}

	@Test
	public void itExtractsMentions () throws Exception {
		testMentionExtraction("@auser where is @buser?", "@auser", "@buser");
	}

	private static void testLinkExtraction (final String body, final String... expectedUrls) {
		Tweet tweet = new TweetBuilder().body(body).build();
		PayloadList payloadList = PayloadUtils.extractPayload(0, tweet);
		payloadList = removeNotOfType(PayloadType.LINK, payloadList);

		assertEquals(expectedUrls.length, payloadList.size());
		for (int i = 0; i < expectedUrls.length; i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.LINK, payload.getType());
			assertEquals(expectedUrls[i], ((LinkPayload) payload).getUrl());
		}
	}

	private static void testHashTagExtraction (final String body, final String... expectedTags) {
		Tweet tweet = new TweetBuilder().body(body).build();
		PayloadList payloadList = PayloadUtils.extractPayload(0, tweet);
		payloadList = removeNotOfType(PayloadType.HASHTAG, payloadList);
		assertEquals(expectedTags.length, payloadList.size());
		for (int i = 0; i < expectedTags.length; i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.HASHTAG, payload.getType());
			assertEquals(expectedTags[i], ((HashTagPayload) payload).getHashtag());
		}
	}

	private static void testMentionExtraction (final String body, final String... expectedMentions) {
		Tweet tweet = new TweetBuilder().body(body).username("user").build();
		PayloadList payloadList = PayloadUtils.extractPayload(0, tweet);
		payloadList = removeNotOfType(PayloadType.MENTION, payloadList);

		List<String> expected = new ArrayList<String>(Arrays.asList(expectedMentions));
		expected.add("@user");

		assertEquals(expected.size(), payloadList.size());
		for (int i = 0; i < expected.size(); i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.MENTION, payload.getType());
			assertEquals(expected.get(i), ((MentionPayload) payload).getScreenName());
		}
	}

	private static PayloadList removeNotOfType(final PayloadType type, final PayloadList payloadList) {
		List<Payload> ret = new ArrayList<Payload>();
		for (Payload p : payloadList.getPayloads()) {
			if (p.getType() == type) ret.add(p);
		}
		return new PayloadList(ret);
	}

}

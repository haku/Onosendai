package com.vaguehope.onosendai.payload;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetBuilder;

@RunWith(RobolectricTestRunner.class)
public class PayloadUtilsTest {

	private static final String ACCOUNT_ID = "ac0";
	private static final Account ACCOUNT = new Account(ACCOUNT_ID, null, null, null, null, null, null);

	private Config conf;

	@Before
	public void before () throws Exception {
		this.conf = mock(Config.class);
		when(this.conf.getAccount(ACCOUNT_ID)).thenReturn(ACCOUNT);
	}

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

	@Test
	public void itDoesNotExtractNonTags () throws Exception {
		testHashTagExtraction("#", new String[] {});
		testHashTagExtraction("something #", new String[] {});
	}

	@Ignore("Not sure how to support this one.")
	@Test
	public void itDoesNotExtractNonTagsHard () throws Exception {
		testHashTagExtraction("On the #16 bus", new String[] {});
		testHashTagExtraction("#0", new String[] {});
	}

	@Test
	public void itExtractsKana () throws Exception {
		testHashTagExtraction("this #カタカナ is a hashtag", "#カタカナ");
		testHashTagExtraction("this #ひらがな is a hashtag", "#ひらがな");
		testHashTagExtraction("this ＃ひらがな is a hashtag", "＃ひらがな");
	}

	private void testLinkExtraction (final String body, final String... expectedUrls) {
		Tweet tweet = new TweetBuilder().body(body).meta(MetaType.ACCOUNT, ACCOUNT_ID).build();
		PayloadList payloadList = PayloadUtils.makePayloads(Robolectric.application, this.conf, tweet);
		payloadList = removeNotOfType(PayloadType.LINK, payloadList);

		assertEquals(expectedUrls.length, payloadList.size());
		for (int i = 0; i < expectedUrls.length; i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.LINK, payload.getType());
			assertEquals(expectedUrls[i], ((LinkPayload) payload).getTitle());
		}
	}

	private void testHashTagExtraction (final String body, final String... expectedTags) {
		Tweet tweet = new TweetBuilder().body(body).meta(MetaType.ACCOUNT, ACCOUNT_ID).build();
		PayloadList payloadList = PayloadUtils.makePayloads(Robolectric.application, this.conf, tweet);
		payloadList = removeNotOfType(PayloadType.HASHTAG, payloadList);
		assertEquals(expectedTags.length, payloadList.size());
		for (int i = 0; i < expectedTags.length; i++) {
			Payload payload = payloadList.getPayload(i);
			assertEquals(PayloadType.HASHTAG, payload.getType());
			assertEquals(expectedTags[i], ((HashTagPayload) payload).getTitle());
		}
	}

	private static PayloadList removeNotOfType (final PayloadType type, final PayloadList payloadList) {
		List<Payload> ret = new ArrayList<Payload>();
		for (Payload p : payloadList.getPayloads()) {
			if (p.getType() == type) ret.add(p);
		}
		return new PayloadList(ret);
	}

}

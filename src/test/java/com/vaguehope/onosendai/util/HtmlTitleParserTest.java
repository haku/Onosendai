package com.vaguehope.onosendai.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.text.SpannableStringBuilder;

// https://github.com/danialgoodwin/dev/blob/master/android/testing/robolectric-cheat-sheet.md#error-for-robolectric-22
@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class HtmlTitleParserTest {

	private static String SIMPLE_HTML_TITLE = "<!DOCTYPE html>\n"
			+ "<html>\n"
			+ "<head>\n"
			+ "<title>some title &gt; goes here</title>\n"
			+ "</head>\n"
			+ "<body>\n"
			+ "foo<br>bar\n"
			+ "</body>\n"
			+ "</html>\n";

	private URLConnection con;

	@Before
	public void before () throws Exception {
		this.con = mock(URLConnection.class);
		when(this.con.getHeaderField("Content-Type")).thenReturn("Content-Type: text/html; charset=ISO-8859-1");
	}

	@Test
	public void itReturnsNullForNotFound () throws Exception {
		runTest("<html></html>", null);
	}

	@Test
	public void itParsesSimpleTitle () throws Exception {
		runTest(SIMPLE_HTML_TITLE, "some title > goes here");
	}

	@Test
	public void itParsesSimpleTitleInIncompleteHtml () throws Exception {
		runTest(SIMPLE_HTML_TITLE.substring(0, SIMPLE_HTML_TITLE.length() - 10), "some title > goes here");
	}

	private void runTest (final String body, final String expectedTitle) throws Exception {
		assertEquals(expectedTitle != null ? new SpannableStringBuilder(expectedTitle) : null,
				HtmlTitleParser.INSTANCE.handleStream(this.con, new ByteArrayInputStream(body.getBytes()), body.length()));
	}

}

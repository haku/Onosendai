package com.vaguehope.onosendai.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.text.SpannableStringBuilder;

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

	@Test
	public void itParsesSimpleTitle () throws Exception {
		final URLConnection con = mock(URLConnection.class);
		when(con.getHeaderField("Content-Type")).thenReturn("Content-Type: text/html; charset=ISO-8859-1");

		assertEquals(new SpannableStringBuilder("some title > goes here"),
				HtmlTitleParser.INSTANCE.handleStream(con, new ByteArrayInputStream(SIMPLE_HTML_TITLE.getBytes()), SIMPLE_HTML_TITLE.length()));
	}

}

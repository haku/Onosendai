package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLConnection;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.Html;
import android.text.Spanned;

import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;

public enum HtmlTitleParser implements HttpStreamHandler<Spanned> {
	INSTANCE;

	private static final String CHARSET = "charset=";
	private static final int MAX_SEARCH_LENGTH = 10 * 1024; // If the </title> is more than this into the page then... well dam.

	@Override
	public void onError (final Exception e) {/* Unused. */}

	@Override
	public Spanned handleStream (final URLConnection connection, final InputStream is, final int contentLength) throws IOException {
		final String charset = parseCharset(connection.getHeaderField("Content-Type"));
		try {
			final HtmlTitleHandler h = new HtmlTitleHandler();
			SAXParserImpl.newInstance(null).parse(new InputSource(new StringReader(IoHelper.toString(is, MAX_SEARCH_LENGTH, charset))), h);
			return Html.fromHtml(h.getTitle());
		}
		catch (final SAXException e) {
			throw new IOException(e.toString(), e);
		}
	}

	// e.g. Content-Type: text/html; charset=ISO-8859-1
	private static String parseCharset (final String contentType) {
		if (contentType == null) return null;
		for (String part : contentType.split(";")) {
			if (part != null) {
				part = part.trim();
				if (part.startsWith(CHARSET)) {
					return part.substring(CHARSET.length()).trim();
				}
			}
		}
		return null;
	}

	private static class HtmlTitleHandler extends DefaultHandler {

		private final StringBuilder currentText = new StringBuilder();

		private boolean keepChar = false;
		private String title;

		public HtmlTitleHandler () {}

		public String getTitle () {
			return this.title;
		}

		@Override
		public void startElement (final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
			final String elementName = !localName.isEmpty() ? localName : qName;

			if (this.title == null && "title".equalsIgnoreCase(elementName)) {
				this.keepChar = true;
				this.currentText.setLength(0);
			}
		}

		@Override
		public void endElement (final String uri, final String localName, final String qName) throws SAXException {
			final String elementName = !localName.isEmpty() ? localName : qName;

			if (this.title == null && "title".equalsIgnoreCase(elementName)) {
				this.title = this.currentText.toString().trim();
				this.keepChar = false;
			}
		}

		@Override
		public void characters (final char[] ch, final int start, final int length) throws SAXException {
			if (this.keepChar) this.currentText.append(ch, start, length);
		}
	}

}

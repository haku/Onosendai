package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.util.StringHelper;

public class ColumnsXml implements ContentHandler {

	private static final int DEFAULT_COLUMN_REFRESH_MINS = 30;

	private final Account account;
	private final List<Column> columns = new ArrayList<Column>();

	public ColumnsXml (final Account account, final InputStream is) throws SAXException {
		this.account = account;
		parse(new InputSource(is));
	}

	private void parse (final InputSource source) throws SAXException {
		final SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			final SAXParser sp = spf.newSAXParser();
			final XMLReader xmlReader = sp.getXMLReader();
			xmlReader.setContentHandler(this);
			try {
				xmlReader.parse(source);
			}
			catch (final IOException e) {
				throw new SAXException(e);
			}
		}
		catch (final ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

	public SuccessWhaleColumns getColumns () {
		return new SuccessWhaleColumns(this.columns);
	}

	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	private String stashedFullpath;
	private String stashedTitle;

	@Override
	public void startElement (final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		this.stack.push(localName);
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
	}

	@Override
	public void endElement (final String uri, final String localName, final String qName) throws SAXException {
		final String elementName = !localName.isEmpty() ? localName : qName;
		if (this.stack.size() == 3) { // NOSONAR not a magic number.
			if ("column".equals(elementName)) {
				if (!StringHelper.isEmpty(this.stashedFullpath)) {
					this.columns.add(new Column(this.columns.size(), this.stashedTitle, this.account.getId(), this.stashedFullpath, DEFAULT_COLUMN_REFRESH_MINS, null, false, false));
				}
				this.stashedFullpath = null;
				this.stashedTitle = null;
			}
		}
		else if (this.stack.size() == 4) { // NOSONAR not a magic number.
			if ("fullpath".equals(elementName)) {
				this.stashedFullpath = this.currentText.toString();
			}
			else if ("title".equals(elementName)) {
				this.stashedTitle = this.currentText.toString();
			}
		}

		this.stack.pop();
	}

	@Override
	public void characters (final char[] ch, final int start, final int length) throws SAXException {
		this.currentText.append(ch, start, length);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void endDocument () throws SAXException { /* UNUSED */}

	@Override
	public void endPrefixMapping (final String prefix) throws SAXException { /* UNUSED */}

	@Override
	public void ignorableWhitespace (final char[] ch, final int start, final int length) throws SAXException { /* UNUSED */}

	@Override
	public void processingInstruction (final String target, final String data) throws SAXException { /* UNUSED */}

	@Override
	public void setDocumentLocator (final Locator locator) { /* UNUSED */}

	@Override
	public void skippedEntity (final String name) throws SAXException { /* UNUSED */}

	@Override
	public void startDocument () throws SAXException { /* UNUSED */}

	@Override
	public void startPrefixMapping (final String prefix, final String uri) throws SAXException { /* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

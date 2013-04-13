package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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

import com.vaguehope.onosendai.provider.ServiceRef;

public class PostToAccountsXml implements ContentHandler {

	private final List<ServiceRef> accounts = new ArrayList<ServiceRef>();

	public PostToAccountsXml (final InputStream is) throws SAXException {
		parse(new InputSource(is));
	}

	public PostToAccountsXml (final Reader reader) throws SAXException {
		parse(new InputSource(reader));
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

	public List<ServiceRef> getAccounts () {
		return this.accounts;
	}

	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	private String stashedService;
	private String stashedUsername;
	private String stashedUid;
	private boolean stashedEnabled;

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
			if ("posttoaccount".equals(elementName)) {
				this.accounts.add(new ServiceRef(this.stashedService, this.stashedUid, this.stashedUsername, this.stashedEnabled));
				this.stashedService = null;
				this.stashedUid = null;
				this.stashedUsername = null;
				this.stashedEnabled = false;
			}
		}
		else if (this.stack.size() == 4) { // NOSONAR not a magic number.
			if ("service".equals(elementName)) {
				this.stashedService = this.currentText.toString();
			}
			else if ("username".equals(elementName)) {
				this.stashedUsername = this.currentText.toString();
			}
			else if ("uid".equals(elementName)) {
				this.stashedUid = this.currentText.toString();
			}
			else if ("enabled".equals(elementName)) {
				this.stashedEnabled = Boolean.parseBoolean(this.currentText.toString());
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

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

import com.vaguehope.onosendai.util.StringHelper;

public class SourcesXml implements ContentHandler {

	private final List<SuccessWhaleSource> sources = new ArrayList<SuccessWhaleSource>();

	public SourcesXml (final InputStream is) throws SAXException {
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

	public SuccessWhaleSources getSources () {
		return new SuccessWhaleSources(this.sources);
	}

	private final Stack<String> stack = new Stack<String>();
	private StringBuilder currentText;
	private String stashedFullname;
	private String stashedFullurl;

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
			if ("source".equals(elementName)) {
				if (!StringHelper.isEmpty(this.stashedFullname) && !StringHelper.isEmpty(this.stashedFullurl)) {
					this.sources.add(new SuccessWhaleSource(this.stashedFullname, this.stashedFullurl));
				}
				this.stashedFullname = null;
				this.stashedFullurl = null;
			}
		}
		else if (this.stack.size() == 4) { // NOSONAR not a magic number.
			if ("fullname".equals(elementName)) {
				this.stashedFullname = this.currentText.toString();
			}
			else if ("fullurl".equals(elementName)) {
				this.stashedFullurl = this.currentText.toString();
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

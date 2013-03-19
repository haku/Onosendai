/*
 * Copyright 2010 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetBuilder;
import com.vaguehope.onosendai.model.TweetList;

public class SuccessWhaleFeedXml implements ContentHandler {

	private final List<Tweet> tweets = new LinkedList<Tweet>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public SuccessWhaleFeedXml (final InputStream dataIs) throws SAXException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xmlReader = sp.getXMLReader();
			xmlReader.setContentHandler(this);
			try {
				xmlReader.parse(new InputSource(dataIs));
			}
			catch (IOException e) {
				throw new SAXException(e);
			}
		}
		catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public TweetList getTweets () {
		return new TweetList(this.tweets);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Stack<String> stack = new Stack<String>();
	private final TweetBuilder currentItem = new TweetBuilder();
	private StringBuilder currentText;

	private final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");

//	<feed>
//	  <success type="boolean">true</success>
//	  <items type="array">
//	    <item>
//	      <service type="symbol">twitter</service>
//	      <content>
//	        <text>a tweet</text>
//	        <id type="integer">113751352559691810</id>
//	        <time type="datetime">2013-03-18T22:38:01+00:00</time>
//	        <fromuser>username</fromuser>

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
		if (this.stack.size() == 4 && elementName.equals("content")) {
			this.tweets.add(this.currentItem.build());
		}
		else if (this.stack.size() == 5) {
			if (elementName.equals("id")) {
				long v = Long.parseLong(this.currentText.toString());
				this.currentItem.id(v);
			}
			else if (elementName.equals("fromuser")) {
				this.currentItem.username(this.currentText.toString());
			}
			else if (elementName.equals("text")) {
				this.currentItem.body(this.currentText.toString());
			}
			else if (elementName.equals("time")) {
				long millis = this.dateFormat.parseMillis(this.currentText.toString());
				this.currentItem.unitTimeSeconds(millis / 1000L);
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

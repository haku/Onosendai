package com.vaguehope.onosendai.provider.mastodon;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sys1yagi.mastodon4j.api.entity.Account;
import com.sys1yagi.mastodon4j.api.entity.Attachment;
import com.sys1yagi.mastodon4j.api.entity.Mention;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.entity.Tag;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

// https://docs.joinmastodon.org/entities/account/
// https://docs.joinmastodon.org/entities/status/

public class MastodonUtils {

	private static final LogWrapper LOG = new LogWrapper("MU");

	static Tweet convertStatusToTweet (
			final com.vaguehope.onosendai.config.Account account,
			final Status status,
			final long ownId,
			final Collection<Meta> extraMetas) {
		final Account statusUser = status.getAccount();
		final long statusUserId = statusUser != null ? statusUser.getId() : -1;
		final String statusUserUsername = statusUser != null ? statusUser.getAcct() : null;

		// The order things are added to these lists is important.
		final List<Meta> metas = new ArrayList<Meta>();
		final List<String> userSubtitle = new ArrayList<String>();

		metas.add(new Meta(MetaType.ACCOUNT, account.getId()));
		if (extraMetas != null) metas.addAll(extraMetas);
		if (statusUser != null) metas.add(new Meta(MetaType.OWNER_NAME, statusUser.getAcct(), statusUser.getDisplayName()));

		final Account viaUser;
		if (status.getReblog() != null) {
			viaUser = statusUser;
			metas.add(new Meta(MetaType.POST_TIME, String.valueOf(parseTimestampToSecondsSinceEpoch(status.getReblog().getCreatedAt()))));
		}
		else {
			viaUser = null;
		}

		final Status s = status.getReblog() != null ? status.getReblog() : status;

		if (s.getAccount() == null) throw new IllegalStateException("Status has null user: " + s);

		addMedia(s, metas, userSubtitle);
		// TODO scan for URLs and add them as metas.
		// TODO scan for external image hosts.
		addHashtags(s, metas);
		addMentions(s, metas, statusUserId, ownId);

		if (viaUser != null && viaUser.getId() != ownId) metas.add(new Meta(MetaType.MENTION, viaUser.getAcct(), viaUser.getDisplayName()));

		if (statusUserId == ownId) metas.add(new Meta(MetaType.EDIT_SID, status.getId()));
		if (s.getInReplyToId() != null) {
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getInReplyToId())));
		}
		else if (s.getReblog() != null && s.getReblog().getId() > 0) { // FIXME should be status no s?
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getReblog().getId())));
		}

		final long unitTimeSeconds = parseTimestampToSecondsSinceEpoch(s.getCreatedAt());

		// TODO Search for quoted tweets?
		final String quotedSid = null;

		final int mediaCount = MetaUtils.countMetaOfType(metas, MetaType.MEDIA);
		if (mediaCount > 1) userSubtitle.add(String.format("%s pictures", mediaCount)); //ES

		if (viaUser != null) userSubtitle.add(String.format("via %s", viaUser.getAcct())); //ES
		final String fullSubtitle = viaUser != null ? String.format("via %s", viaUser.getDisplayName()) : null; //ES

		String body = deHtmlBody(s.getContent());
		if (StringHelper.notEmpty(s.getSpoilerText())) {
			body = s.getSpoilerText() + "\n\n" + body;
		}

		return new Tweet(
				String.valueOf(s.getId()),
				s.getAccount().getAcct(),
				s.getAccount().getDisplayName(),
				userSubtitle.size() > 0 ? ArrayHelper.join(userSubtitle, ", ") : null,
				fullSubtitle,
				statusUserUsername,
				body,
				unitTimeSeconds,  // columns are sorted by this one.
				s.getAccount().getAvatar(),
				MetaUtils.firstMetaOfTypesData(metas, MetaType.MEDIA),
				quotedSid,
				metas);
	}

	private static long parseTimestampToSecondsSinceEpoch (final String stringTimeStamp) {
		// TODO extract this.
		// 2017-04-20T15:14:50.148Z
		final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		return TimeUnit.MILLISECONDS.toSeconds(dateFormat.parseMillis(stringTimeStamp));
	}

	private static void addMedia (final Status s, final List<Meta> metas, final List<String> userSubtitle) {
		boolean hasGif = false;
		boolean hasVideo = false;
		boolean hasAudio = false;

		for (final Attachment att : s.getMediaAttachments()) {
			String imgUrl;
			if ("image".equalsIgnoreCase(att.getType())) {
				imgUrl = att.getUrl();
				if (StringHelper.isEmpty(imgUrl)) imgUrl = att.getRemoteUrl();
				if (StringHelper.isEmpty(imgUrl)) imgUrl = att.getPreviewUrl();
			}
			else {
				imgUrl = att.getPreviewUrl();
			}

			String clickUrl = att.getUrl();
			if (StringHelper.isEmpty(clickUrl)) clickUrl = att.getRemoteUrl();
			if (StringHelper.isEmpty(clickUrl)) clickUrl = att.getPreviewUrl();
			metas.add(new Meta(MetaType.MEDIA, imgUrl, clickUrl));

			// If alt-text is ever supported, it goes here.

			if ("gifv".equals(att.getType())) {
				hasGif = true;
			}
			else if ("video".equals(att.getType())) {
				hasVideo = true;
			}
			else if ("audio".equals(att.getType())) {
				hasAudio = true;
			}
		}
		if (hasGif) userSubtitle.add("gif"); //ES
		if (hasVideo) userSubtitle.add("video"); //ES
		if (hasAudio) userSubtitle.add("audio"); //ES
	}

	private static void addHashtags (final Status s, final List<Meta> metas) {
		for (final Tag tag : s.getTags()) {
			// TODO use tag.getUrl() ?
			metas.add(new Meta(MetaType.HASHTAG, tag.getName()));
		}
	}

	private static void addMentions (final Status s, final List<Meta> metas, final long tweetOwnerId, final long tweetViewerId) {
		for (final Mention m : s.getMentions()) {
			if (m.getId() == tweetOwnerId) continue;
			if (m.getId() == tweetViewerId) continue;
			metas.add(new Meta(MetaType.MENTION, m.getAcct()));
		}
	}

	private static String deHtmlBody (final String html) {
		try {
			final HtmlBodyHandler h = new HtmlBodyHandler();
			SAXParserImpl.newInstance(null).parse(new InputSource(new StringReader(html)), h);
			return h.getBody();
		}
		catch (final SAXException e) {
			LOG.w("Failed to parse html body: %s", html);
			return html;
		}
		catch (final IOException e) {
			LOG.w("Failed to parse html body: %s", html);
			return html;
		}
	}

	private static class HtmlBodyHandler extends DefaultHandler {

		private final StringBuilder output = new StringBuilder();
		private final StringBuilder currentChars = new StringBuilder();

		public String getBody () {
			return this.output.toString().trim();
		}

		private void startTag (final String tag, final Attributes attributes) {
			//
		}

		private void endTag (final String tag) {
			this.output.append(this.currentChars);
			if ("p".equals(tag)) {
				if (this.output.length() > 0) this.output.append("\n");
			}
			else if ("br".equals(tag)) {
				this.output.append("\n");
			}
			this.currentChars.setLength(0);
		}

		@Override
		public void startElement (final String uri, final String localName, final String qName, final Attributes attributes) {
			startTag(localName.toLowerCase(Locale.ENGLISH), attributes);
		}

		@Override
		public void endElement (final String uri, final String localName, final String qName) {
			endTag(localName.toLowerCase(Locale.ENGLISH));
		}

		@Override
		public void characters (final char[] ch, final int start, final int length) throws SAXException {
			this.currentChars.append(ch, start, length);
		}

	}

}

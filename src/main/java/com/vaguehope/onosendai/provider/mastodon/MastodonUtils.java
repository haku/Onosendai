package com.vaguehope.onosendai.provider.mastodon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
import com.vaguehope.onosendai.util.StringHelper;

public class MastodonUtils {

	static Tweet convertStatusToTweet (
			final com.vaguehope.onosendai.config.Account account,
			final Status status,
			final long ownId,
			final Collection<Meta> extraMetas) {
		final Account statusUser = status.getAccount();
		final long statusUserId = statusUser != null ? statusUser.getId() : -1;

		// The order things are added to these lists is important.
		final List<Meta> metas = new ArrayList<Meta>();
		final List<String> userSubtitle = new ArrayList<String>();

		metas.add(new Meta(MetaType.ACCOUNT, account.getId()));
		if (extraMetas != null) metas.addAll(extraMetas);
		if (statusUser != null) metas.add(new Meta(MetaType.OWNER_NAME, statusUser.getUserName(), statusUser.getDisplayName()));

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

		if (viaUser != null && viaUser.getId() != ownId) metas.add(new Meta(MetaType.MENTION, viaUser.getUserName(), viaUser.getDisplayName()));

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

		if (viaUser != null) userSubtitle.add(String.format("via %s", viaUser.getUserName())); //ES
		final String fullSubtitle = viaUser != null ? String.format("via %s", viaUser.getDisplayName()) : null; //ES

		return new Tweet(
				String.valueOf(s.getId()),
				s.getAccount().getUserName(),
				s.getAccount().getDisplayName(),
				userSubtitle.size() > 0 ? ArrayHelper.join(userSubtitle, ", ") : null,
				fullSubtitle,
				s.getContent(),
				unitTimeSeconds,
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

		for (final Attachment me : s.getMediaAttachments()) {
			final String imgUrl = me.getPreviewUrl();
			final String clickUrl = StringHelper.notEmpty(me.getRemoteUrl()) ? me.getRemoteUrl() : me.getPreviewUrl();
			metas.add(new Meta(MetaType.MEDIA, imgUrl, clickUrl));

			// If alt-text is ever supported, it goes here.

			if ("gifv".equals(me.getType())) {
				hasGif = true;
			}
			else if ("video".equals(me.getType())) {
				hasVideo = true;
			}
		}
		if (hasGif) userSubtitle.add("gif"); //ES
		if (hasVideo) userSubtitle.add("video"); //ES
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

}

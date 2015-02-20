package com.vaguehope.onosendai.payload;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.NetworkType;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

public final class PayloadUtils {

	// http://www.regular-expressions.info/unicode.html

	private static final Pattern HASHTAG_PATTERN = Pattern.compile(
			"\\B([#|\uFF03][a-z0-9_\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff\\u3040-\\u309F\\u30A0-\\u30FF]+)", Pattern.CASE_INSENSITIVE);

	private static final LogWrapper LOG = new LogWrapper("PU");

	private PayloadUtils () {
		throw new AssertionError();
	}

	public static PayloadList makePayloads (final Config conf, final Tweet tweet) {
		final Account account = MetaUtils.accountFromMeta(tweet, conf);

		final Set<Payload> set = new LinkedHashSet<Payload>();
		set.add(new PrincipalPayload(tweet));
		replyToOwner(account, tweet, set);
		if (account != null) convertMeta(account, tweet, set);
		extractUrls(tweet, set);
		extractHashTags(tweet, set);
		if (account != null) {
			repliesAndExtractMentions(account, tweet, set);
			addShareOptions(account, tweet, set);
		}

		final List<Payload> sorted = new ArrayList<Payload>(set);
		Collections.sort(sorted, Payload.TYPE_COMP);
		return new PayloadList(sorted);
	}

	private static void addShareOptions (final Account account, final Tweet tweet, final Set<Payload> set) {
		if (account.getProvider() == null) return;
		switch (account.getProvider()) {
			case TWITTER:
				set.add(new SharePayload(tweet, NetworkType.TWITTER));
				break;
			case SUCCESSWHALE:
				final Meta svcMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
				final ServiceRef serviceRef = svcMeta != null ? ServiceRef.parseServiceMeta(svcMeta) : null;
				final NetworkType networkType = serviceRef != null ? serviceRef.getType() : null;
				if (networkType == NetworkType.FACEBOOK) set.add(new AddCommentPayload(account, tweet));
				set.add(new SharePayload(tweet, networkType));
				break;
			default:
				set.add(new SharePayload(tweet));
				break;
		}
	}

	private static void convertMeta (final Account account, final Tweet tweet, final Set<Payload> ret) {
		final List<Meta> metas = tweet.getMetas();
		if (metas == null) return;
		for (final Meta meta : metas) {
			final Payload payload = metaToPayload(account, tweet, meta);
			if (payload != null) ret.add(payload);
		}
	}

	private static Payload metaToPayload (final Account account, final Tweet tweet, final Meta meta) {
		switch (meta.getType()) {
			case MEDIA:
				return new MediaPayload(tweet, meta);
			case HASHTAG:
				return new HashTagPayload(tweet, meta);
			case MENTION:
				return new MentionPayload(account, tweet, meta);
			case URL:
				return new LinkPayload(tweet, meta);
			case EDIT_SID:
				return new EditPayload(tweet, meta);
			case DELETED:
				return new PlaceholderPayload(tweet, String.format("Deleted at %s.", //ES
						DateFormat.getDateTimeInstance().format(new Date(TimeUnit.SECONDS.toMillis(meta.toLong(0L))))));
			case INREPLYTO:
			case SERVICE:
			case ACCOUNT:
			case POST_TIME:
				return null;
			default:
				LOG.e("Unknown meta type: %s", meta.getType());
				return null;
		}
	}

	private static void extractUrls (final Tweet tweet, final Set<Payload> ret) {
		if (payloadsContainsType(ret, PayloadType.LINK)) return;
		if (payloadsContainsType(ret, PayloadType.MEDIA)) return;
		final String text = tweet.getBody();
		if (text == null || text.isEmpty()) return;
		final Matcher m = StringHelper.URL_PATTERN.matcher(text);
		while (m.find()) {
			String g = m.group();
			if (g.startsWith("(") && g.endsWith(")")) g = g.substring(1, g.length() - 1);
			ret.add(new LinkPayload(tweet, g));
		}
	}

	private static void extractHashTags (final Tweet tweet, final Set<Payload> set) {
		if (payloadsContainsType(set, PayloadType.HASHTAG)) return;
		final String text = tweet.getBody();
		if (text == null || text.isEmpty()) return;
		final Matcher m = HASHTAG_PATTERN.matcher(text);
		while (m.find()) {
			final String g = m.group();
			set.add(new HashTagPayload(tweet, g));
		}
	}

	private static void replyToOwner (final Account account, final Tweet tweet, final Set<Payload> set) {
		if (tweet.getUsername() != null) set.add(new MentionPayload(
				account,
				tweet,
				StringHelper.firstLine(tweet.getUsername()),
				StringHelper.firstLine(tweet.getFullname())));
	}

	private static void repliesAndExtractMentions (final Account account, final Tweet tweet, final Set<Payload> set) {
		final String tweetUsername = StringHelper.firstLine(tweet.getUsername());
		final String tweetFullname = StringHelper.firstLine(tweet.getFullname());

		List<String> allMentions = null;
		for (final Meta meta : tweet.getMetas()) {
			if (meta.getType() == MetaType.MENTION && !EqualHelper.equal(tweetUsername, meta.getData())) {
				if (allMentions == null) allMentions = new ArrayList<String>();
				allMentions.add(meta.getData());
			}
		}

		if (allMentions != null && tweetUsername != null) {
			set.add(new MentionPayload(account, tweet, tweetUsername, tweetFullname, allMentions.toArray(new String[allMentions.size()])));
		}
	}

	private static boolean payloadsContainsType (final Collection<Payload> col, final PayloadType type) {
		for (final Payload p : col) {
			if (type == p.getType()) return true;
		}
		return false;
	}

}

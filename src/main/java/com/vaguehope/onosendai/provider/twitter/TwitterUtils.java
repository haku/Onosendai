package com.vaguehope.onosendai.provider.twitter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.MediaEntity.Variant;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.SinceIdType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.DateHelper;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.ImageHostHelper;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;

public final class TwitterUtils {

	private static final int TWITTER_ERROR_CODE_ACCOUNT_SUSPENDED = 64;
	private static final int TWITTER_ERROR_CODE_RATE_LIMIT_EXCEEDED = 88;
	private static final int TWITTER_ERROR_CODE_INVALID_EXPIRED_TOKEN = 89;
	private static final int TWITTER_ERROR_CODE_TWITTER_IS_DOWN = 131;
	private static final int TWITTER_ERROR_CODE_OVER_CAPCACITY = 130;
	private static final int TWITTER_ERROR_CODE_NOT_AUTH_TO_VIEW_STATUS = 179;
	private static final int TWITTER_ERROR_CODE_DAILY_STATUS_LIMIT_EXCEEDED = 185;

	private static final LogWrapper LOG = new LogWrapper("TU");

	private TwitterUtils () {
		throw new AssertionError();
	}

	/*
	 * Paging: https://dev.twitter.com/docs/working-with-timelines
	 * http://twitter4j.org/en/code-examples.html
	 */

	static TweetList fetchTwitterFeed (final Account account, final Twitter t, final FeedGetter getter, final long sinceId, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		final List<Tweet> tweets = new ArrayList<Tweet>();
		final List<Tweet> quotedTweets = new ArrayList<Tweet>();
		final int minCount = getter.recommendedFetchCount();
		final int pageSize = Math.min(minCount, C.TWEET_FETCH_PAGE_SIZE);
		int page = 1; // First page is 1.
		long minId = -1;
		while (tweets.size() < minCount) {
			final Paging paging = new Paging(page, pageSize);
			if (sinceId > 0) paging.setSinceId(sinceId);
			if (minId > 0) paging.setMaxId(minId);
			final ResponseList<Status> timelinePage = getter.getTweets(t, paging);
			LOG.i("Page %d of '%s'(sinceId=%s) contains %d items.", page, getter.toString(), sinceId, timelinePage.size());
			if (timelinePage.size() < 1) break;
			addTweetsToList(tweets, account, timelinePage, t.getId(), hdMedia, extraMetas, quotedTweets);
			minId = TwitterUtils.minIdOf(minId, timelinePage);
			page++;
		}
		return new TweetList(tweets, SinceIdType.SID, quotedTweets);
	}

	static void addTweetsToList (final List<Tweet> list, final Account account, final List<Status> tweets, final long ownId, final boolean hdMedia, final Collection<Meta> extraMetas, final List<Tweet> quotedTweets) {
		for (final Status status : tweets) {
			try {
				list.add(convertTweet(account, status, ownId, hdMedia, extraMetas, quotedTweets));
			}
			catch (final RuntimeException e) { // Better chance of debugging.
				throw new RuntimeException("Failed to convert status: " + status, e);
			}
		}
	}

	static Tweet convertTweet (final Account account, final Status status, final long ownId, final boolean hdMedia) {
		return convertTweet(account, status, ownId, hdMedia, null, null);
	}

	static Tweet convertTweet (final Account account, final Status status, final long ownId, final boolean hdMedia, final Collection<Meta> extraMetas, final List<Tweet> quotedTweets) {
		final User statusUser = status.getUser();
		final long statusUserId = statusUser != null ? statusUser.getId() : -1;
		final String statusUserUsername = statusUser != null ? statusUser.getScreenName() : null;

		// The order things are added to these lists is important.
		final List<Meta> metas = new ArrayList<Meta>();
		final List<String> userSubtitle = new ArrayList<String>();

		metas.add(new Meta(MetaType.ACCOUNT, account.getId()));
		if (extraMetas != null) metas.addAll(extraMetas);
		if (statusUser != null) metas.add(new Meta(MetaType.OWNER_NAME, statusUser.getScreenName(), statusUser.getName()));

		final User viaUser;
		if (status.isRetweet()) {
			viaUser = statusUser;
			metas.add(new Meta(MetaType.POST_TIME, String.valueOf(TimeUnit.MILLISECONDS.toSeconds(status.getRetweetedStatus().getCreatedAt().getTime()))));
		}
		else {
			viaUser = null;
		}

		final Status s = status.isRetweet() ? status.getRetweetedStatus() : status;

		if (s.getUser() == null) throw new IllegalStateException("Status has null user: " + s);

		addMedia(s, metas, hdMedia, userSubtitle);
		checkUrlsForMedia(s, metas, hdMedia);

		final String text = removeMediaAndQuotedUrls(expandUrls(s.getText(), s.getURLEntities(), metas), s);
		addHashtags(s, metas);

		addMentions(s, metas, statusUserId, ownId);
		if (viaUser != null && viaUser.getId() != ownId) metas.add(new Meta(MetaType.MENTION, viaUser.getScreenName(), viaUser.getName()));

		if (statusUserId == ownId) metas.add(new Meta(MetaType.EDIT_SID, status.getId()));
		if (s.getInReplyToStatusId() > 0) {
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getInReplyToStatusId())));
		}
		else if (s.isRetweet() && s.getRetweetedStatus().getId() > 0) { // FIXME should be status no s?
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getRetweetedStatus().getId())));
		}

		final Status q = s.getQuotedStatus();
		if (q != null && quotedTweets != null) {
			metas.add(new Meta(MetaType.QUOTED_SID, q.getId()));
			if (q.getUser() != null) { // Sometimes Twitter does this.  I have no idea why.
				quotedTweets.add(convertTweet(account, q, ownId, hdMedia, extraMetas, quotedTweets));
			}
			else {
				// Its not useful, so let it get loaded later.
				LOG.w("Inline quoted status has null user: " + q);
			}
		}

		final int mediaCount = MetaUtils.countMetaOfType(metas, MetaType.MEDIA);
		if (mediaCount > 1) userSubtitle.add(String.format("%s pictures", mediaCount)); //ES

		if (viaUser != null) userSubtitle.add(String.format("via %s", viaUser.getScreenName())); //ES
		final String fullSubtitle = viaUser != null ? String.format("via %s", viaUser.getName()) : null; //ES

		// https://dev.twitter.com/docs/user-profile-images-and-banners

		return new Tweet(String.valueOf(s.getId()),
				s.getUser().getScreenName(), s.getUser().getName(),
				userSubtitle.size() > 0 ? ArrayHelper.join(userSubtitle, ", ") : null,
				fullSubtitle,
				statusUserUsername,
				text,
				TimeUnit.MILLISECONDS.toSeconds(status.getCreatedAt().getTime()),
				hdMedia ? s.getUser().getBiggerProfileImageURLHttps() : s.getUser().getProfileImageURLHttps(),
				MetaUtils.firstMetaOfTypesData(metas, MetaType.MEDIA),
				q != null ? String.valueOf(q.getId()) : null,
				metas);
	}

	static long minIdOf (final long statingMin, final List<Status> tweets) {
		long min = statingMin;
		for (final Status status : tweets) {
			min = Math.min(min, status.getId());
		}
		return min;
	}

	private static String expandUrls (final String text, final URLEntity[] urls, final List<Meta> metas) {
		if (urls == null || urls.length < 1) return text;

		String textWithUrls = text;
		for (int i = 0; i < urls.length; i++) {
			final URLEntity url = urls[i];
			if (url.getURL() != null && url.getExpandedURL() != null) {
				textWithUrls = StringHelper.replaceOnce(textWithUrls, url.getURL(), url.getExpandedURL());
			}
			final String fullUrl = url.getExpandedURL() != null ? url.getExpandedURL() : url.getURL();
			if (!(url instanceof MediaEntity) && !MetaUtils.containsMetaWithTitle(metas, fullUrl)) { // Image metas have same title.
				metas.add(new Meta(MetaType.URL, fullUrl, url.getDisplayURL()));
			}
		}
		//LOG.d("Expanded '%s' --> '%s'.", text, expandedText);
		return textWithUrls;
	}

	private static void addMedia (final Status s, final List<Meta> metas, final boolean hdMedia, final List<String> userSubtitle) {
		final MediaEntity[] mes = s.getMediaEntities();
		if (mes == null) return;
		boolean hasGif = false;
		boolean hasVideo = false;
		for (final MediaEntity me : mes) {
			final String clickUrl = me.getExpandedURL() != null ? me.getExpandedURL() : me.getURL();
			String imgUrl = me.getMediaURLHttps();
			if (hdMedia) imgUrl += ":large";
			metas.add(new Meta(MetaType.MEDIA, imgUrl, clickUrl));

			if (StringHelper.notEmpty(me.getExtAltText())) {
				metas.add(new Meta(MetaType.ALT_TEXT, me.getExtAltText()));
			}

			final Variant[] variants = me.getVideoVariants();
			if (variants != null) {
				Arrays.sort(variants, VariantOrder.INSTANCE);
				for (final Variant variant : variants) {
					if ("animated_gif".equals(me.getType())) hasGif = true;
					else if ("video".equals(me.getType())) hasVideo = true;

					final StringBuilder title = new StringBuilder();
					title.append(variant.getContentType());
					if (me.getVideoDurationMillis() > 0) title.append(" ").append(DateHelper.formatDurationMillis(me.getVideoDurationMillis()));
					if (variant.getBitrate() > 0) title.append(" ").append(IoHelper.readableFileSize(variant.getBitrate())).append("/s");
					metas.add(new Meta(MetaType.URL, variant.getUrl(), title.toString()));
				}
			}
		}
		if (hasGif) userSubtitle.add("gif"); //ES
		if (hasVideo) userSubtitle.add("video"); //ES
	}

	private static String removeMediaAndQuotedUrls(final String text, final Status s) {
		String textWithoutMedia = text;

		final MediaEntity[] mes = s.getMediaEntities();
		if (mes != null) {
			for (final MediaEntity me : mes) {
				textWithoutMedia = StringHelper.replaceOnce(textWithoutMedia, me.getURL(), "");
				textWithoutMedia = textWithoutMedia.trim();
			}
		}

		final Status q = s.getQuotedStatus();
		if (q != null) {
			final URLEntity urlEntity = findQuotedTweetUrl(s, q);
			if (urlEntity != null) {
				textWithoutMedia = StringHelper.removeSuffex(textWithoutMedia, urlEntity.getURL());
				textWithoutMedia = StringHelper.removeSuffex(textWithoutMedia, urlEntity.getExpandedURL());
				textWithoutMedia = textWithoutMedia.trim();
			}
		}

		return textWithoutMedia;
	}

	private static URLEntity findQuotedTweetUrl (final Status outerTweet, final Status quotedTweet) {
		final String qId = String.valueOf(quotedTweet.getId());

		final URLEntity[] urls = outerTweet.getURLEntities();
		for (int i = 0; i < urls .length; i++) {
			final URLEntity url = urls[i];
			if (StringHelper.endsWith(url.getURL(), qId) || StringHelper.endsWith(url.getExpandedURL(), qId)) {
				return url;
			}
		}

		return null;
	}

	private static void checkUrlsForMedia (final Status s, final List<Meta> metas, final boolean hdMedia) {
		final URLEntity[] urls = s.getURLEntities();
		if (urls == null) return;
		for (final URLEntity url : urls) {
			final String fullUrl = url.getExpandedURL() != null ? url.getExpandedURL() : url.getURL();
			final List<String> thumbUrls = ImageHostHelper.thumbUrl(fullUrl, hdMedia);
			if (thumbUrls != null) {
				for (final String thumbUrl : thumbUrls) {
					metas.add(new Meta(MetaType.MEDIA, thumbUrl, fullUrl));
				}
			}
		}
	}

	private static void addHashtags (final Status s, final List<Meta> metas) {
		final HashtagEntity[] tags = s.getHashtagEntities();
		if (tags == null) return;
		for (final HashtagEntity tag : tags) {
			metas.add(new Meta(MetaType.HASHTAG, tag.getText()));
		}
	}

	private static void addMentions (final Status s, final List<Meta> metas, final long tweetOwnderId, final long tweetViewerId) {
		final UserMentionEntity[] umes = s.getUserMentionEntities();
		if (umes == null) return;
		for (final UserMentionEntity ume : umes) {
			if (ume == null) throw new IllegalStateException("null entry in UME array: " + Arrays.toString(umes));
			if (ume.getId() == tweetOwnderId) continue;
			if (ume.getId() == tweetViewerId) continue;
			metas.add(new Meta(MetaType.MENTION, ume.getScreenName(), ume.getName()));
		}
	}

	public static String friendlyExceptionMessage (final TwitterException e) {
		switch (e.getErrorCode()) {
			case TWITTER_ERROR_CODE_ACCOUNT_SUSPENDED:
				return "Your account is suspended and is not permitted to access this feature. :("; //ES
			case TWITTER_ERROR_CODE_RATE_LIMIT_EXCEEDED:
				return "Rate limit exceeded.  Please try again in a while."; //ES
			case TWITTER_ERROR_CODE_INVALID_EXPIRED_TOKEN:
				return "Invalid or expired token.  Please try reauthenticating."; //ES
			case TWITTER_ERROR_CODE_OVER_CAPCACITY:
				return "OMG Twitter is over capacity!"; //ES
			case TWITTER_ERROR_CODE_TWITTER_IS_DOWN:
				return "OMG Twitter is down!"; //ES
			case TWITTER_ERROR_CODE_NOT_AUTH_TO_VIEW_STATUS:
				return "You are not authorized to see this status."; //ES
			case TWITTER_ERROR_CODE_DAILY_STATUS_LIMIT_EXCEEDED:
				return "You are over daily status update limit."; //ES
			default:
		}
		final Throwable cause = e.getCause();
		if (cause != null) {
			if (cause instanceof UnknownHostException) {
				return "Network error: " + cause.getMessage(); //ES
			}
			else if (cause instanceof IOException && StringHelper.safeContainsIgnoreCase(cause.getMessage(), "connection timed out")) {
				return "Network error: Connection timed out."; //ES
			}
			else if (cause instanceof IOException) {
				return "Network error: " + cause; //ES
			}
			else if (cause instanceof twitter4j.JSONException) {
				return "Network error: Invalid or incomplete data received."; //ES
			}
		}
		return ExcpetionHelper.causeTrace(e);
	}

}

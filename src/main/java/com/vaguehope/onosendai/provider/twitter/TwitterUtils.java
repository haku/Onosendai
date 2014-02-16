package com.vaguehope.onosendai.provider.twitter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.ImageHostHelper;
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

	static TweetList fetchTwitterFeed (final Account account, final Twitter t, final FeedGetter getter, final long sinceId) throws TwitterException {
		final List<Tweet> tweets = new ArrayList<Tweet>();
		final int minCount = getter.recommendedFetchCount();
		final int pageSize = Math.min(minCount, C.TWEET_FETCH_PAGE_SIZE);
		int page = 1; // First page is 1.
		long minId = -1;
		while (tweets.size() < minCount) {
			final Paging paging = new Paging(page, pageSize);
			if (sinceId > 0) paging.setSinceId(sinceId);
			if (minId > 0) paging.setMaxId(minId);
			final ResponseList<Status> timelinePage = getter.getTweets(t, paging);
			LOG.i("Page %d of '%s' contains %d items.", page, getter.toString(), timelinePage.size());
			if (timelinePage.size() < 1) break;
			addTweetsToList(tweets, account, timelinePage, t.getId());
			minId = TwitterUtils.minIdOf(minId, timelinePage);
			page++;
		}
		return new TweetList(tweets);
	}

	static void addTweetsToList (final List<Tweet> list, final Account account, final List<Status> tweets, final long ownId) {
		for (final Status status : tweets) {
			list.add(convertTweet(account, status, ownId));
		}
	}

	static Tweet convertTweet (final Account account, final Status status, final long ownId) {
		final List<Meta> metas = new ArrayList<Meta>();
		metas.add(new Meta(MetaType.ACCOUNT, account.getId()));

		if (status.isRetweet() && status.getUser().getId() != ownId) {
			metas.add(new Meta(MetaType.MENTION, status.getUser().getScreenName(), String.format("RT by %s", status.getUser().getName())));
		}
		final Status s = status.isRetweet() ? status.getRetweetedStatus() : status;

		final URLEntity[] urls = mergeArrays(s.getURLEntities(), s.getMediaEntities());
		final String text = expandUrls(s.getText(), urls, metas);

		if (s.getInReplyToStatusId() > 0) {
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getInReplyToStatusId())));
		}
		else if (s.isRetweet() && s.getRetweetedStatus().getId() > 0) {
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getRetweetedStatus().getId())));
		}

		addMedia(s, metas);
		checkUrlsForMedia(s, metas);
		addHashtags(s, metas);
		addMentions(s, metas, ownId);

		// https://dev.twitter.com/docs/user-profile-images-and-banners

		return new Tweet(String.valueOf(s.getId()),
				s.getUser().getScreenName(),
				s.getUser().getName(),
				text,
				TimeUnit.MILLISECONDS.toSeconds(s.getCreatedAt().getTime()),
				s.getUser().getProfileImageURLHttps(),
				MetaUtils.firstMetaOfTypesData(metas, MetaType.MEDIA),
				metas);
	}

	static long minIdOf (final long statingMin, final List<Status> tweets) {
		long min = statingMin;
		for (final Status status : tweets) {
			min = Math.min(min, status.getId());
		}
		return min;
	}

	private static URLEntity[] mergeArrays (final URLEntity[]... urlss) {
		int count = 0;
		for (final URLEntity[] urls : urlss) {
			if (urls != null) count += urls.length;
		}
		final URLEntity[] ret = new URLEntity[count];
		int x = 0;
		for (final URLEntity[] urls : urlss) {
			if (urls != null) {
				System.arraycopy(urls, 0, ret, x, urls.length);
				x += urls.length;
			}
		}
		Arrays.sort(ret, URLENTITY_COMP);
		return ret;
	}

	private static String expandUrls (final String text, final URLEntity[] urls, final List<Meta> metas) {
		if (urls == null || urls.length < 1) return text;

		final StringBuilder bld = new StringBuilder();
		for (int i = 0; i < urls.length; i++) {
			final URLEntity url = urls[i];
			if (url.getStart() < 0 || url.getEnd() > text.length()) return text; // All bets are off.
			final String fullUrl = url.getExpandedURL() != null ? url.getExpandedURL() : url.getURL();
			bld.append(text.substring(i == 0 ? 0 : urls[i - 1].getEnd(), url.getStart())).append(fullUrl);
			metas.add(new Meta(MetaType.URL, fullUrl, url.getDisplayURL()));
		}
		bld.append(text.substring(urls[urls.length - 1].getEnd()));
		final String expandedText = bld.toString();
		LOG.d("Expanded '%s' --> '%s'.", text, expandedText);
		return expandedText;
	}

	private static void addMedia (final Status s, final List<Meta> metas) {
		final MediaEntity[] mes = s.getMediaEntities();
		if (mes == null) return;
		for (final MediaEntity me : mes) {
			metas.add(new Meta(MetaType.MEDIA, me.getMediaURLHttps()));
		}
	}

	private static void checkUrlsForMedia (final Status s, final List<Meta> metas) {
		final URLEntity[] urls = s.getURLEntities();
		if (urls == null) return;
		for (final URLEntity url : urls) {
			final String fullUrl = url.getExpandedURL() != null ? url.getExpandedURL() : url.getURL();
			final String thumbUrl = ImageHostHelper.thumbUrl(fullUrl);
			if (thumbUrl != null) metas.add(new Meta(MetaType.MEDIA, thumbUrl));
		}
	}

	private static void addHashtags (final Status s, final List<Meta> metas) {
		final HashtagEntity[] tags = s.getHashtagEntities();
		if (tags == null) return;
		for (final HashtagEntity tag : tags) {
			metas.add(new Meta(MetaType.HASHTAG, tag.getText()));
		}
	}

	private static void addMentions (final Status s, final List<Meta> metas, final long ownId) {
		final UserMentionEntity[] umes = s.getUserMentionEntities();
		if (umes == null) return;
		for (final UserMentionEntity ume : umes) {
			if (ume.getId() != ownId) metas.add(new Meta(MetaType.MENTION, ume.getScreenName(), ume.getName()));
		}
	}

	private static final Comparator<URLEntity> URLENTITY_COMP = new Comparator<URLEntity>() {
		@Override
		public int compare (final URLEntity lhs, final URLEntity rhs) {
			return lhs.getStart() - rhs.getStart();
		}
	};

	public static String friendlyExceptionMessage (final TwitterException e) {
		switch (e.getErrorCode()) {
			case TWITTER_ERROR_CODE_ACCOUNT_SUSPENDED:
				return "Your account is suspended and is not permitted to access this feature. :(";
			case TWITTER_ERROR_CODE_RATE_LIMIT_EXCEEDED:
				return "Rate limit exceeded.  Please try again in a while.";
			case TWITTER_ERROR_CODE_INVALID_EXPIRED_TOKEN:
				return "Invalid or expired token.  Please try reauthenticating.";
			case TWITTER_ERROR_CODE_OVER_CAPCACITY:
				return "OMG Twitter is over capacity!";
			case TWITTER_ERROR_CODE_TWITTER_IS_DOWN:
				return "OMG Twitter is down!";
			case TWITTER_ERROR_CODE_NOT_AUTH_TO_VIEW_STATUS:
				return "You are not authorized to see this status.";
			case TWITTER_ERROR_CODE_DAILY_STATUS_LIMIT_EXCEEDED:
				return "You are over daily status update limit.";
			default:
		}
		final Throwable cause = e.getCause();
		if (cause != null) {
			if (cause instanceof UnknownHostException) {
				return "Network error: " + cause.getMessage();
			}
			else if (cause instanceof IOException && StringHelper.safeContainsIgnoreCase(cause.getMessage(), "connection timed out")) {
				return "Network error: Connection timed out.";
			}
			else if (cause instanceof IOException) {
				return "Network error: " + cause;
			}
			else if (cause instanceof twitter4j.internal.org.json.JSONException) {
				return "Network error: Invalid or incomplete data received.";
			}
		}
		return ExcpetionHelper.causeTrace(e);
	}

}

package com.vaguehope.onosendai.provider.twitter;

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
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.LogWrapper;

public final class TwitterUtils {

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
			TwitterUtils.addTweetsToList(tweets, account, timelinePage);
			minId = TwitterUtils.minIdOf(minId, timelinePage);
			page++;
		}
		return new TweetList(tweets);
	}

	static void addTweetsToList (final List<Tweet> list, final Account account, final List<Status> tweets) {
		for (final Status status : tweets) {
			list.add(convertTweet(account, status));
		}
	}

	static Tweet convertTweet (final Account account, final Status s) {
		final List<Meta> metas = new ArrayList<Meta>();
		metas.add(new Meta(MetaType.ACCOUNT, account.getId()));

		final URLEntity[] urls = mergeArrays(s.getURLEntities(), s.getMediaEntities());
		final String text = expandUrls(s.getText(), urls, metas);

		if (s.getInReplyToStatusId() > 0) {
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getInReplyToStatusId())));
		}
		else if (s.isRetweet() && s.getRetweetedStatus().getId() > 0) {
			metas.add(new Meta(MetaType.INREPLYTO, String.valueOf(s.getRetweetedStatus().getId())));
		}

		addMedia(s, metas);
		addHashtags(s, metas);
		addMentions(s, metas);

		// https://dev.twitter.com/docs/user-profile-images-and-banners

		return new Tweet(String.valueOf(s.getId()),
				s.getUser().getScreenName(),
				s.getUser().getName(),
				text,
				TimeUnit.MILLISECONDS.toSeconds(s.getCreatedAt().getTime()),
				s.getUser().getProfileImageURLHttps(),
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
			count += urls.length;
		}
		final URLEntity[] ret = new URLEntity[count];
		int x = 0;
		for (final URLEntity[] urls : urlss) {
			System.arraycopy(urls, 0, ret, x, urls.length);
			x += urls.length;
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
		if (mes == null || mes.length < 1) return;
		for (int i = 0; i < mes.length; i++) {
			metas.add(new Meta(MetaType.MEDIA, mes[i].getMediaURLHttps()));
		}
	}

	private static void addHashtags (final Status s, final List<Meta> metas) {
		final HashtagEntity[] tags = s.getHashtagEntities();
		if (tags == null || tags.length < 1) return;
		for (int i = 0; i < tags.length; i++) {
			metas.add(new Meta(MetaType.HASHTAG, tags[i].getText()));
		}
	}

	private static void addMentions (final Status s, final List<Meta> metas) {
		final UserMentionEntity[] ums = s.getUserMentionEntities();
		if (ums == null || ums.length < 1) return;
		for (int i = 0; i < ums.length; i++) {
			metas.add(new Meta(MetaType.MENTION, ums[i].getScreenName()));
		}
	}

	private static final Comparator<URLEntity> URLENTITY_COMP = new Comparator<URLEntity>() {
		@Override
		public int compare (final URLEntity lhs, final URLEntity rhs) {
			return lhs.getStart() - rhs.getStart();
		}
	};

}

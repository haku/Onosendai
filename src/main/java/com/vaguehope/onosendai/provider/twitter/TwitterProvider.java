package com.vaguehope.onosendai.provider.twitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;
import twitter4j.conf.ConfigurationBuilder;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.LogWrapper;

public class TwitterProvider {

	private static final LogWrapper LOG = new LogWrapper("TP");

	private final ConcurrentMap<String, Twitter> accounts;

	public TwitterProvider () {
		this.accounts = new ConcurrentHashMap<String, Twitter>();
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.getId())) return;
		final TwitterFactory tf = makeTwitterFactory(account);
		final Twitter t = tf.getInstance();
		if (this.accounts.putIfAbsent(account.getId(), t) != null) {
			t.shutdown();
		}
	}

	private Twitter getTwitter (final Account account) {
		final Twitter t = this.accounts.get(account.getId());
		if (t != null) return t;
		addAccount(account);
		return this.accounts.get(account.getId());
	}

	public void shutdown () {
		final Iterator<Twitter> itr = this.accounts.values().iterator();
		while (itr.hasNext()) {
			final Twitter t = itr.next();
			t.shutdown();
			itr.remove();
		}
	}

	/**
	 * TODO use a call back to return tweets progressively.
	 */
	public TweetList getTweets (final TwitterFeed feed, final Account account, final long sinceId) throws TwitterException {
		return fetchTwitterFeed(account, feed, sinceId);
	}

	public Tweet getTweet (final Account account, final long id) throws TwitterException {
		return convertTweet(account, getTwitter(account).showStatus(id));
	}

	public void post (final Account account, final String body, final long inReplyTo) throws TwitterException {
		final StatusUpdate s = new StatusUpdate(body);
		if (inReplyTo > 0) s.setInReplyToStatusId(inReplyTo);
		getTwitter(account).updateStatus(s);
	}

	public void rt (final Account account, final long id) throws TwitterException {
		getTwitter(account).retweetStatus(id);
	}

	private static TwitterFactory makeTwitterFactory (final Account account) {
		final ConfigurationBuilder cb = new ConfigurationBuilder()
				.setOAuthConsumerKey(account.getConsumerKey())
				.setOAuthConsumerSecret(account.getConsumerSecret())
				.setOAuthAccessToken(account.getAccessToken())
				.setOAuthAccessTokenSecret(account.getAccessSecret());
		return new TwitterFactory(cb.build());
	}

	/*
	 * Paging: https://dev.twitter.com/docs/working-with-timelines
	 * http://twitter4j.org/en/code-examples.html
	 */

	private TweetList fetchTwitterFeed (final Account account, final TwitterFeed feed, final long sinceId) throws TwitterException {
		final Twitter t = getTwitter(account);
		final List<Tweet> tweets = new ArrayList<Tweet>();
		final int minCount = feed.recommendedFetchCount();
		final int pageSize = Math.min(minCount, C.TWEET_FETCH_PAGE_SIZE);
		int page = 1; // First page is 1.
		long minId = -1;
		while (tweets.size() < minCount) {
			final Paging paging = new Paging(page, pageSize);
			if (sinceId > 0) paging.setSinceId(sinceId);
			if (minId > 0) paging.setMaxId(minId);
			final ResponseList<Status> timelinePage = feed.getTweets(t, paging);
			LOG.i("Page %d of '%s' contains %d items.", page, feed.toString(), timelinePage.size());
			if (timelinePage.size() < 1) break;
			addTweetsToList(tweets, account, timelinePage);
			minId = minIdOf(minId, timelinePage);
			page++;
		}
		return new TweetList(tweets);
	}

	private static void addTweetsToList (final List<Tweet> list, final Account account, final ResponseList<Status> tweets) {
		for (final Status status : tweets) {
			list.add(convertTweet(account, status));
		}
	}

	private static Tweet convertTweet (final Account account, final Status s) {
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

	private static long minIdOf (final long statingMin, final ResponseList<Status> tweets) {
		long min = statingMin;
		for (final Status status : tweets) {
			min = Math.min(min, status.getId());
		}
		return min;
	}

	private static final Comparator<URLEntity> URLENTITY_COMP = new Comparator<URLEntity>() {
		@Override
		public int compare (final URLEntity lhs, final URLEntity rhs) {
			return lhs.getStart() - rhs.getStart();
		}
	};

}

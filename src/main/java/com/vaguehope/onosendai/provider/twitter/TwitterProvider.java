package com.vaguehope.onosendai.provider.twitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;
import twitter4j.conf.ConfigurationBuilder;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.MetaBuilder;
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
		if (this.accounts.containsKey(account.id)) return;
		TwitterFactory tf = makeTwitterFactory(account);
		Twitter t = tf.getInstance();
		this.accounts.putIfAbsent(account.id, t);
	}

	public TweetList getTweets (final TwitterFeed feed, final Account account, final long sinceId) throws TwitterException {
		Twitter t = this.accounts.get(account.id);
		if (t == null) throw new IllegalStateException("Account not configured: '" + account.id + "'.");
		return fetchTwitterFeed(t, feed, sinceId);
	}

	public void shutdown () {
		Iterator<Twitter> itr = this.accounts.values().iterator();
		while (itr.hasNext()) {
			Twitter t = itr.next();
			t.shutdown();
			itr.remove();
		}
	}

	private static TwitterFactory makeTwitterFactory (final Account account) {
		ConfigurationBuilder cb = new ConfigurationBuilder()
				.setOAuthConsumerKey(account.consumerKey)
				.setOAuthConsumerSecret(account.consumerSecret)
				.setOAuthAccessToken(account.accessToken)
				.setOAuthAccessTokenSecret(account.accessSecret);
		return new TwitterFactory(cb.build());
	}

	/*
	 * Paging: https://dev.twitter.com/docs/working-with-timelines
	 * http://twitter4j.org/en/code-examples.html
	 */

	private static TweetList fetchTwitterFeed (final Twitter t, final TwitterFeed feed, final long sinceId) throws TwitterException {
		List<Tweet> tweets = new ArrayList<Tweet>();
		int minCount = feed.recommendedFetchCount();
		int pageSize = Math.min(minCount, C.TWEET_FETCH_PAGE_SIZE);
		int page = 1; // First page is 1.
		long minId = -1;
		while (tweets.size() < minCount) {
			Paging paging = new Paging(page, pageSize);
			if (sinceId > 0) paging.setSinceId(sinceId);
			if (minId > 0) paging.setMaxId(minId);
			ResponseList<Status> timelinePage = feed.getTweets(t, paging);
			LOG.i("Page %d of '%s' contains %d items.", page, feed.toString(), timelinePage.size());
			if (timelinePage.size() < 1) break;
			addTweetsToList(tweets, timelinePage);
			if (timelinePage.size() < pageSize) break;
			minId = minIdOf(minId, timelinePage);
			page++;
		}
		return new TweetList(tweets);
	}

	private static void addTweetsToList (final List<Tweet> list, final ResponseList<Status> tweets) {
		for (Status status : tweets) {
			list.add(convertTweet(status));
		}
	}

	private static Tweet convertTweet (final Status s) {
		URLEntity[] urls = mergeArrays(s.getURLEntities(), s.getMediaEntities());
		String text = expandUrls(s.getText(), urls);

		MetaBuilder metaBuilder = new MetaBuilder();
		addMedia(s, metaBuilder);
		addHashtags(s, metaBuilder);
		addMentions(s, metaBuilder);

		// https://dev.twitter.com/docs/user-profile-images-and-banners

		return new Tweet(s.getId(), s.getUser().getScreenName(), text,
				s.getCreatedAt().getTime() / 1000L,
				s.getUser().getProfileImageURLHttps(),
				metaBuilder);
	}

	private static URLEntity[] mergeArrays (final URLEntity[]... urlss) {
		int count = 0;
		for (URLEntity[] urls : urlss) {
			count += urls.length;
		}
		URLEntity[] ret = new URLEntity[count];
		int x = 0;
		for (URLEntity[] urls : urlss) {
			System.arraycopy(urls, 0, ret, x, urls.length);
			x += urls.length;
		}
		Arrays.sort(ret, URLENTITY_COMP);
		return ret;
	}

	private static String expandUrls (final String text, final URLEntity[] urls) {
		if (urls == null || urls.length < 1) return text;

		final StringBuilder bld = new StringBuilder();
		for (int i = 0; i < urls.length; i++) {
			final URLEntity url = urls[i];
			if (url.getStart() < 0 || url.getEnd() > text.length()) return text; // All bets are off.
			bld.append(text.substring(i == 0 ? 0 : urls[i - 1].getEnd(), url.getStart()))
					.append(url.getExpandedURL() != null ? url.getExpandedURL() : url.getURL());
		}
		bld.append(text.substring(urls[urls.length - 1].getEnd()));
		String expandedText = bld.toString();
		LOG.d("Expanded '%s' --> '%s'.", text, expandedText);
		return expandedText;
	}

	private static void addMedia (final Status s, final MetaBuilder metaBuilder) {
		MediaEntity[] mes = s.getMediaEntities();
		if (mes == null || mes.length < 1) return;
		for (int i = 0; i < mes.length; i++) {
			metaBuilder.add(MetaType.MEDIA, mes[i].getMediaURLHttps());
		}
	}

	private static void addHashtags (final Status s, final MetaBuilder metaBuilder) {
		HashtagEntity[] tags = s.getHashtagEntities();
		if (tags == null || tags.length < 1) return;
		for (int i = 0; i < tags.length; i++) {
			metaBuilder.add(MetaType.HASHTAG, tags[i].getText());
		}
	}

	private static void addMentions (final Status s, final MetaBuilder metaBuilder) {
		UserMentionEntity[] ums = s.getUserMentionEntities();
		if (ums == null || ums.length < 1) return;
		for (int i = 0; i < ums.length; i++) {
			metaBuilder.add(MetaType.MENTION, ums[i].getScreenName());
		}
	}

	private static long minIdOf (final long statingMin, final ResponseList<Status> tweets) {
		long min = statingMin;
		for (Status status : tweets) {
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

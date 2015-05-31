package com.vaguehope.onosendai.provider.twitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.LogWrapper;

class SearchFeed implements TwitterFeed {

	private static final LogWrapper LOG = new LogWrapper("TS");

	private final String term;

	public SearchFeed (final String term) {
		this.term = term;
	}

	@Override
	public TweetList getTweets (final Account account, final Twitter t, final long sinceId, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		final List<Tweet> tweets = new ArrayList<Tweet>();
		final int page = 1; // First page is 1.
		Query query = new Query()
				.query(this.term)
				.count(C.TWEET_FETCH_PAGE_SIZE)
				.resultType(Query.RECENT);
		if (sinceId > 0) query.setSinceId(sinceId);
		QueryResult result;
		do {
			result = t.search(query);
			final List<Status> resTweets = result.getTweets();
			LOG.i("Page %d of query '%s' contains %d items.", page, this.term, resTweets.size());
			TwitterUtils.addTweetsToList(tweets, account, resTweets, t.getId(), hdMedia, extraMetas);
		}
		while (tweets.size() < C.TWITTER_SEARCH_MAX_FETCH && (query = result.nextQuery()) != null); // NOSONAR I am ok with this inner assignment.
		return new TweetList(tweets);
	}

	@Override
	public String toString () {
		return "search{" + this.term + "}";
	}

}

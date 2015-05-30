package com.vaguehope.onosendai.provider.twitter;

import java.util.Collection;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.StringHelper;

class ListFeed implements FeedGetter {

	private final String ownerScreenName;
	private final String slug;

	/**
	 * "mylist" or "user/theirlist"
	 */
	public ListFeed (final String ownerScreenNameAndSlug) {
		if (StringHelper.isEmpty(ownerScreenNameAndSlug)) throw new IllegalArgumentException("ownerScreenNameAndSlug must not be empty.");
		final int x = ownerScreenNameAndSlug.indexOf('/');
		if (x > 0) {
			this.ownerScreenName = ownerScreenNameAndSlug.substring(0, x);
			this.slug = ownerScreenNameAndSlug.substring(x + 1);
		}
		else if (x < 0) {
			this.ownerScreenName = null;
			this.slug = ownerScreenNameAndSlug;
		}
		else {
			throw new IllegalArgumentException("ownerScreenNameAndSlug can not start with /: " + ownerScreenNameAndSlug);
		}
	}

	public String getOwnerScreenName () {
		return this.ownerScreenName;
	}

	public String getSlug () {
		return this.slug;
	}

	@Override
	public ResponseList<Status> getTweets (final Twitter t, final Paging paging) throws TwitterException {
		if (StringHelper.isEmpty(this.ownerScreenName)) {
			return t.getUserListStatuses(t.getId(), this.slug, paging);
		}
		return t.getUserListStatuses(this.ownerScreenName, this.slug, paging);
	}

	@Override
	public int recommendedFetchCount () {
		return C.TWITTER_LIST_MAX_FETCH;
	}

	@Override
	public TweetList getTweets (final Account account, final Twitter t, final long sinceId, final boolean hdMedia, final Collection<Meta> extraMetas) throws TwitterException {
		return TwitterUtils.fetchTwitterFeed(account, t, this, sinceId, hdMedia, extraMetas);
	}

	@Override
	public String toString () {
		return "list{" + this.slug + "}";
	}

}
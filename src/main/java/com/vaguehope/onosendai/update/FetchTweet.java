package com.vaguehope.onosendai.update;

import java.util.Collections;
import java.util.concurrent.Callable;

import twitter4j.HttpResponseCode;
import twitter4j.TwitterException;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.DiscardOrder;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class FetchTweet implements Callable<Void> {

	private static final LogWrapper LOG = new LogWrapper("FT");

	private final DbInterface db;
	private final ProviderMgr provMgr;
	private final String linkedTweetSid;
	private final Account account;
	private final Column column;
	private final Meta meta;

	public FetchTweet (final DbInterface db, final ProviderMgr provMgr, final Account account, final Column column, final String linkedTweetSid, final Meta meta) {
		if (db == null) throw new IllegalArgumentException("db can not be null.");
		if (provMgr == null) throw new IllegalArgumentException("provMgr can not be null.");
		if (account == null) throw new IllegalArgumentException("account can not be null.");
		if (column == null) throw new IllegalArgumentException("column can not be null.");
		if (linkedTweetSid == null) throw new IllegalArgumentException("linkedTweetSid can not be null.");
		if (meta == null) throw new IllegalArgumentException("meta can not be null.");
		this.db = db;
		this.provMgr = provMgr;
		this.account = account;
		this.column = column;
		this.linkedTweetSid = linkedTweetSid;
		this.meta = meta;
	}

	@Override
	public Void call () throws Exception {
		try {
			fetchLinkedTweet(this.db, this.provMgr, this.account, this.column, this.linkedTweetSid);
		}
		catch (final TwitterException e) {
			LOG.w("Failed to retrieve tweet %s: %s", this.linkedTweetSid, ExcpetionHelper.causeTrace(e, " > "));
			if (e.getStatusCode() == HttpResponseCode.NOT_FOUND) {
				FetchLinkTitle.setTitle(this.db, this.meta, String.format("Tweet not found: %s", this.linkedTweetSid));
			}
			else if (e.getStatusCode() == HttpResponseCode.FORBIDDEN) {
				FetchLinkTitle.setTitle(this.db, this.meta, String.format("Tweet access forbidden: %s", this.linkedTweetSid));
			}
		}
		catch (final Exception e) { // NOSONAR report all errors.
			LOG.w("Failed to retrieve tweet %s: %s", this.linkedTweetSid, ExcpetionHelper.causeTrace(e, " > "));
		}
		return null;
	}

	private static void fetchLinkedTweet (final DbInterface db, final ProviderMgr provMgr, final Account account, final Column column, final String linkedTweetSid) throws TwitterException {
		switch (account.getProvider()) {
			case TWITTER:
				fetchFromTwitter(db, provMgr, account, column, linkedTweetSid);
			default:
		}
	}

	private static void fetchFromTwitter (final DbInterface db, final ProviderMgr provMgr, final Account account, final Column column, final String linkedTweetSid) throws TwitterException {
		final Tweet fromCache = db.getTweetDetails(linkedTweetSid);
		if (fromCache != null) {
			LOG.d("From cache: %s=%s", linkedTweetSid, fromCache);
			return;
		}

		final Tweet fetched = provMgr.getTwitterProvider().getTweet(account, Long.parseLong(linkedTweetSid), column.isHdMedia());
		LOG.i("Fetched: %s=%s", linkedTweetSid, fetched);
		if (fetched != null) {
			db.storeTweets(Column.ID_CACHED, Collections.singletonList(fetched), DiscardOrder.FIRST_DOWNLOADED);
		}

	}

}

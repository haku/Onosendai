package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twitter4j.TwitterException;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.payload.InReplyToLoaderTask.ReplyLoaderResult;
import com.vaguehope.onosendai.provider.NetworkType;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class InReplyToLoaderTask extends AsyncTask<Tweet, Void, ReplyLoaderResult> {

	private static final LogWrapper LOG = new LogWrapper("RL");

	private final Config conf;
	private final ProviderMgr provMgr;
	private final DbInterface db;
	private final PayloadListAdapter payloadListAdapter;
	private final Payload placeholderPayload;

	public InReplyToLoaderTask (final Config conf, final ProviderMgr provMgr, final DbInterface db, final PayloadListAdapter payloadListAdapter) {
		super();
		this.conf = conf;
		this.provMgr = provMgr;
		this.db = db;
		this.payloadListAdapter = payloadListAdapter;
		this.placeholderPayload = new PlaceholderPayload(null, "Fetching conversation...");
	}

	@Override
	protected void onPreExecute () {
		this.payloadListAdapter.addItem(this.placeholderPayload);
	}

	@Override
	protected ReplyLoaderResult doInBackground (final Tweet... params) {
		if (params.length != 1) throw new IllegalArgumentException("Only one param per task.");
		final Tweet startingTweet = params[0];

		final Account account = MetaUtils.accountFromMeta(startingTweet, this.conf);
		if (account != null) {
			switch (account.getProvider()) {
				case TWITTER:
					return twitter(account, startingTweet);
				case SUCCESSWHALE:
					return successWhale(account, startingTweet);
				default:
			}
		}
		return generic(startingTweet);
	}

	private ReplyLoaderResult twitter (final Account account, final Tweet startingTweet) {
		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return null;

		final ReplyLoaderResult fromCache = fetchFromCache(startingTweet, inReplyToMeta.getData());
		if (fromCache != null) return fromCache;

		try {
			final Tweet inReplyToTweet = this.provMgr.getTwitterProvider().getTweet(account, Long.parseLong(inReplyToMeta.getData()));
			if (inReplyToTweet != null) return new ReplyLoaderResult(new InReplyToPayload(startingTweet, inReplyToTweet), true);
		}
		catch (TwitterException e) {
			LOG.w("Failed to retrieve tweet %s: %s", inReplyToMeta.getData(), e.toString());
			return new ReplyLoaderResult("Error fetching tweet: " + e.getMessage(), startingTweet);
		}

		return null;
	}

	private ReplyLoaderResult successWhale (final Account account, final Tweet startingTweet) {
		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return fetchComments(account, startingTweet);

		final ReplyLoaderResult fromCache = fetchFromCache(startingTweet, inReplyToMeta.getData());
		if (fromCache != null) return fromCache;

		final Meta serviceMeta = startingTweet.getFirstMetaOfType(MetaType.SERVICE);
		if (serviceMeta != null) {
			try {
				final TweetList thread = this.provMgr.getSuccessWhaleProvider().getThread(account, serviceMeta.getData(), startingTweet.getSid());
				if (thread != null && thread.count() > 0) return new ReplyLoaderResult(tweetListToReplyPayloads(startingTweet, thread), false);
			}
			catch (SuccessWhaleException e) {
				LOG.w("Failed to retrieve thread %s: %s", inReplyToMeta.getData(), e.toString());
				return new ReplyLoaderResult("Error fetching thread: " + e.getMessage(), startingTweet);
			}
		}

		return null;
	}

	private ReplyLoaderResult generic (final Tweet startingTweet) {
		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return null;

		final ReplyLoaderResult fromCache = fetchFromCache(startingTweet, inReplyToMeta.getData());
		if (fromCache != null) return fromCache;

		return null;
	}

	private ReplyLoaderResult fetchFromCache (final Tweet startingTweet, final String inReplyToSid) {
		final Tweet inReplyToTweet = this.db.getTweetDetails(inReplyToSid);
		if (inReplyToTweet != null) return new ReplyLoaderResult(new InReplyToPayload(startingTweet, inReplyToTweet), true);
		return null;
	}

	private ReplyLoaderResult fetchComments (final Account account, final Tweet startingTweet) {
		// Hack because FB items are not immutable and must always be checked for comments.
		final Meta serviceMeta = startingTweet.getFirstMetaOfType(MetaType.SERVICE);
		if (serviceMeta != null && ServiceRef.parseServiceMeta(serviceMeta).getType() == NetworkType.FACEBOOK) {
			try {
				final TweetList thread = this.provMgr.getSuccessWhaleProvider().getThread(account, serviceMeta.getData(), startingTweet.getSid());
				if (thread != null && thread.count() > 0) return new ReplyLoaderResult(tweetListToReplyPayloads(startingTweet, thread), false);
				return new ReplyLoaderResult("No comments.", startingTweet);
			}
			catch (SuccessWhaleException e) {
				LOG.w("Failed to retrieve thread %s: %s", startingTweet.getSid(), e.toString());
				return new ReplyLoaderResult("Error fetching comments: " + e.getMessage(), startingTweet);
			}
		}
		return null;
	}

	private static List<InReplyToPayload> tweetListToReplyPayloads (final Tweet startingTweet, final TweetList thread) {
		List<InReplyToPayload> ret = new ArrayList<InReplyToPayload>();
		for (Tweet tweet : thread.getTweets()) {
			ret.add(new InReplyToPayload(startingTweet, tweet));
		}
		return ret;
	}

	@Override
	protected void onPostExecute (final ReplyLoaderResult result) {
		if (result == null || !result.hasResults()) {
			this.payloadListAdapter.removeItem(this.placeholderPayload);
			return;
		}
		this.payloadListAdapter.replaceItem(this.placeholderPayload, result.getPayloads());
		if (result.checkAgain()) {
			new InReplyToLoaderTask(this.conf, this.provMgr, this.db, this.payloadListAdapter).execute(result.getFirstTweet());
		}
	}

	protected static class ReplyLoaderResult {

		private final List<InReplyToPayload> payloads;
		private final boolean checkAgain;
		private final Payload fallback;

		public ReplyLoaderResult (final String msg, final Tweet ownerTweet) {
			this.fallback = new PlaceholderPayload(ownerTweet, msg);
			this.payloads = null;
			this.checkAgain = false;
		}

		public ReplyLoaderResult (final InReplyToPayload inReplyToPayload, final boolean checkAgain) {
			this(Collections.singletonList(inReplyToPayload), checkAgain);
		}

		public ReplyLoaderResult (final List<InReplyToPayload> inReplyToPayloads, final boolean checkAgain) {
			this.payloads = inReplyToPayloads;
			this.checkAgain = checkAgain;
			this.fallback = null;
		}

		public boolean hasResults () {
			return (this.payloads != null && this.payloads.size() > 0) || this.fallback != null;
		}

		public List<? extends Payload> getPayloads () {
			if (this.payloads != null) return this.payloads;
			if (this.fallback != null) return Collections.singletonList(this.fallback);
			return null;
		}

		public Tweet getFirstTweet () {
			if (this.payloads == null || this.payloads.size() < 1) return null;
			final InReplyToPayload payload = this.payloads.get(0);
			if (payload == null) return null;
			return payload.getInReplyToTweet();
		}

		public boolean checkAgain () {
			return this.checkAgain;
		}

	}

}

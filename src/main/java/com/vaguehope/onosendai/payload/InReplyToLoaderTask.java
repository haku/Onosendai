package com.vaguehope.onosendai.payload;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import android.content.Context;

import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.MetaUtils;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.payload.InReplyToLoaderTask.ReplyLoaderResult;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.DiscardOrder;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;

public class InReplyToLoaderTask extends DbBindingAsyncTask<Void, Void, ReplyLoaderResult> {

	private static final LogWrapper LOG = new LogWrapper("RL");

	// How many times it is allowed to call itself, to avoid going forever of long threads.
	private static final int MAX_INVOCATION_COUNT = 10;

	private final Config conf;
	private final ProviderMgr provMgr;
	private final PayloadListAdapter payloadListAdapter;
	private final Tweet rootTweet;
	private final Tweet threadTweet;
	private final boolean hdMedia;
	private final Executor es;
	private final Payload placeholderPayload;
	private final int prevInvocationCount;

	public InReplyToLoaderTask (final ExecutorEventListener eventListener, final Context context, final Config conf, final ProviderMgr provMgr,
			final Tweet rootTweet, final boolean hdMedia, final PayloadListAdapter payloadListAdapter, final Executor es) {
		this(eventListener, context, conf, provMgr, rootTweet, rootTweet, hdMedia, payloadListAdapter, es, 0);
	}

	private InReplyToLoaderTask (final ExecutorEventListener eventListener, final Context context, final Config conf, final ProviderMgr provMgr,
			final Tweet rootTweet, final Tweet threadTweet, final boolean hdMedia, final PayloadListAdapter payloadListAdapter, final Executor es, final int prevInvocationCount) {
		super(eventListener, context);
		this.conf = conf;
		this.provMgr = provMgr;
		this.rootTweet = rootTweet;
		this.threadTweet = threadTweet;
		this.hdMedia = hdMedia;
		this.payloadListAdapter = payloadListAdapter;
		this.es = es;
		this.placeholderPayload = new PlaceholderPayload(null, "Fetching conversation...", true); //ES
		this.prevInvocationCount = prevInvocationCount;
	}

	@Override
	public String toString () {
		return "inReplyToLoader:" + this.threadTweet.getSid();
	}

	@Override
	protected LogWrapper getLog () {
		return LOG;
	}

	@Override
	protected void onPreExecute () {
		this.payloadListAdapter.addItem(this.placeholderPayload);
	}

	@Override
	protected ReplyLoaderResult doInBackgroundWithDb (final DbInterface db, final Void... unused) {
		final Account account = MetaUtils.accountFromMeta(this.threadTweet, this.conf);
		if (account != null) {
			switch (account.getProvider()) {
				case MASTODON:
					return mastodon(db, account, this.threadTweet);
				default:
			}
		}
		return generic(db, this.threadTweet);
	}

	private ReplyLoaderResult mastodon (final DbInterface db, final Account account, final Tweet startingTweet) {
		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return null;

		final ReplyLoaderResult fromCache = fetchFromCache(db, startingTweet, inReplyToMeta.getData());
		if (fromCache != null) return fromCache;

		try {
			final Tweet inReplyToTweet = this.provMgr.getMastodonProvider().getToot(account, inReplyToMeta.toLong(0L));
			if (inReplyToTweet != null) {
				cacheInReplyTos(db, Collections.singletonList(inReplyToTweet));
				return new ReplyLoaderResult(new InReplyToPayload(startingTweet, inReplyToTweet), true);
			}
		}
		catch (final Mastodon4jRequestException e) {
			LOG.w("Failed to retrieve toot %s: %s", inReplyToMeta.getData(), e.toString());
			return new ReplyLoaderResult("Error fetching toot: " + e.getMessage(), startingTweet);
		}

		return null;
	}

	private static ReplyLoaderResult generic (final DbInterface db, final Tweet startingTweet) {
		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return null;

		final ReplyLoaderResult fromCache = fetchFromCache(db, startingTweet, inReplyToMeta.getData());
		if (fromCache != null) return fromCache;

		return null;
	}

	private static ReplyLoaderResult fetchFromCache (final DbInterface db, final Tweet startingTweet, final String inReplyToSid) {
		final Tweet inReplyToTweet = db.getTweetDetails(inReplyToSid);
		if (inReplyToTweet != null) return new ReplyLoaderResult(new InReplyToPayload(startingTweet, inReplyToTweet), true);
		return null;
	}

	private static void cacheInReplyTos (final DbInterface db, final List<Tweet> tweets) {
		db.storeTweets(Column.ID_CACHED, tweets, DiscardOrder.FIRST_DOWNLOADED);
	}

	@Override
	protected void onPostExecute (final ReplyLoaderResult result) {
		if (!this.payloadListAdapter.isForTweet(this.rootTweet)) return;
		if (result == null || !result.hasResults()) {
			this.payloadListAdapter.removeItem(this.placeholderPayload);
			return;
		}
		this.payloadListAdapter.replaceItem(this.placeholderPayload, result.getPayloads());
		if (result.checkAgain() && this.prevInvocationCount < MAX_INVOCATION_COUNT) {
			new InReplyToLoaderTask(getEventListener(), getContext(), this.conf, this.provMgr, this.rootTweet, result.getFirstTweet(),
					this.hdMedia, this.payloadListAdapter, this.es, this.prevInvocationCount + 1)
			.executeOnExecutor(this.es);
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

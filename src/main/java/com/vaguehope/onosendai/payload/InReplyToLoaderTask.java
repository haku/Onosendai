package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twitter4j.TwitterException;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class InReplyToLoaderTask extends AsyncTask<Tweet, Void, List<InReplyToPayload>> {

	private static final LogWrapper LOG = new LogWrapper("RL");

	private final Account account;
	private final Column column;
	private final ProviderMgr provMgr;
	private final DbInterface db;
	private final PayloadListAdapter payloadListAdapter;
	private final Payload placeholderPayload;

	public InReplyToLoaderTask (final Account account, final Column column, final ProviderMgr provMgr, final DbInterface db, final PayloadListAdapter payloadListAdapter) {
		this.account = account;
		this.column = column;
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
	protected List<InReplyToPayload> doInBackground (final Tweet... params) {
		if (params.length != 1) throw new IllegalArgumentException("Only one param per task.");
		final Tweet startingTweet = params[0];

		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return null;
		final String inReplyToSid = inReplyToMeta.getData();

		Tweet inReplyToTweet = this.db.getTweetDetails(this.column.getId(), inReplyToSid);
		if (inReplyToTweet != null) return Collections.singletonList(new InReplyToPayload(startingTweet, inReplyToTweet));

		inReplyToTweet = this.db.getTweetDetails(inReplyToSid);
		if (inReplyToTweet != null) return Collections.singletonList(new InReplyToPayload(startingTweet, inReplyToTweet));

		if (this.account != null) {
			try {
				// TODO cache the tweets we specifically fetch?
				switch (this.account.getProvider()) {
					case TWITTER:
						inReplyToTweet = this.provMgr.getTwitterProvider().getTweet(this.account, Long.parseLong(inReplyToSid));
						if (inReplyToTweet != null) return Collections.singletonList(new InReplyToPayload(startingTweet, inReplyToTweet));
						break;
					case SUCCESSWHALE:
						final Meta serviceMeta = startingTweet.getFirstMetaOfType(MetaType.SERVICE);
						if (serviceMeta != null) {
							final TweetList thread = this.provMgr.getSuccessWhaleProvider().getThread(this.account, serviceMeta.getData(), inReplyToSid);
							if (thread != null && thread.count() > 0) return tweetListToReplyPayloads(startingTweet, thread);
						}
						break;
					default:
				}
			}
			catch (TwitterException e) {
				LOG.w("Failed to retrieve tweet %s: %s", inReplyToSid, e.toString());
				return null;
			}
			catch (SuccessWhaleException e) {
				LOG.w("Failed to retrieve thrad %s: %s", inReplyToSid, e.toString());
				return null;
			}
		}

		return null;
	}

	@Override
	protected void onPostExecute (final List<InReplyToPayload> inReplyToPayloads) {
		if (inReplyToPayloads == null || inReplyToPayloads.size() < 1) {
			this.payloadListAdapter.removeItem(this.placeholderPayload);
			return;
		}
		else if (inReplyToPayloads.size() == 1) {
			final InReplyToPayload inReplyToPayload = inReplyToPayloads.get(0);
			this.payloadListAdapter.replaceItem(this.placeholderPayload, inReplyToPayload);
			if (this.account.getProvider() == AccountProvider.TWITTER) {
				new InReplyToLoaderTask(this.account, this.column, this.provMgr, this.db, this.payloadListAdapter).execute(inReplyToPayload.getInReplyToTweet());
			}
		}
		else {
			this.payloadListAdapter.replaceItem(this.placeholderPayload, inReplyToPayloads);
		}
	}

	private static List<InReplyToPayload> tweetListToReplyPayloads (final Tweet startingTweet, final TweetList thread) {
		List<InReplyToPayload> ret = new ArrayList<InReplyToPayload>();
		for (Tweet tweet : thread.getTweets()) {
			ret.add(new InReplyToPayload(startingTweet, tweet));
		}
		return ret;
	}

}

package com.vaguehope.onosendai.payload;

import twitter4j.TwitterException;
import android.os.AsyncTask;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class InReplyToLoaderTask extends AsyncTask<Tweet, Void, InReplyToPayload> {

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
	protected InReplyToPayload doInBackground (final Tweet... params) {
		if (params.length != 1) throw new IllegalArgumentException("Only one param per task.");
		final Tweet startingTweet = params[0];

		final Meta inReplyToMeta = startingTweet.getFirstMetaOfType(MetaType.INREPLYTO);
		if (inReplyToMeta == null) return null;
		final String inReplyToSid = inReplyToMeta.getData();

		Tweet inReplyToTweet = this.db.getTweetDetails(this.column.getId(), inReplyToSid);
		if (inReplyToTweet != null) return new InReplyToPayload(startingTweet, inReplyToTweet);

		inReplyToTweet = this.db.getTweetDetails(inReplyToSid);
		if (inReplyToTweet != null) return new InReplyToPayload(startingTweet, inReplyToTweet);

		if (this.account != null) {
			try {
				switch (this.account.getProvider()) {
					case TWITTER:
						inReplyToTweet = this.provMgr.getTwitterProvider().getTweet(this.account, Long.parseLong(inReplyToSid));
						// TODO cache the tweet we just specifically fetched?
						if (inReplyToTweet != null) return new InReplyToPayload(startingTweet, inReplyToTweet);
					default:
						// TODO fetch via other provider types?
				}
			}
			catch (TwitterException e) {
				LOG.w("Failed to retrieve tweet %s: %s", inReplyToSid, e.toString());
				return null;
			}
		}

		return null;
	}

	@Override
	protected void onPostExecute (final InReplyToPayload inReplyToPayload) {
		if (inReplyToPayload == null) {
			this.payloadListAdapter.removeItem(this.placeholderPayload);
			return;
		}
		this.payloadListAdapter.replaceItem(this.placeholderPayload, inReplyToPayload);
		new InReplyToLoaderTask(this.account, this.column, this.provMgr, this.db, this.payloadListAdapter).execute(inReplyToPayload.getInReplyToTweet());
	}

}

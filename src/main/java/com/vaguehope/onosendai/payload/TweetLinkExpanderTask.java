package com.vaguehope.onosendai.payload;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.TwitterException;
import android.content.Context;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.TaskUtils;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;

public class TweetLinkExpanderTask extends DbBindingAsyncTask<Void, Object, Void> {

	private static final Pattern TWEET_URL = Pattern.compile("^https?://twitter.com/([^/]+)/status/([^/]+)$");
	private static final LogWrapper LOG = new LogWrapper("LE");

	public static void checkAndRun (final ExecutorEventListener eventListener, final Context context, final ProviderMgr provMgr, final Tweet tweet, final boolean hdMedia, final Account account, final PayloadListAdapter payloadListAdapter, final Executor es) {
		for (final Meta meta : tweet.getMetas()) {
			if (meta.getType() != MetaType.URL) continue;
			if (TWEET_URL.matcher(meta.getData()).matches()) {
				new TweetLinkExpanderTask(eventListener, context, provMgr, tweet, hdMedia, account, payloadListAdapter).executeOnExecutor(es);
				return;
			}
		}
	}

	private final ProviderMgr provMgr;
	private final Tweet tweet;
	private final boolean hdMedia;
	private final Account account;
	private final PayloadListAdapter payloadListAdapter;

	public TweetLinkExpanderTask (final ExecutorEventListener eventListener, final Context context, final ProviderMgr provMgr, final Tweet tweet, final boolean hdMedia, final Account account, final PayloadListAdapter payloadListAdapter) {
		super(eventListener, context);
		this.provMgr = provMgr;
		this.tweet = tweet;
		this.hdMedia = hdMedia;
		this.account = account;
		this.payloadListAdapter = payloadListAdapter;
	}

	@Override
	public String toString () {
		return "tweetLinkExpander:" + this.tweet.getSid();
	}

	@Override
	protected LogWrapper getLog () {
		return LOG;
	}

	@Override
	protected Void doInBackgroundWithDb (final DbInterface db, final Void... params) {
		for (final Meta meta : this.tweet.getMetas()) {
			if (meta.getType() != MetaType.URL) continue;
			final Matcher m = TWEET_URL.matcher(meta.getData());
			if (m.matches()) fetchLinkedTweet(db, meta, m.group(2));
		}
		return null;
	}

	private final Map<String, Payload> placeHolders = new HashMap<String, Payload>();

	@Override
	protected void onProgressUpdate (final Object... values) {
		if (values == null || values.length < 1) return;
		if (!this.payloadListAdapter.isForTweet(this.tweet)) return;

		switch ((Integer) values[0]) {
			case 0: // initial(meta, linkedTweetSid).
				displayPlaceholder((Meta) values[1], (String) values[2]);
				break;
			case 1: // successResult(linkedTweetSid, linkedTweet).
				displayTweet((String) values[1], (Tweet) values[2]);
				break;
			case 2: // failResult(linkedTweetSid, exception).
				displayError((String) values[1], (Exception) values[2]);
				break;
			default:
		}
	}

	private void displayPlaceholder (final Meta meta, final String linkedTweetSid) {
		final Payload placeHolder = new PlaceholderPayload(this.tweet, String.format("Fetching %s...", linkedTweetSid), true);
		this.placeHolders.put(linkedTweetSid, placeHolder);

		final Payload linkPayload = this.payloadListAdapter.findForMeta(meta);
		if (linkPayload != null) {
			this.payloadListAdapter.addItemAfter(placeHolder, linkPayload);
		}
		else {
			this.payloadListAdapter.addItem(placeHolder);
		}
	}

	private void displayTweet (final String linkedTweetSid, final Tweet linkedTweet) {
		final Payload placeHolder = this.placeHolders.get(linkedTweetSid);
		if (placeHolder == null) throw new IllegalStateException("No cached placeholder for " + linkedTweetSid);

		if (linkedTweet != null) {
			this.payloadListAdapter.replaceItem(placeHolder, new InReplyToPayload(this.tweet, linkedTweet));
		}
		else {
			this.payloadListAdapter.replaceItem(placeHolder, new PlaceholderPayload(this.tweet, "Error: null tweet."));
		}
	}

	private void displayError (final String linkedTweetSid, final Exception exception) {
		final Payload placeHolder = this.placeHolders.get(linkedTweetSid);
		if (placeHolder == null) throw new IllegalStateException("No cached placeholder for " + linkedTweetSid);

		final String msg = exception != null ? TaskUtils.getEmsg(exception) : "Error: null exception.";
		this.payloadListAdapter.replaceItem(placeHolder, new PlaceholderPayload(this.tweet, msg));
	}

	private void fetchLinkedTweet (final DbInterface db, final Meta meta, final String linkedTweetSid) {
		switch (this.account.getProvider()) {
			case TWITTER:
				fetchFromTwitter(db, meta, linkedTweetSid);
			default:
		}
	}

	private void fetchFromTwitter (final DbInterface db, final Meta meta, final String linkedTweetSid) {
		publishProgress(0, meta, linkedTweetSid);

		final Tweet fromCache = db.getTweetDetails(linkedTweetSid);
		if (fromCache != null) {
			LOG.i("From cache: %s=%s", linkedTweetSid, fromCache);
			publishProgress(1, linkedTweetSid, fromCache);
			return;
		}

		try {
			final Tweet linkedTweet = this.provMgr.getTwitterProvider().getTweet(this.account, Long.parseLong(linkedTweetSid), this.hdMedia);
			LOG.i("Fetched: %s=%s", linkedTweetSid, linkedTweet);
			if (linkedTweet != null) {
				db.storeTweets(Column.ID_CACHED, Collections.singletonList(linkedTweet));
			}
			publishProgress(1, linkedTweetSid, linkedTweet);
		}
		catch (final TwitterException e) {
			LOG.w("Failed to retrieve tweet %s: %s", linkedTweetSid, e.toString());
			publishProgress(2, linkedTweetSid, e);
		}
	}
}

package com.vaguehope.onosendai.payload;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Spanned;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.TaskUtils;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.HtmlTitleParser;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.HttpHelper.FinalUrlHandler;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;

public class TweetLinkExpanderTask extends DbBindingAsyncTask<Void, Object, Void> {

	private static final Pattern TWEET_URL = Pattern.compile("^https?://(?:mobile\\.)?twitter.com/([^/]+)/status/([0-9]+)[^/]*$");
	private static final Pattern UNTITLEABLE_URL = Pattern.compile("^.*\\.(mp[0-9]|m3u8|mov|webm|jpe?g|png|gif|pdf)$", Pattern.CASE_INSENSITIVE);
	private static final LogWrapper LOG = new LogWrapper("LE");

	public static void checkAndRun (final ExecutorEventListener eventListener, final Context context, final ProviderMgr provMgr, final Tweet tweet, final boolean hdMedia, final Account account, final PayloadListAdapter payloadListAdapter, final Executor es) {
		for (final Meta meta : tweet.getMetas()) {
			if (meta.getType() != MetaType.URL) continue;
			new TweetLinkExpanderTask(eventListener, context, provMgr, tweet, hdMedia, account, payloadListAdapter).executeOnExecutor(es);
			return;
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
			if (m.matches()) {
				fetchLinkedTweet(db, meta, m.group(2));
				continue;
			}
			if (UNTITLEABLE_URL.matcher(meta.getData()).matches()) continue;
			fetchLinkTitle(meta);
		}
		return null;
	}

	private enum Prg {
		INIT,
		URL_AND_TITLE,
		TWEET,
		MSG,
		FAIL;
	}
	private final Map<Meta, Payload> metaToPayload = new HashMap<Meta, Payload>();

	@Override
	protected void onProgressUpdate (final Object... values) {
		if (values == null || values.length < 1) return;
		if (!this.payloadListAdapter.isForTweet(this.tweet)) return;

		switch ((Prg) values[0]) {
			case INIT: // (meta, msg).
				displayPlaceholder((Meta) values[1], (String) values[2]);
				break;
			case URL_AND_TITLE: // (meta, url, title).
				displayLinkTitle((Meta) values[1], (URL) values[2], (CharSequence) values[3]);
				break;
			case TWEET: // (meta, tweet).
				displayTweet((Meta) values[1], (Tweet) values[2]);
				break;
			case MSG: // (meta, msg).
				displayError((Meta) values[1], (String) values[2]);
				break;
			case FAIL: // (meta, exception).
				displayError((Meta) values[1], (Exception) values[2]);
				break;
			default:
		}
	}

	private void displayPlaceholder (final Meta meta, final String msg) {
		final Payload placeHolder = new PlaceholderPayload(this.tweet, msg, true);
		this.metaToPayload.put(meta, placeHolder);

		final Payload linkPayload = this.payloadListAdapter.findForMeta(meta);
		if (linkPayload != null) {
			this.payloadListAdapter.addItemAfter(placeHolder, linkPayload);
		}
		else {
			this.payloadListAdapter.addItem(placeHolder);
		}
	}

	private void displayLinkTitle (final Meta meta, final URL finalUrl, final CharSequence title) {
		final Payload placeHolder = this.metaToPayload.get(meta);
		if (placeHolder == null) throw new IllegalStateException("No cached placeholder for " + meta);

		final Payload linkPayload = this.payloadListAdapter.findForMeta(meta);
		if (linkPayload != null) {
			final String url = finalUrl != null ? finalUrl.toExternalForm() : meta.getData();
			this.payloadListAdapter.replaceItem(linkPayload, new LinkPayload(this.tweet, meta, url, title));
			this.payloadListAdapter.removeItem(placeHolder);
		}
		else {
			this.payloadListAdapter.replaceItem(placeHolder, new PlaceholderPayload(this.tweet, title));
		}
	}

	private void displayTweet (final Meta meta, final Tweet linkedTweet) {
		final Payload placeHolder = this.metaToPayload.get(meta);
		if (placeHolder == null) throw new IllegalStateException("No cached placeholder for " + meta);

		if (linkedTweet != null) {
			this.payloadListAdapter.replaceItem(placeHolder, new InReplyToPayload(this.tweet, linkedTweet));
		}
		else {
			this.payloadListAdapter.replaceItem(placeHolder, new PlaceholderPayload(this.tweet, "Error: null tweet."));
		}
	}

	private void displayError (final Meta meta, final Exception exception) {
		displayError(meta, exception != null ? TaskUtils.getEmsg(exception) : "Error: null exception.");
	}

	private void displayError (final Meta meta, final String msg) {
		final Payload placeHolder = this.metaToPayload.get(meta);
		if (placeHolder == null) throw new IllegalStateException("No cached placeholder for " + meta);

		this.payloadListAdapter.replaceItem(placeHolder, new PlaceholderPayload(this.tweet, msg));
	}

	private void fetchLinkTitle (final Meta meta) {
		publishProgress(Prg.INIT, meta, "Fetching title..."); //ES
		try {
			// TODO some form of caching of titles?
			final FinalUrlHandler<Spanned> handler = new FinalUrlHandler<Spanned>(HtmlTitleParser.INSTANCE);
			final CharSequence title = HttpHelper.get(meta.getData(), handler);
			if (title != null) {
				publishProgress(Prg.URL_AND_TITLE, meta, handler.getUrl(), title);
			}
			else {
				publishProgress(Prg.MSG, meta, "Title not found."); //ES
			}
		}
		catch (final Exception e) {
			LOG.w("Failed to retrieve title: %s", e.toString());
			publishProgress(Prg.FAIL, meta, e);
		}
	}

	private void fetchLinkedTweet (final DbInterface db, final Meta meta, final String linkedTweetSid) {
		switch (this.account.getProvider()) {
			case TWITTER:
				fetchFromTwitter(db, meta, linkedTweetSid);
			default:
		}
	}

	private void fetchFromTwitter (final DbInterface db, final Meta meta, final String linkedTweetSid) {
		publishProgress(Prg.INIT, meta, String.format("Fetching %s...", linkedTweetSid)); //ES

		final Tweet fromCache = db.getTweetDetails(linkedTweetSid);
		if (fromCache != null) {
			LOG.i("From cache: %s=%s", linkedTweetSid, fromCache);
			publishProgress(Prg.TWEET, meta, fromCache);
			return;
		}

		try {
			final Tweet linkedTweet = this.provMgr.getTwitterProvider().getTweet(this.account, Long.parseLong(linkedTweetSid), this.hdMedia);
			LOG.i("Fetched: %s=%s", linkedTweetSid, linkedTweet);
			if (linkedTweet != null) {
				db.storeTweets(Column.ID_CACHED, Collections.singletonList(linkedTweet));
			}
			publishProgress(Prg.TWEET, meta, linkedTweet);
		}
		catch (final Exception e) { // NOSONAR report all errors to UI.
			LOG.w("Failed to retrieve tweet %s: %s", linkedTweetSid, e.toString());
			publishProgress(Prg.FAIL, meta, e);
		}
	}
}

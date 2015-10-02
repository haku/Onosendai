package com.vaguehope.onosendai.model;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import android.view.View;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.TweetRowView.QuotingTweetRowView;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.provider.ProviderMgr.ProviderMgrProvider;
import com.vaguehope.onosendai.storage.DbProvider;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.Result;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;

public class LinkedTweetLoader {

	protected static final LogWrapper LOG = new LogWrapper("LTL");

	private final DbProvider dbProvider;
	private final Executor localEs;
	private final Executor netEs;
	private final ExecutorEventListener eventListener;
	private final Config config;
	private final ProviderMgrProvider provMgr;
	private final boolean hdMedia;

	public LinkedTweetLoader (final DbProvider dbProvider, final Executor localEs, final Executor netEs, final ExecutorEventListener eventListener,
			final Config config, final ProviderMgrProvider provMgr, final boolean hdMedia) {
		if (dbProvider == null) throw new IllegalArgumentException("Must specificy a db provider.");
		if (localEs == null) throw new IllegalArgumentException("Must specificy a local executor.");
		if (netEs == null) throw new IllegalArgumentException("Must specificy a network executor.");
		if (config == null) throw new IllegalArgumentException("Must specificy config.");
		if (provMgr == null) throw new IllegalArgumentException("Must specificy a provMgr.");
		this.dbProvider = dbProvider;
		this.localEs = localEs;
		this.netEs = netEs;
		this.eventListener = eventListener;
		this.config = config;
		this.provMgr = provMgr;
		this.hdMedia = hdMedia;
	}

	protected ExecutorEventListener getEventListener () {
		return this.eventListener;
	}

	protected DbProvider getDbProvider () {
		return this.dbProvider;
	}

	protected Executor getNetEs () {
		return this.netEs;
	}

	protected Config getConf () {
		return this.config;
	}

	protected ProviderMgr getProvMgr () {
		return this.provMgr.getProviderMgr();
	}

	protected boolean getHdMedia () {
		return this.hdMedia;
	}

	protected void loadTweet (final TweetLoadRequest req) {
		if (!req.shouldStartLoading()) return;
		req.displayPending();
		new TweetLoaderTask(this, req).executeOnExecutor(this.localEs);
	}

	public static class TweetLoadRequest {

		private final String parentSid;
		private final String quotedSid;
		private final QuotingTweetRowView rowView;
		private final ImageLoader imageLoader;
		private final int reqWidth;

		public TweetLoadRequest (final String parentSid, final String quotedSid, final QuotingTweetRowView rowView, final ImageLoader imageLoader, final int reqWidth) {
			this.parentSid = parentSid;
			this.quotedSid = quotedSid;
			this.rowView = rowView;
			this.imageLoader = imageLoader;
			this.reqWidth = reqWidth;
		}

		public String getParentSid () {
			return this.parentSid;
		}

		public String getQuotedSid () {
			return this.quotedSid;
		}

		private View getTrackerWidget () {
			return this.rowView.getQTweet();
		}

		public void displayIfRequired (final Tweet tweet) {
			if (!shouldFinishLoading()) return;
			display(tweet);
		}

		public void displayIfRequired (final Exception e) {
			if (!shouldFinishLoading()) return;
			display(e);
		}

		public void displayPending () {
			this.rowView.getQcte().setExpanded(false);
			this.rowView.getQTweet().setText(String.format("[ %s ]", this.quotedSid));
			this.rowView.getQName().setText("");
			this.rowView.getQAvatar().setImageResource(R.drawable.question_blue);
			this.rowView.showQInlineMedia(false);

			getTrackerWidget().setTag(R.id.imageLoading, this.quotedSid);
			getTrackerWidget().setTag(R.id.imageLoaded, null);
		}

		private void display (final Tweet quotedTweet) {
			this.rowView.getQTweet().setText(quotedTweet.getBody());

			final String usernameWithSubtitle = quotedTweet.getUsernameWithSubtitle();
			this.rowView.getQName().setText(usernameWithSubtitle != null ? usernameWithSubtitle : quotedTweet.getFullnameWithSubtitle());

			final String avatarUrl = quotedTweet.getAvatarUrl();
			if (avatarUrl != null) {
				this.imageLoader.loadImage(new ImageLoadRequest(avatarUrl, this.rowView.getQAvatar()));
			}
			else {
				this.rowView.getQAvatar().setImageResource(R.drawable.question_blue);
			}

			final String quotedInlineMediaUrl = quotedTweet.getInlineMediaUrl();
			if (quotedInlineMediaUrl != null) {
				this.rowView.showQInlineMedia(true);
				this.imageLoader.loadImage(new ImageLoadRequest(quotedInlineMediaUrl, this.rowView.getQInlineMedia(), this.reqWidth, this.rowView.getQInlineMediaLoadListener()));
			}
			else {
				this.rowView.showQInlineMedia(false);
			}

			getTrackerWidget().setTag(R.id.imageLoading, null);
			getTrackerWidget().setTag(R.id.imageLoaded, this.quotedSid);
		}

		private void display (final Exception e) {
			this.rowView.getQTweet().setText(ExcpetionHelper.veryShortMessage(e));
			this.rowView.getQName().setText(String.format("[ %s ]", this.quotedSid));
			this.rowView.getQAvatar().setImageResource(R.drawable.exclamation_red);
			this.rowView.showQInlineMedia(false);

			getTrackerWidget().setTag(R.id.imageLoading, null);
			getTrackerWidget().setTag(R.id.imageLoaded, null);
		}

		/**
		 * On UI thread.
		 */
		public boolean shouldStartLoading () {
			return !this.quotedSid.equals(getTrackerWidget().getTag(R.id.imageLoaded));
		}

		/**
		 * On BG thread.
		 */
		public boolean shouldFinishLoading () {
			return this.quotedSid.equals(getTrackerWidget().getTag(R.id.imageLoading));
		}

	}

	private static class TweetLoaderTask extends TrackingAsyncTask<Void, Void, Result<Tweet>> {

		private final LinkedTweetLoader loader;
		private final TweetLoadRequest req;

		public TweetLoaderTask (final LinkedTweetLoader loader, final TweetLoadRequest req) {
			super(loader.getEventListener());
			this.loader = loader;
			this.req = req;
		}

		@Override
		public String toString () {
			return "load:" + this.req.getQuotedSid();
		}

		@Override
		protected Result<Tweet> doInBackgroundWithTracking (final Void... params) {
			if (!this.req.shouldFinishLoading()) return null;
			try {
				final Tweet tweet = this.loader.getDbProvider().getDb().getTweetDetails(this.req.getQuotedSid());
				return tweet != null ? new Result<Tweet>(tweet) : null;
			}
			catch (final Exception e) { // NOSONAR To report errors.
				return new Result<Tweet>(e);
			}
			catch (final Throwable e) { // NOSONAR To report errors.
				return new Result<Tweet>(new ExecutionException("Failed to load tweet.", e));
			}
		}

		@Override
		protected void onPostExecute (final Result<Tweet> result) {
			if (result == null) {
				if (this.req.shouldFinishLoading()) {
					new TweetFetcherTask(this.loader, this.req).executeOnExecutor(this.loader.getNetEs());
				}
			}
			else if (result.isSuccess()) {
				this.req.displayIfRequired(result.getData());
			}
			else {
				LOG.e("Error loading tweet.", result.getE());
				this.req.displayIfRequired(result.getE());
			}
		}

	}

	private static class TweetFetcherTask extends TrackingAsyncTask<Void, Void, Result<Tweet>> {

		private final LinkedTweetLoader loader;
		private final TweetLoadRequest req;

		public TweetFetcherTask (final LinkedTweetLoader loader, final TweetLoadRequest req) {
			super(loader.getEventListener());
			this.loader = loader;
			this.req = req;
		}

		@Override
		public String toString () {
			return "fetch:" + this.req.getQuotedSid();
		}

		@Override
		protected Result<Tweet> doInBackgroundWithTracking (final Void... params) {
			if (!this.req.shouldFinishLoading()) return null;
			try {
				final Tweet parent = this.loader.getDbProvider().getDb().getTweetDetails(this.req.getParentSid());
				if (parent != null) {
					final Account account = MetaUtils.accountFromMeta(parent, this.loader.getConf());
					if (account != null) {
						final Tweet tweet = this.loader.getProvMgr().getTwitterProvider().getTweet(account, Long.parseLong(this.req.getQuotedSid()), this.loader.getHdMedia());
						LOG.i("Fetched: %s=%s", this.req.getQuotedSid(), tweet);
						this.loader.getDbProvider().getDb().storeTweets(Column.ID_CACHED, Collections.singletonList(tweet));
						return new Result<Tweet>(tweet);
					}
					return new Result<Tweet>(new IllegalStateException("Tweet missing account meta: " + parent));
				}
				return new Result<Tweet>(new IllegalStateException("Tweet not in DB: " + this.req.getParentSid()));
			}
			catch (final Exception e) { // NOSONAR To report errors.
				return new Result<Tweet>(e);
			}
			catch (final Throwable e) { // NOSONAR To report errors.
				return new Result<Tweet>(new ExecutionException("Failed to fetch tweet.", e));
			}
		}

		@Override
		protected void onPostExecute (final Result<Tweet> result) {
			if (result == null) return; // Request was no longer required.
			if (result.isSuccess()) {
				this.req.displayIfRequired(result.getData());
			}
			else {
				LOG.e("Error fetching tweet.", result.getE());
				this.req.displayIfRequired(result.getE());
			}
		}

	}

}

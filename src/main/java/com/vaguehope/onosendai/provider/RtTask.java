package com.vaguehope.onosendai.provider;

import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.RtTask.RtResult;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;

public class RtTask extends AsyncTask<Void, Void, RtResult> {

	private final Context context;
	private final RtRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public RtTask (final Context context, final RtRequest req) {
		this.context = context;
		this.req = req;
		this.notificationId = (int) System.currentTimeMillis(); // Probably unique.
	}

	@Override
	protected void onPreExecute () {
		this.notificationMgr = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new NotificationCompat.Builder(this.context)
				.setSmallIcon(R.drawable.question_blue) // TODO better icon.
				.setContentTitle(String.format("RTing via %s...", this.req.getAccount().toHumanString()))
				.setOngoing(true)
				.setUsesChronometer(true)
				.build();
		this.notificationMgr.notify(this.notificationId, n);
	}

	@Override
	protected RtResult doInBackground (final Void... params) {
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return rtViaTwitter();
			default:
				return new RtResult(this.req, new UnsupportedOperationException("Do not know how to RT via account type: " + this.req.getAccount().toHumanString()));
		}
	}

	private RtResult rtViaTwitter () {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.rt(this.req.getAccount(), Long.parseLong(this.req.getTweet().getSid()));
			return new RtResult(this.req);
		}
		catch (TwitterException e) {
			return new RtResult(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final RtResult res) {
		if (!res.isSuccess()) {
			Notification n = new NotificationCompat.Builder(this.context)
					.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
					.setContentTitle(String.format("Failed to RT via %s.", this.req.getAccount().toHumanString()))
					.setContentText(res.getE().toString())
					.setAutoCancel(true)
					.setUsesChronometer(false)
					.build();
			this.notificationMgr.notify(this.notificationId, n);
		}
		else {
			this.notificationMgr.cancel(this.notificationId);
		}
	}

	public static class RtRequest {

		private final Account account;
		private final Tweet tweet;

		public RtRequest (final Account account, final Tweet tweet) {
			this.account = account;
			this.tweet = tweet;
		}

		public Account getAccount () {
			return this.account;
		}

		public Tweet getTweet () {
			return this.tweet;
		}

	}

	protected static class RtResult {

		private final boolean success;
		private final RtRequest request;
		private final Exception e;

		public RtResult (final RtRequest request) {
			this.success = true;
			this.request = request;
			this.e = null;
		}

		public RtResult (final RtRequest request, final Exception e) {
			this.success = false;
			this.request = request;
			this.e = e;
		}

		public boolean isSuccess () {
			return this.success;
		}

		public RtRequest getRequest () {
			return this.request;
		}

		public Exception getE () {
			return this.e;
		}

	}

}

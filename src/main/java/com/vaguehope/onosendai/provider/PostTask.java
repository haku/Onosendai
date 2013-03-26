package com.vaguehope.onosendai.provider;

import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.PostTask.PostResult;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;

public class PostTask extends AsyncTask<Void, Void, PostResult> {

	private final Context context;
	private final PostRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public PostTask (final Context context, final PostRequest req) {
		this.context = context;
		this.req = req;
		this.notificationId = (int) System.currentTimeMillis(); // Probably unique.
	}

	@Override
	protected void onPreExecute () {
		this.notificationMgr = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new NotificationCompat.Builder(this.context)
				.setSmallIcon(R.drawable.question_blue) // TODO better icon.
				.setContentTitle(String.format("Posting to %s...", this.req.getAccount().toHumanString()))
				.setOngoing(true)
				.setUsesChronometer(true)
				.build();
		this.notificationMgr.notify(this.notificationId, n);
	}

	@Override
	protected PostResult doInBackground (final Void... params) {
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return postTwitter();
			default:
				return new PostResult(this.req, new UnsupportedOperationException("Do not know how to post to account type: " + this.req.getAccount().toHumanString()));
		}
	}

	private PostResult postTwitter () {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.addAccount(this.req.getAccount());
			p.post(this.req.getAccount(), this.req.getBody(), this.req.getInReplyTo());
			return new PostResult(this.req);
		}
		catch (TwitterException e) {
			return new PostResult(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final PostResult res) {
		if (!res.isSuccess()) {
			PendingIntent contentIntent = PendingIntent.getActivity(this.context, 0, this.req.getRecoveryIntent(), 0);
			Notification n = new NotificationCompat.Builder(this.context)
					.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
					.setContentTitle(String.format("Tap to retry post to %s.", this.req.getAccount().toHumanString()))
					.setContentText(res.getE().toString())
					.setContentIntent(contentIntent)
					.setAutoCancel(true)
					.setUsesChronometer(false)
					.build();
			this.notificationMgr.notify(this.notificationId, n);
		}
		else {
			this.notificationMgr.cancel(this.notificationId);
		}
	}

	public static class PostRequest {

		private final Account account;
		private final String body;
		private final long inReplyTo;
		private final Intent recoveryIntent;

		public PostRequest (final Account account, final String body, final long inReplyTo, final Intent recoveryIntent) {
			this.account = account;
			this.body = body;
			this.inReplyTo = inReplyTo;
			this.recoveryIntent = recoveryIntent;
		}

		public Account getAccount () {
			return this.account;
		}

		public String getBody () {
			return this.body;
		}

		public long getInReplyTo () {
			return this.inReplyTo;
		}

		public Intent getRecoveryIntent () {
			return this.recoveryIntent;
		}

	}

	protected static class PostResult {

		private final boolean success;
		private final PostRequest request;
		private final Exception e;

		public PostResult (final PostRequest request) {
			this.success = true;
			this.request = request;
			this.e = null;
		}

		public PostResult (final PostRequest request, final Exception e) {
			this.success = false;
			this.request = request;
			this.e = e;
		}

		public boolean isSuccess () {
			return this.success;
		}

		public PostRequest getRequest () {
			return this.request;
		}

		public Exception getE () {
			return this.e;
		}

	}

}

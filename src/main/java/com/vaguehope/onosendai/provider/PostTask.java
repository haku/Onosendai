package com.vaguehope.onosendai.provider;

import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.PostTask.PostResult;
import com.vaguehope.onosendai.provider.bufferapp.BufferAppProvider;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class PostTask extends DbBindingAsyncTask<Void, Void, PostResult> {

	private static final LogWrapper LOG = new LogWrapper("PT");

	private final Context context;
	private final PostRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public PostTask (final Context context, final PostRequest req) {
		super(context);
		this.context = context;
		this.req = req;
		this.notificationId = (int) System.currentTimeMillis(); // Probably unique.
	}

	@Override
	protected LogWrapper getLog () {
		return LOG;
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
	protected PostResult doInBackgroundWithDb (final DbInterface db, final Void... params) {
		LOG.i("Posting: %s", this.req);
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return postTwitter();
			case SUCCESSWHALE:
				return postSuccessWhale(db);
			case BUFFER:
				return postBufferApp();
			default:
				return new PostResult(this.req, new UnsupportedOperationException("Do not know how to post to account type: " + this.req.getAccount().toHumanString()));
		}
	}

	private PostResult postTwitter () {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.post(this.req.getAccount(), this.req.getBody(), this.req.getInReplyToSidLong());
			return new PostResult(this.req);
		}
		catch (Exception e) { // NOSONAR need to report all errors.
			return new PostResult(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private PostResult postSuccessWhale (final DbInterface db) {
		final SuccessWhaleProvider s = new SuccessWhaleProvider(db);
		try {
			s.post(this.req.getAccount(), this.req.getPostToSvc(), this.req.getBody(), this.req.getInReplyToSid());
			return new PostResult(this.req);
		}
		catch (Exception e) { // NOSONAR need to report all errors.
			return new PostResult(this.req, e);
		}
		finally {
			s.shutdown();
		}
	}

	private PostResult postBufferApp () {
		final BufferAppProvider b = new BufferAppProvider();
		try {
			if (this.req.getInReplyToSid() != null) LOG.w("BufferApp does not support inReplyTo field, ignoring it.");
			b.post(this.req.getAccount(), this.req.getPostToSvc(), this.req.getBody());
			return new PostResult(this.req);
		}
		catch (Exception e) { // NOSONAR need to report all errors.
			return new PostResult(this.req, e);
		}
		finally {
			b.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final PostResult res) {
		if (!res.isSuccess()) {
			LOG.e("Post failed.", res.getE());
			PendingIntent contentIntent = PendingIntent.getActivity(this.context, 0, this.req.getRecoveryIntent(), PendingIntent.FLAG_CANCEL_CURRENT);
			Notification n = new NotificationCompat.Builder(this.context)
					.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
					.setContentTitle(String.format("Tap to retry post to %s.", this.req.getAccount().toHumanString()))
					.setContentText(res.getEmsg())
					.setContentIntent(contentIntent)
					.setAutoCancel(true)
					.setUsesChronometer(false)
					.setWhen(System.currentTimeMillis())
					.build();
			this.notificationMgr.notify(this.notificationId, n);
		}
		else {
			this.notificationMgr.cancel(this.notificationId);
		}
	}

	public static class PostRequest {

		private final Account account;
		private final Set<ServiceRef> postToSvc;
		private final String body;
		private final String inReplyToSid;
		private final Intent recoveryIntent;

		public PostRequest (final Account account, final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid, final Intent recoveryIntent) {
			this.account = account;
			this.postToSvc = postToSvc;
			this.body = body;
			this.inReplyToSid = inReplyToSid;
			this.recoveryIntent = recoveryIntent;
		}

		public Account getAccount () {
			return this.account;
		}

		public Set<ServiceRef> getPostToSvc () {
			return this.postToSvc;
		}

		public String getBody () {
			return this.body;
		}

		public String getInReplyToSid () {
			return this.inReplyToSid;
		}

		public long getInReplyToSidLong () {
			if (this.inReplyToSid == null || this.inReplyToSid.isEmpty()) return -1;
			return Long.parseLong(this.inReplyToSid);
		}

		public Intent getRecoveryIntent () {
			return this.recoveryIntent;
		}

		@Override
		public String toString () {
			return new StringBuilder()
					.append("PostRequest{").append(this.account)
					.append(",").append(this.postToSvc)
					.append(",").append(this.inReplyToSid)
					.append("}").toString();
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

		public String getEmsg () {
			return TaskUtils.getEmsg(this.e);
		}

	}

}

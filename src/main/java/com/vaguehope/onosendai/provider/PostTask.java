package com.vaguehope.onosendai.provider;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.PostTask.PostResult;
import com.vaguehope.onosendai.provider.bufferapp.BufferAppProvider;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.OutboxActivity;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.LogWrapper;

public class PostTask extends DbBindingAsyncTask<Void, Integer, PostResult> {

	private static final LogWrapper LOG = new LogWrapper("PT");

	private final PostRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;
	private NotificationCompat.Builder notificationBuilder;

	public PostTask (final Context context, final PostRequest req) {
		super(context);
		this.req = req;
		this.notificationId = (int) System.currentTimeMillis(); // Probably unique.
	}

	@Override
	protected LogWrapper getLog () {
		return LOG;
	}

	@Override
	protected void onPreExecute () {
		this.notificationMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		this.notificationBuilder = new NotificationCompat.Builder(getContext())
				.setSmallIcon(Notifications.notificationIcon())
				.setContentTitle(String.format("Posting to %s...", this.req.getAccount().getUiTitle()))
				.setOngoing(true)
				.setUsesChronometer(true);
		updateNotificaiton();
	}

	private void updateNotificaiton () {
		this.notificationMgr.notify(this.notificationId, this.notificationBuilder.build());
	}

	protected void setProgress (final int max, final int progress) {
		this.notificationBuilder.setProgress(max, progress, false);
		updateNotificaiton();
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
				return new PostResult(this.req, new UnsupportedOperationException("Do not know how to post to account type: " + this.req.getAccount().getUiTitle()));
		}
	}

	private PostResult postTwitter () {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.post(this.req.getAccount(), this.req.getBody(), this.req.getInReplyToSidLong(), resolveAttachment());
			return new PostResult(this.req);
		}
		catch (final Exception e) { // NOSONAR need to report all errors.
			return new PostResult(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private PostResult postSuccessWhale (final DbInterface db) {
		final SuccessWhaleProvider s = new SuccessWhaleProvider(db);
		try {
			s.post(this.req.getAccount(), this.req.getPostToSvc(), this.req.getBody(), this.req.getInReplyToSid(), resolveAttachment());
			return new PostResult(this.req);
		}
		catch (final Exception e) { // NOSONAR need to report all errors.
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
		catch (final Exception e) { // NOSONAR need to report all errors.
			return new PostResult(this.req, e);
		}
		finally {
			b.shutdown();
		}
	}

	private ImageMetadata resolveAttachment () throws FileNotFoundException {
		if (this.req.getAttachment() == null) return null;
		final ImageMetadata image = new ProgressTrackingImageMetadata(this, getContext(), this.req.getAttachment());
		if (!image.exists()) throw new FileNotFoundException("Attachment not found: " + this.req.getAttachment());
		return image;
	}

	@Override
	protected void onPostExecute (final PostResult res) {
		if (!res.isSuccess()) {
			LOG.w("Post failed.", res.getE());
			Intent intent;
			String title;
			if (this.req.getRecoveryIntent() != null) {
				intent = this.req.getRecoveryIntent();
				title = String.format("Tap to retry post to %s.", this.req.getAccount().getUiTitle());
			}
			else {
				intent = new Intent(getContext(), OutboxActivity.class);
				title = String.format("Post to %s will be retried in background.", this.req.getAccount().getUiTitle());
			}
			final PendingIntent contentIntent = PendingIntent.getActivity(getContext(), this.notificationId,
					intent, PendingIntent.FLAG_CANCEL_CURRENT);
			final Notification n = new NotificationCompat.Builder(getContext())
					.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
					.setContentText(res.getEmsg())
					.setAutoCancel(true)
					.setUsesChronometer(false)
					.setWhen(System.currentTimeMillis())
					.setContentIntent(contentIntent)
					.setContentTitle(title)
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
		private final Uri attachment;
		private final Intent recoveryIntent;

		public PostRequest (final Account account, final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid, final Uri attachment) {
			this(account, postToSvc, body, inReplyToSid, attachment, null);
		}

		public PostRequest (final Account account, final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid, final Uri attachment, final Intent recoveryIntent) {
			this.account = account;
			this.postToSvc = postToSvc;
			this.body = body;
			this.inReplyToSid = inReplyToSid;
			this.attachment = attachment;
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

		public Uri getAttachment () {
			return this.attachment;
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
					.append(",").append(this.attachment)
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

	private static class ProgressTrackingImageMetadata extends ImageMetadata {

		private final PostTask host;

		public ProgressTrackingImageMetadata (final PostTask host, final Context context, final Uri uri) {
			super(context, uri);
			this.host = host;
		}

		@Override
		public InputStream open () throws IOException {
			final InputStream is = super.open();
			if (is == null) return null;
			return new ProgressTrackingInputStream(this.host, getSize(), is);
		}

	}

	private static class ProgressTrackingInputStream extends FilterInputStream {

		private static final float PERCENT_F = 100f;
		private static final int PERCENT_I = 100;

		private final PostTask host;
		private final long size;

		private long progress = 0;
		private int lastPercent = -1;

		protected ProgressTrackingInputStream (final PostTask host, final long size, final InputStream is) {
			super(is);
			this.host = host;
			this.size = size;
		}

		private void increment (final int added) {
			if (added < 1) return;
			this.progress += added;
			final int percent = (int) (this.progress * PERCENT_F / this.size);
			if (percent != this.lastPercent) {
				this.host.setProgress(PERCENT_I, percent);
				this.lastPercent = percent;
			}
		}

		@Override
		public int read () throws IOException {
			final int n = super.read();
			increment(1);
			return n;
		}

		@Override
		public int read (final byte[] buffer, final int offset, final int count) throws IOException {
			final int n = super.read(buffer, offset, count);
			increment(n);
			return n;
		}

	}

}

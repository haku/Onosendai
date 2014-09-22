package com.vaguehope.onosendai.provider;

import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.DeleteTask.DeleteRequest;
import com.vaguehope.onosendai.provider.successwhale.ItemAction;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.OutboxActivity;
import com.vaguehope.onosendai.util.LogWrapper;

public class DeleteTask extends DbBindingAsyncTask<Void, Void, SendResult<DeleteRequest>> {

	private static final LogWrapper LOG = new LogWrapper("DT");

	private final Context context;
	private final DeleteRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public DeleteTask (final Context context, final DeleteRequest req) {
		super(context);
		this.context = context;
		this.req = req;
		this.notificationId = (int) (req.getReqId() != null ? req.getReqId() : System.currentTimeMillis()); // Probably unique.
	}

	@Override
	protected LogWrapper getLog () {
		return LOG;
	}

	@Override
	protected void onPreExecute () {
		this.notificationMgr = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification n = new NotificationCompat.Builder(this.context)
				.setSmallIcon(Notifications.notificationIcon())
				.setContentTitle(String.format("Deleting update for %s...", this.req.getAccount().getUiTitle()))
				.setOngoing(true)
				.setUsesChronometer(true)
				.build();
		this.notificationMgr.notify(this.notificationId, n);
	}

	@Override
	protected SendResult<DeleteRequest> doInBackgroundWithDb (final DbInterface db, final Void... params) {
		final String editSid = this.req.getSid();
		LOG.i("Deleting: editSid=%s; %s.", editSid, this.req);
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return deleteViaTwitter(editSid);
			case SUCCESSWHALE:
				return deleteViaSuccessWhale(db, editSid);
			default:
				return new SendResult<DeleteRequest>(this.req, new UnsupportedOperationException("Do not know how to delete via account type: " + this.req.getAccount().getUiTitle()));
		}
	}

	private SendResult<DeleteRequest> deleteViaTwitter (final String editSid) {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.delete(this.req.getAccount(), Long.parseLong(editSid));
			LOG.i("Deleted tweet: editSid=%s", editSid);
			return new SendResult<DeleteRequest>(this.req);
		}
		catch (final TwitterException e) {
			return new SendResult<DeleteRequest>(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private SendResult<DeleteRequest> deleteViaSuccessWhale (final DbInterface db, final String editSid) {
		final SuccessWhaleProvider p = new SuccessWhaleProvider(db);
		try {
			final ServiceRef svc = this.req.getSvc();
			if (svc != null) {
				final NetworkType networkType = svc.getType();
				if (networkType != null) {
					switch (networkType) {
						case TWITTER:
							p.itemAction(this.req.getAccount(), svc, editSid, ItemAction.DELETE_TWITTER);
							LOG.i("Deleted SW tweet: editSid=%s", editSid);
							return new SendResult<DeleteRequest>(this.req);
						case FACEBOOK:
							p.itemAction(this.req.getAccount(), svc, editSid, ItemAction.DELETE_FACEBOOK);
							LOG.i("Deleted FB update: editSid=%s", editSid);
							return new SendResult<DeleteRequest>(this.req);
						default:
							return new SendResult<DeleteRequest>(this.req, new SuccessWhaleException("Unknown network type: " + networkType));
					}
				}
				return new SendResult<DeleteRequest>(this.req, new SuccessWhaleException("Service metadata missing network type: " + svc));
			}
			return new SendResult<DeleteRequest>(this.req, new SuccessWhaleException("Invalid service metadata: " + svc));
		}
		catch (final SuccessWhaleException e) {
			return new SendResult<DeleteRequest>(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final SendResult<DeleteRequest> res) {
		switch (res.getOutcome()) {
			case SUCCESS:
			case PREVIOUS_ATTEMPT_SUCCEEDED:
				this.notificationMgr.cancel(this.notificationId);
				break;
			default:
				LOG.w("Delete failed: %s", res.getE());
				final Notification n = new NotificationCompat.Builder(this.context)
						.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
						.setContentTitle(String.format("Failed to delete via %s.", this.req.getAccount().getUiTitle()))
						.setContentText(res.getEmsg())
						.setAutoCancel(true)
						.setUsesChronometer(false)
						.setWhen(System.currentTimeMillis())
						.setContentIntent(PendingIntent.getActivity(getContext(), this.notificationId,
								new Intent(getContext(), OutboxActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
						.build();
				this.notificationMgr.notify(this.notificationId, n);
		}
	}

	public static class DeleteRequest {

		private final Long reqId;
		private final Account account;
		private final ServiceRef svc;
		private final String sid;

		public DeleteRequest (final Long reqId, final Account account, final ServiceRef svc, final String sid) {
			this.reqId = reqId;
			this.account = account;
			this.svc = svc;
			this.sid = sid;
		}

		public Long getReqId () {
			return this.reqId;
		}

		public Account getAccount () {
			return this.account;
		}

		public ServiceRef getSvc () {
			return this.svc;
		}

		public String getSid () {
			return this.sid;
		}

		@Override
		public String toString () {
			return new StringBuilder()
					.append("DeleteRequest{").append(this.account)
					.append(",").append(this.svc)
					.append(",").append(this.sid)
					.append("}").toString();
		}

	}

}

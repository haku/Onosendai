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
import com.vaguehope.onosendai.provider.RtTask.RtRequest;
import com.vaguehope.onosendai.provider.successwhale.ItemAction;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.OutboxActivity;
import com.vaguehope.onosendai.util.LogWrapper;

public class RtTask extends DbBindingAsyncTask<Void, Void, SendResult<RtRequest>> {

	private static final LogWrapper LOG = new LogWrapper("RT");

	private final Context context;
	private final RtRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public RtTask (final Context context, final RtRequest req) {
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
				.setContentTitle(String.format("RTing via %s...", this.req.getAccount().getUiTitle()))
				.setOngoing(true)
				.setUsesChronometer(true)
				.build();
		this.notificationMgr.notify(this.notificationId, n);
	}

	@Override
	protected SendResult<RtRequest> doInBackgroundWithDb (final DbInterface db, final Void... params) {
		LOG.i("RTing: %s", this.req);
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return rtViaTwitter();
			case SUCCESSWHALE:
				return rtViaSuccessWhale(db);
			default:
				return new SendResult<RtRequest>(this.req, new UnsupportedOperationException("Do not know how to RT via account type: " + this.req.getAccount().getUiTitle()));
		}
	}

	private SendResult<RtRequest> rtViaTwitter () {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.rt(this.req.getAccount(), Long.parseLong(this.req.getSid()));
			return new SendResult<RtRequest>(this.req);
		}
		catch (final TwitterException e) {
			return new SendResult<RtRequest>(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private SendResult<RtRequest> rtViaSuccessWhale (final DbInterface db) {
		final SuccessWhaleProvider p = new SuccessWhaleProvider(db);
		try {
			final ServiceRef svc = this.req.getSvc();
			if (svc != null) {
				final NetworkType networkType = svc.getType();
				if (networkType != null) {
					switch (networkType) {
						case TWITTER:
							p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.RETWEET);
							return new SendResult<RtRequest>(this.req);
						case FACEBOOK:
							p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.LIKE);
							return new SendResult<RtRequest>(this.req);
						default:
							return new SendResult<RtRequest>(this.req, new SuccessWhaleException("Unknown network type: " + networkType));
					}
				}
				return new SendResult<RtRequest>(this.req, new SuccessWhaleException("Service metadata missing network type: " + svc));
			}
			return new SendResult<RtRequest>(this.req, new SuccessWhaleException("Invalid service metadata: " + svc));
		}
		catch (final SuccessWhaleException e) {
			return new SendResult<RtRequest>(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final SendResult<RtRequest> res) {
		switch (res.getOutcome()) {
			case SUCCESS:
			case PREVIOUS_ATTEMPT_SUCCEEDED:
				this.notificationMgr.cancel(this.notificationId);
				break;
			default:
				LOG.w("RT failed: %s", res.getE());
				final Notification n = new NotificationCompat.Builder(this.context)
						.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
						.setContentTitle(String.format("Failed to RT via %s.", this.req.getAccount().getUiTitle()))
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

	public static class RtRequest {

		private final Long reqId;
		private final Account account;
		private final ServiceRef svc;
		private final String sid;

		public RtRequest (final Long reqId, final Account account, final ServiceRef svc, final String sid) {
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
					.append("RtRequest{").append(this.account)
					.append(",").append(this.svc)
					.append(",").append(this.sid)
					.append("}").toString();
		}

	}

}

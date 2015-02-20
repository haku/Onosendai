package com.vaguehope.onosendai.provider;

import java.util.concurrent.TimeUnit;

import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxAction;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.OutboxTask.OtRequest;
import com.vaguehope.onosendai.provider.successwhale.ItemAction;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.OutboxActivity;
import com.vaguehope.onosendai.util.LogWrapper;

public class OutboxTask extends DbBindingAsyncTask<Void, Void, SendResult<OtRequest>> {

	private static final LogWrapper LOG = new LogWrapper("OT");

	private final Context context;
	private final OtRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public OutboxTask (final Context context, final OtRequest req) {
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
				.setContentTitle(String.format("%s via %s...", this.req.getAction().getUiVerb(), this.req.getAccount().getUiTitle())) //ES
				.setOngoing(true)
				.setUsesChronometer(true)
				.build();
		this.notificationMgr.notify(this.notificationId, n);
	}

	@Override
	protected SendResult<OtRequest> doInBackgroundWithDb (final DbInterface db, final Void... params) {
		LOG.i("%s: %s", this.req.getAction().getUiVerb(), this.req);
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return viaTwitter(db);
			case SUCCESSWHALE:
				return viaSuccessWhale(db);
			default:
				return new SendResult<OtRequest>(this.req, new UnsupportedOperationException("Do not know how to " + this.req.getAction().getUiTitle() + " via account type: " + this.req.getAccount().getUiTitle()));
		}
	}

	private SendResult<OtRequest> viaTwitter (final DbInterface db) {
		final TwitterProvider p = new TwitterProvider();
		try {
			switch (this.req.getAction()) {
				case RT:
					p.rt(this.req.getAccount(), Long.parseLong(this.req.getSid()));
					LOG.i("RTed tweet: sid=%s", this.req.getSid());
					return new SendResult<OtRequest>(this.req);
				case FAV:
					p.fav(this.req.getAccount(), Long.parseLong(this.req.getSid()));
					LOG.i("Favorited tweet: sid=%s", this.req.getSid());
					return new SendResult<OtRequest>(this.req);
				case DELETE:
					p.delete(this.req.getAccount(), Long.parseLong(this.req.getSid()));
					LOG.i("Deleted tweet: editSid=%s", this.req.getSid());
					markAsDeleted(db);
					return new SendResult<OtRequest>(this.req);
				default:
					return new SendResult<OtRequest>(this.req, new UnsupportedOperationException("Do not know how to " + this.req.getAction().getUiTitle() + " via account type: " + this.req.getAccount().getUiTitle()));
			}
		}
		catch (final TwitterException e) {
			return new SendResult<OtRequest>(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private SendResult<OtRequest> viaSuccessWhale (final DbInterface db) {
		final SuccessWhaleProvider p = new SuccessWhaleProvider(db);
		try {
			final ServiceRef svc = this.req.getSvc();
			if (svc != null) {
				final NetworkType networkType = svc.getType();
				if (networkType != null) {
					switch (networkType) {
						case TWITTER:
							switch (this.req.getAction()) {
								case RT:
									p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.RETWEET);
									LOG.i("RTed tweet: sid=%s", this.req.getSid());
									return new SendResult<OtRequest>(this.req);
								case FAV:
									p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.FAVORITE);
									LOG.i("Favorited tweet: sid=%s", this.req.getSid());
									return new SendResult<OtRequest>(this.req);
								case DELETE:
									p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.DELETE_TWITTER);
									LOG.i("Deleted SW tweet: editSid=%s", this.req.getSid());
									markAsDeleted(db);
									return new SendResult<OtRequest>(this.req);
								default:
									return new SendResult<OtRequest>(this.req, new UnsupportedOperationException("Do not know how to " + this.req.getAction().getUiTitle() + " via account type: " + this.req.getAccount().getUiTitle()));
							}
						case FACEBOOK:
							switch (this.req.getAction()) {
								case RT:
									p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.LIKE);
									LOG.i("Liked FB update: sid=%s", this.req.getSid());
									return new SendResult<OtRequest>(this.req);
								case DELETE:
									p.itemAction(this.req.getAccount(), svc, this.req.getSid(), ItemAction.DELETE_FACEBOOK);
									LOG.i("Deleted FB update: editSid=%s", this.req.getSid());
									markAsDeleted(db);
									return new SendResult<OtRequest>(this.req);
								default:
									return new SendResult<OtRequest>(this.req, new UnsupportedOperationException("Do not know how to " + this.req.getAction().getUiTitle() + " via account type: " + this.req.getAccount().getUiTitle()));
							}
						default:
							return new SendResult<OtRequest>(this.req, new SuccessWhaleException("Unknown network type: " + networkType));
					}
				}
				return new SendResult<OtRequest>(this.req, new SuccessWhaleException("Service metadata missing network type: " + svc));
			}
			return new SendResult<OtRequest>(this.req, new SuccessWhaleException("Invalid service metadata: " + svc));
		}
		catch (final SuccessWhaleException e) {
			return new SendResult<OtRequest>(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private void markAsDeleted (final DbInterface db) {
		final Meta meta = new Meta(MetaType.DELETED, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
		for (final Tweet t : db.getTweetsWithSid(this.req.getSid())) {
			db.appendToTweet(t, meta);
			LOG.i("Marked as deleted: uid=%s sid=%s", t.getUid(), this.req.getSid());
		}
	}

	@Override
	protected void onPostExecute (final SendResult<OtRequest> res) {
		switch (res.getOutcome()) {
			case SUCCESS:
			case PREVIOUS_ATTEMPT_SUCCEEDED:
				this.notificationMgr.cancel(this.notificationId);
				break;
			default:
				LOG.w("%s failed: %s", this.req.getAction().getUiTitle(), res.getE()); //ES
				final Notification n = new NotificationCompat.Builder(this.context)
						.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
						.setContentTitle(String.format("Failed to %s via %s.", this.req.getAction().getUiTitle(), this.req.getAccount().getUiTitle()))
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

	public static class OtRequest {

		private final OutboxAction action;
		private final Long reqId;
		private final Account account;
		private final ServiceRef svc;
		private final String sid;

		public OtRequest (final OutboxAction action, final Long reqId, final Account account, final ServiceRef svc, final String sid) {
			this.action = action;
			this.reqId = reqId;
			this.account = account;
			this.svc = svc;
			this.sid = sid;
		}

		public OutboxAction getAction () {
			return this.action;
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
					.append("OtRequest{").append(this.action)
					.append(",").append(this.account)
					.append(",").append(this.svc)
					.append(",").append(this.sid)
					.append("}").toString();
		}

	}
}

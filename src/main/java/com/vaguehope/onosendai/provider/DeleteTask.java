package com.vaguehope.onosendai.provider;

import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.notifications.Notifications;
import com.vaguehope.onosendai.provider.DeleteTask.DeleteResult;
import com.vaguehope.onosendai.provider.successwhale.ItemAction;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.provider.twitter.TwitterProvider;
import com.vaguehope.onosendai.storage.DbBindingAsyncTask;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.util.LogWrapper;

public class DeleteTask extends DbBindingAsyncTask<Void, Void, DeleteResult> {

	private static final LogWrapper LOG = new LogWrapper("DT");

	private final Context context;
	private final DeleteRequest req;
	private final int notificationId;

	private NotificationManager notificationMgr;

	public DeleteTask (final Context context, final DeleteRequest req) {
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
		final Notification n = new NotificationCompat.Builder(this.context)
				.setSmallIcon(Notifications.notificationIcon())
				.setContentTitle(String.format("Deleting update for %s...", this.req.getAccount().getUiTitle()))
				.setOngoing(true)
				.setUsesChronometer(true)
				.build();
		this.notificationMgr.notify(this.notificationId, n);
	}

	@Override
	protected DeleteResult doInBackgroundWithDb (final DbInterface db, final Void... params) {
		final Meta editSidMeta = this.req.getTweet().getFirstMetaOfType(MetaType.EDIT_SID);
		if (editSidMeta == null) throw new IllegalStateException("Tried to delete a tweet with out EDIT_SID set: " + this.req.getTweet());
		final String editSid = editSidMeta.getData();

		LOG.i("Deleting: editSid=%s; %s.", editSid, this.req);
		switch (this.req.getAccount().getProvider()) {
			case TWITTER:
				return deleteViaTwitter(editSid);
			case SUCCESSWHALE:
				return deleteViaSuccessWhale(db, editSid);
			default:
				return new DeleteResult(this.req, new UnsupportedOperationException("Do not know how to delete via account type: " + this.req.getAccount().getUiTitle()));
		}
	}

	private DeleteResult deleteViaTwitter (final String editSid) {
		final TwitterProvider p = new TwitterProvider();
		try {
			p.delete(this.req.getAccount(), Long.parseLong(editSid));
			LOG.i("Deleted tweet: editSid=%s", editSid);
			return new DeleteResult(this.req);
		}
		catch (final TwitterException e) {
			return new DeleteResult(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	private DeleteResult deleteViaSuccessWhale (final DbInterface db, final String editSid) {
		final SuccessWhaleProvider p = new SuccessWhaleProvider(db);
		try {
			final Meta svcMeta = this.req.getTweet().getFirstMetaOfType(MetaType.SERVICE);
			if (svcMeta != null) {
				final ServiceRef svc = ServiceRef.parseServiceMeta(svcMeta);
				if (svc != null) {
					final NetworkType networkType = svc.getType();
					if (networkType != null) {
						switch (networkType) {
							case TWITTER:
								p.itemAction(this.req.getAccount(), svc, editSid, ItemAction.DELETE_TWITTER);
								LOG.i("Deleted SW tweet: editSid=%s", editSid);
								return new DeleteResult(this.req);
							case FACEBOOK:
								p.itemAction(this.req.getAccount(), svc, editSid, ItemAction.DELETE_FACEBOOK);
								LOG.i("Deleted FB update: editSid=%s", editSid);
								return new DeleteResult(this.req);
							default:
								return new DeleteResult(this.req, new SuccessWhaleException("Unknown network type: " + networkType));
						}
					}
					return new DeleteResult(this.req, new SuccessWhaleException("Service metadata missing network type: " + svc));
				}
				return new DeleteResult(this.req, new SuccessWhaleException("Invalid service metadata: " + svcMeta.getData()));
			}
			return new DeleteResult(this.req, new SuccessWhaleException("Service metadata missing from message."));
		}
		catch (final SuccessWhaleException e) {
			return new DeleteResult(this.req, e);
		}
		finally {
			p.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final DeleteResult res) {
		if (!res.isSuccess()) {
			LOG.w("Delete failed: %s", res.getE());
			final Notification n = new NotificationCompat.Builder(this.context)
					.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
					.setContentTitle(String.format("Failed to delete via %s.", this.req.getAccount().getUiTitle()))
					.setContentText(res.getEmsg())
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

	public static class DeleteRequest {

		private final Account account;
		private final Tweet tweet;

		public DeleteRequest (final Account account, final Tweet tweet) {
			this.account = account;
			this.tweet = tweet;
		}

		public Account getAccount () {
			return this.account;
		}

		public Tweet getTweet () {
			return this.tweet;
		}

		@Override
		public String toString () {
			return new StringBuilder()
					.append("DeleteRequest{").append(this.account)
					.append(",").append(this.tweet)
					.append("}").toString();
		}

	}

	protected static class DeleteResult {

		private final boolean success;
		private final DeleteRequest request;
		private final Exception e;

		public DeleteResult (final DeleteRequest request) {
			this.success = true;
			this.request = request;
			this.e = null;
		}

		public DeleteResult (final DeleteRequest request, final Exception e) {
			this.success = false;
			this.request = request;
			this.e = e;
		}

		public boolean isSuccess () {
			return this.success;
		}

		public DeleteRequest getRequest () {
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

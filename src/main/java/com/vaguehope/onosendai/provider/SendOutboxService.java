package com.vaguehope.onosendai.provider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import android.content.Intent;
import android.os.AsyncTask;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.provider.DeleteTask.DeleteRequest;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.provider.RtTask.RtRequest;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.exec.ExecUtils;

public class SendOutboxService extends DbBindingService {

	protected static final LogWrapper LOG = new LogWrapper("SS");

	private ExecutorService es;

	public SendOutboxService () {
		super("OnosendaiSendOutboxService", LOG);
	}

	@Override
	public void onCreate () {
		super.onCreate();
		this.es = ExecUtils.newBoundedCachedThreadPool(C.SEND_OUTBOX_MAX_THREADS, LOG);
	}

	@Override
	public void onDestroy () {
		if (this.es != null) this.es.shutdown();
		super.onDestroy();
	}

	@Override
	protected void doWork (final Intent i) {
		if (!waitForDbReady()) return;

		final List<OutboxTweet> entries = getDb().getOutboxEntries(OutboxTweetStatus.PENDING);
		if (entries.size() < 1) {
			LOG.d("No pending tweets to send.");
			return;
		}
		LOG.d("Entries to send: %s...", entries.size());

		Config conf;
		try {
			final Prefs prefs = new Prefs(getBaseContext());
			conf = prefs.asConfig();
		}
		catch (final Exception e) { // No point continuing if any exception.
			LOG.e("Failed to read config.", e); // FIXME replace with something the user will actually see.
			return;
		}

		for (final OutboxTweet ot : entries) {
			final AsyncTask<Void, ?, ? extends SendResult<?>> task;
			switch (ot.getAction()) {
				case POST:
					task = new PostTask(getApplicationContext(), outboxTweetToPostRequest(ot, conf));
					break;
				case RT:
					task = new RtTask(getApplicationContext(), outboxTweetToRtRequest(ot, conf));
					break;
				case DELETE:
					task = new DeleteTask(getApplicationContext(), outboxTweetToDeleteRequest(ot, conf));
					break;
				default:
					throw new IllegalStateException("Do not know how to process action: " + ot.getAction());
			}

			task.executeOnExecutor(this.es);
			OutboxTweet otStat = null;
			try {
				final SendResult<?> res = task.get();
				switch (res.getOutcome()) {
					case SUCCESS:
					case PREVIOUS_ATTEMPT_SUCCEEDED:
						LOG.i("Sent (%s): %s", res.getOutcome(), ot);
						getDb().deleteFromOutbox(ot);
						break;
					case TEMPORARY_FAILURE:
						otStat = ot.tempFailure(res.getEmsg());
						break;
					default:
						otStat = ot.permFailure(res.getEmsg());
				}
			}
			catch (final Exception e) { // NOSONAR report all errors.
				switch (TaskUtils.failureType(e)) {
					case TEMPORARY_FAILURE:
						otStat = ot.tempFailure(e.toString());
						break;
					default:
						otStat = ot.permFailure(e.toString());
				}
			}
			if (otStat != null) {
				LOG.w("Send failed: %s", otStat.getLastError());
				getDb().updateOutboxEntry(otStat);
			}
		}
	}

	private static PostRequest outboxTweetToPostRequest (final OutboxTweet ot, final Config conf) {
		return new PostRequest(conf.getAccount(ot.getAccountId()),
				ot.getSvcMetasParsed(),
				ot.getBody(),
				ot.getInReplyToSid(),
				ot.getAttachment());
	}

	private static RtRequest outboxTweetToRtRequest (final OutboxTweet ot, final Config conf) {
		return new RtRequest(ot.getUid(), conf.getAccount(ot.getAccountId()),
				atMostOne(ot.getSvcMetasParsed()),
				ot.getInReplyToSid());
	}

	private static DeleteRequest outboxTweetToDeleteRequest (final OutboxTweet ot, final Config conf) {
		return new DeleteRequest(ot.getUid(), conf.getAccount(ot.getAccountId()),
				atMostOne(ot.getSvcMetasParsed()),
				ot.getInReplyToSid());
	}

	private static <T> T atMostOne (final Set<T> set) {
		if (set == null || set.size() < 1) return null;
		if (set.size() == 1) return set.iterator().next();
		throw new IllegalStateException("Expected set " + set + " to contain at most one entry.");
	}

}

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
import com.vaguehope.onosendai.model.OutboxTweet.OutboxAction;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.provider.OutboxTask.OtRequest;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;
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

		final Config conf;
		try {
			final Prefs prefs = new Prefs(getBaseContext());
			conf = prefs.asConfig();
		}
		catch (final Exception e) { // No point continuing if any exception.
			LOG.e("Failed to read config.", e); // FIXME replace with something the user will actually see.
			return;
		}

		for (final OutboxTweet ot : entries) {
			final AsyncTask<Void, ?, ? extends SendResult<?>> task = makeTask(conf, ot);
			if (task != null) executeTask(ot, task);
		}
	}

	private AsyncTask<Void, ?, ? extends SendResult<?>> makeTask (final Config conf, final OutboxTweet ot) {
		switch (ot.getAction()) {
			case POST:
				if (OutboxTweet.isTempSid(ot.getInReplyToSid())) {
					return makePostTaskReplacingTempSid(conf, ot);
				}
				else {
					return new PostTask(getApplicationContext(), outboxTweetToPostRequest(ot, conf));
				}
			case RT:
				return new OutboxTask(getApplicationContext(), outboxTweetToOtRequest(OutboxAction.RT, ot, conf));
			case FAV:
				return new OutboxTask(getApplicationContext(), outboxTweetToOtRequest(OutboxAction.FAV, ot, conf));
			case DELETE:
				return new OutboxTask(getApplicationContext(), outboxTweetToOtRequest(OutboxAction.DELETE, ot, conf));
			default:
				throw new IllegalStateException("Do not know how to process action: " + ot.getAction());
		}
	}

	private AsyncTask<Void, ?, ? extends SendResult<?>> makePostTaskReplacingTempSid (final Config conf, final OutboxTweet ot) {
		final long inReplyToObId = OutboxTweet.uidFromTempSid(ot.getInReplyToSid());
		final OutboxTweet inReplyToObEntry = getDb().getOutboxEntry(inReplyToObId);
		if (inReplyToObEntry != null) {
			if (inReplyToObEntry.getStatus() == OutboxTweetStatus.SENT) {
				if (StringHelper.notEmpty(inReplyToObEntry.getSid())) {
					if (inReplyToObEntry.getStatusTime() != null) {
						final long toWait = C.SEND_OUTBOX_THREAD_POST_RATE_LIMIT_MILLIS
								- (System.currentTimeMillis() - inReplyToObEntry.getStatusTime());
						if (toWait > 0) {
							LOG.i("Sleeping %sms to rate limit posting.", toWait);
							try {
								Thread.sleep(toWait);
							}
							catch (InterruptedException e) {/* Ignore. */}
						}
					}
					return new PostTask(getApplicationContext(), outboxTweetToPostRequest(
							ot.withInReplyToSid(inReplyToObEntry.getSid()), conf));
				}
				else {
					LOG.w("Unable to send, inReplyToObEntry missing SID: %s", inReplyToObEntry);
					return null;
				}
			}
			else {
				LOG.d("Unable to send, temp SID not yet sent: %s", ot);
				return null;
			}
		}
		else {
			LOG.w("Unable to send, temp SID not in DB: %s", ot);
			return null;
		}
	}

	private static PostRequest outboxTweetToPostRequest (final OutboxTweet ot, final Config conf) {
		return new PostRequest(conf.getAccount(ot.getAccountId()),
				ot.getSvcMetasParsed(),
				ot.getBody(),
				ot.getInReplyToSid(),
				ot.getAttachment());
	}

	private static OtRequest outboxTweetToOtRequest (final OutboxAction action, final OutboxTweet ot, final Config conf) {
		return new OtRequest(action, ot.getUid(), conf.getAccount(ot.getAccountId()),
				atMostOne(ot.getSvcMetasParsed()),
				ot.getInReplyToSid());
	}

	private static <T> T atMostOne (final Set<T> set) {
		if (set == null || set.size() < 1) return null;
		if (set.size() == 1) return set.iterator().next();
		throw new IllegalStateException("Expected set " + set + " to contain at most one entry.");
	}

	private void executeTask (final OutboxTweet ot, final AsyncTask<Void, ?, ? extends SendResult<?>> task) {
		task.executeOnExecutor(this.es);
		OutboxTweet otStat = null;
		try {
			final SendResult<?> res = task.get();
			switch (res.getOutcome()) {
				case SUCCESS:
				case PREVIOUS_ATTEMPT_SUCCEEDED:
					final String sid = res.getResponse() != null ? res.getResponse().getSid() : null;
					LOG.i("Sent (%s, sid=%s): %s", res.getOutcome(), sid, ot);
					getDb().updateOutboxEntry(ot.markAsSent(sid));
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

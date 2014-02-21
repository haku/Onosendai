package com.vaguehope.onosendai.provider;

import java.util.List;
import java.util.concurrent.ExecutorService;

import android.content.Intent;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.provider.PostTask.PostResult;
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
			final PostTask task = new PostTask(getApplicationContext(), outboxTweetToPostRequest(ot, conf));
			task.executeOnExecutor(this.es);
			OutboxTweet otStat = null;
			try {
				final PostResult res = task.get();
				switch (res.getOutcome()) {
					case SUCCESS:
					case PREVIOUS_ATTEMPT_SUCCEEDED:
						LOG.i("Posted (%s): %s", res.getOutcome(), ot);
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
				LOG.w("Post failed: %s", otStat.getLastError());
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

}

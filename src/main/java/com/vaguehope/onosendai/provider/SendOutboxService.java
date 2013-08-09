package com.vaguehope.onosendai.provider;

import java.util.List;

import android.content.Intent;

import com.vaguehope.onosendai.config.Config;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxTweetStatus;
import com.vaguehope.onosendai.provider.PostTask.PostRequest;
import com.vaguehope.onosendai.provider.PostTask.PostResult;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;

public class SendOutboxService extends DbBindingService {

	protected static final LogWrapper LOG = new LogWrapper("SS");

	public SendOutboxService () {
		super("OnosendaiSendOutboxService", LOG);
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
			task.execute();
			OutboxTweet otStat = null;
			try {
				final PostResult res = task.get();
				if (res.isSuccess()) {
					LOG.i("Posted: %s", ot);
					getDb().deleteFromOutbox(ot);
				}
				else {
					otStat = addFailureType(ot, res.getEmsg(), res.getE());
				}
			}
			catch (final Exception e) { // NOSONAR report all errors.
				otStat = addFailureType(ot, e.toString(), e);
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

	private static OutboxTweet addFailureType(final OutboxTweet ot, final String msg, final Exception e) {
		if (TaskUtils.isFailurePermanent(e)) return ot.permFailure(msg);
		return ot.tempFailure(msg);
	}

}

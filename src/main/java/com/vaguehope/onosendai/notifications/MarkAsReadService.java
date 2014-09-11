package com.vaguehope.onosendai.notifications;

import android.content.Intent;

import com.vaguehope.onosendai.model.ScrollState;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;

public class MarkAsReadService extends DbBindingService {

	private static final LogWrapper LOG = MarkAsReadReceiver.LOG; // Lazy.

	public MarkAsReadService () {
		super("OnosendaiMarkAsReadService", MarkAsReadReceiver.LOG);
	}

	@Override
	protected void doWork (final Intent i) {
		final int colId = i.getIntExtra(MarkAsReadReceiver.EXTRA_COL_ID, -1);
		final long upToTime = i.getLongExtra(MarkAsReadReceiver.EXTRA_UP_TO_TIME, -1);
		if (colId < 0 || upToTime < 0) {
			LOG.e("Invalid mark as read, colId=%s, upToTime=%s.", colId, upToTime);
			return;
		}

		if (!waitForDbReady()) return;

		try {
			// TODO could make this more efficient with a specific DbInterface method.
			final ScrollState s = getDb().getScroll(colId);
			getDb().storeScroll(colId, new ScrollState(s.getItemId(), s.getTop(), s.getItemTime(), upToTime));

			LOG.i("Set col=%s as read upToTime=%s.", colId, upToTime);
			Notifications.clearColumn(this, colId);
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to mark as read.", e);
		}
	}

}

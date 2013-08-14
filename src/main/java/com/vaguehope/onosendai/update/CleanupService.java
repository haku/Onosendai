package com.vaguehope.onosendai.update;

import android.content.Intent;

import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.storage.AttachmentStorage;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.util.LogWrapper;

/*
 * TODO move this class?
 */
public class CleanupService extends DbBindingService {

	protected static final LogWrapper LOG = new LogWrapper("CS");

	public CleanupService () {
		super("OnosendaiCleanupService", LOG);
	}

	@Override
	protected void doWork (final Intent i) {
		try {
			AttachmentStorage.cleanTempOutputDir(this); // FIXME what if attachment in use in Outbox?
			HybridBitmapCache.cleanCacheDir(this);
			if (!waitForDbReady()) return;
			getDb().vacuum();
			LOG.i("Clean up complete.");
		}
		catch (final Exception e) { // NOSONAR want to log all errors.
			LOG.e("Clean up failed.", e);
		}
	}

}

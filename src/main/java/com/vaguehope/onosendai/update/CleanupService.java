package com.vaguehope.onosendai.update;

import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.storage.AttachmentStorage;
import com.vaguehope.onosendai.storage.DbBindingService;
import com.vaguehope.onosendai.storage.DbInterface;
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
		final long start = System.currentTimeMillis();
		try {
			if (!waitForDbReady()) return;
			clean(this, getDb());
			LOG.i("Clean up completed in %sms.", System.currentTimeMillis() - start);
		}
		catch (final Exception e) { // NOSONAR want to log all errors.
			LOG.e("Clean up failed.", e);
		}
	}

	public static void clean (final Context context, final DbInterface db) {
		AttachmentStorage.cleanTempOutputDir(context); // FIXME what if attachment in use in Outbox?
		HybridBitmapCache.cleanCacheDir(context);
		db.housekeep();
	}

}

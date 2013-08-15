package com.vaguehope.onosendai.update;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.images.HybridBitmapCache;
import com.vaguehope.onosendai.storage.AttachmentStorage;
import com.vaguehope.onosendai.util.LogWrapper;

/*
 * TODO move this class?
 */
public class CleanupService extends IntentService {

	protected static final LogWrapper LOG = new LogWrapper("CS");

	public CleanupService () {
		super("OnosendaiCleanupService");
	}

	@Override
	protected final void onHandleIntent (final Intent i) {
		try {
			clean(this);
			LOG.i("Clean up complete.");
		}
		catch (final Exception e) { // NOSONAR want to log all errors.
			LOG.e("Clean up failed.", e);
		}
	}

	public static void clean (final Context context) {
		AttachmentStorage.cleanTempOutputDir(context); // FIXME what if attachment in use in Outbox?
		HybridBitmapCache.cleanCacheDir(context);
	}

}

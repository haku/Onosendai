package com.vaguehope.onosendai.update;

import android.app.IntentService;
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
	protected void onHandleIntent (final Intent i) {
		// FIXME what if attachment in use in Outbox?
		AttachmentStorage.cleanTempOutputDir(this);
		HybridBitmapCache.cleanCacheDir(this);
	}

}

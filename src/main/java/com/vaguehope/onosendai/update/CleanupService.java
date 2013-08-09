package com.vaguehope.onosendai.update;

import android.app.IntentService;
import android.content.Intent;

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
		// TODO what if attachment in use in Outbox?
		AttachmentStorage.cleanTempOutputDir(this);
		// TODO also clean out old cached images.
	}

}

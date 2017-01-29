package com.vaguehope.onosendai.update;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.notifications.NotificationIds;
import com.vaguehope.onosendai.ui.MainActivity;
import com.vaguehope.onosendai.util.FileHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class BatteryNotify extends BroadcastReceiver {

	private static final int OVERRIDE_DURATION_HOURS = 1;
	private static final long OVERRIDE_DURATION_MILLIS = TimeUnit.HOURS.toMillis(OVERRIDE_DURATION_HOURS);
	private static final long GRACE_MILLIS = TimeUnit.MINUTES.toMillis(1L);

	private static final String FILE_PREFIX = "battery-";

	private static final String EXTRA_REQUEST_CODE = "request_code";

	private static final int NOT_UPDATING_NOTIFICATION_ID = NotificationIds.BASE_BATTERY_ID + 1;
	private static final int RC_SHOW_MAIN_ACT = NotificationIds.BASE_BATTERY_ID + 2;
	private static final int RC_PLUS_TIME = NotificationIds.BASE_BATTERY_ID + 3;

	private static final LogWrapper LOG = new LogWrapper("BN");

	private static volatile Long overrideLastModifiedCache;
	private static final Object[] OVERRIDE_LAST_MODIFIED_CACHE_LOCK = new Object[0];

	private static volatile Boolean notifyNotUpdatingCache;
	private static final Object[] NOTIFY_NOT_UPDATING_CACHE_LOCK = new Object[0];

	private static File getOverrideFile (final Context context) {
		return new File(context.getCacheDir(), FILE_PREFIX + "override");
	}

	private static File getUpdateNotifiyFile (final Context context) {
		return new File(context.getCacheDir(), FILE_PREFIX + "update-notify");
	}

	public static boolean isOverrideEnabled (final Context context) {
		Long lastModified;
		synchronized (OVERRIDE_LAST_MODIFIED_CACHE_LOCK) {
			lastModified = overrideLastModifiedCache;
			if (lastModified == null) {
				final File file = getOverrideFile(context);
				if (file.exists()) {
					lastModified = file.lastModified();
				}
				else {
					lastModified = 0L;
				}
				overrideLastModifiedCache = lastModified;
			}
		}
		return (System.currentTimeMillis() - lastModified) <= OVERRIDE_DURATION_MILLIS;
	}

	// Visible for testing.
	static void enableOverride (final Context context) {
		final File file = getOverrideFile(context);
		try {
			synchronized (OVERRIDE_LAST_MODIFIED_CACHE_LOCK) {
				FileHelper.touchFile(file, GRACE_MILLIS);
				overrideLastModifiedCache = null;
			}
			LOG.i("Override enabled.");
		}
		catch (final IOException e) {
			LOG.w("Override marker file error: " + file.getAbsolutePath(), e);
		}
	}

	public static void notifyNotUpdating (final Context context) {
		if (shouldNotifyNotUpdating(context)) showNotification(context);
	}

	// Visible for testing.
	static boolean shouldNotifyNotUpdating (final Context context) {
		synchronized (NOTIFY_NOT_UPDATING_CACHE_LOCK) {
			if (notifyNotUpdatingCache != null && notifyNotUpdatingCache) return false;

			final File file = getUpdateNotifiyFile(context);
			if (file.exists()) {
				notifyNotUpdatingCache = true;
				return false;
			}

			try {
				FileHelper.touchFile(file, GRACE_MILLIS);
				notifyNotUpdatingCache = true;
			}
			catch (final IOException e) {
				LOG.w("NotUpdating marker file error: " + file.getAbsolutePath(), e);
			}
		}
		return true;
	}

	public static void clearNotUpdating (final Context context) {
		synchronized (NOTIFY_NOT_UPDATING_CACHE_LOCK) {
			if (notifyNotUpdatingCache == null || notifyNotUpdatingCache) {
				final File file = getUpdateNotifiyFile(context);
				if (file.exists()) {
					if (file.delete()) {
						notifyNotUpdatingCache = false;
					}
					else {
						notifyNotUpdatingCache = null;
						LOG.w("Failed to rm: " + file.getAbsolutePath());
					}
				}
				else {
					notifyNotUpdatingCache = false;
				}
			}
		}

	}

	private static void showNotification (final Context context) {
		final Builder nb = new NotificationCompat.Builder(context)
				.setOnlyAlertOnce(true)
				.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
				.setContentTitle(context.getString(R.string.background_updating_disabled_notification_title))
				.setContentText(context.getString(R.string.background_updating_disabled_notification_msg))
				.setTicker(context.getString(R.string.background_updating_disabled_notification_title))
				.setAutoCancel(true)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(makeShowMainActPi(context))
				.addAction(android.R.drawable.ic_menu_add,
						String.format("+%s Hour", OVERRIDE_DURATION_HOURS), //ES
						makePlusTimePi(context));
		getManager(context).notify(NOT_UPDATING_NOTIFICATION_ID, nb.build());
	}

	private static NotificationManager getManager (final Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private static PendingIntent makeShowMainActPi (final Context context) {
		return PendingIntent.getActivity(context, RC_SHOW_MAIN_ACT,
				new Intent(context, MainActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private static PendingIntent makePlusTimePi (final Context context) {
		return PendingIntent.getBroadcast(context, RC_PLUS_TIME,
				new Intent(context, BatteryNotify.class)
						.putExtra(EXTRA_REQUEST_CODE, RC_PLUS_TIME),
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public BatteryNotify () {
		super();
	}

	@Override
	public void onReceive (final Context context, final Intent intent) {
		try {
			final Bundle extras = intent.getExtras();
			final int requestCode = extras != null ? extras.getInt(EXTRA_REQUEST_CODE, -1) : -1;
			switch (requestCode) {
				case RC_PLUS_TIME:
					enableOverride(context);
					getManager(context).cancel(NOT_UPDATING_NOTIFICATION_ID);
					break;
				default:
					LOG.w("Intent with unknown request code %s: %s", requestCode, intent);
			}
		}
		catch (final Exception e) { // NOSONAR record all errors.
			LOG.e("Failed enable override.", e);
		}
	}

}

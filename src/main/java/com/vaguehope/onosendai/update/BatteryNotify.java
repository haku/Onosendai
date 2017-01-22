package com.vaguehope.onosendai.update;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.notifications.NotificationIds;
import com.vaguehope.onosendai.util.FileHelper;
import com.vaguehope.onosendai.util.LogWrapper;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

public class BatteryNotify {

	private static final long RENOTIFY_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1L);
	private static final long RENOTIFY_GRACE_MILLIS = TimeUnit.MINUTES.toMillis(1L);
	private static final LogWrapper LOG = new LogWrapper("BN");

	private static File getUpdateMarkerFile (final Context context) {
		return new File(context.getCacheDir(), "update-marker");
	}

	public static void notifyNotUpdating (final Context context) {
		showNotification(context,
				getUpdateMarkerFile(context),
				NotificationIds.NOT_UPDATING_NOTIFICATION_ID,
				R.string.background_updating_disabled_notification_title,
				R.string.background_updating_disabled_notification_msg);
	}

	private static void showNotification (final Context context, final File file, final int nId, final int msg, final int subMsg) {
			try {
				if (FileHelper.fileLastModifiedAgeMillis(file) > RENOTIFY_INTERVAL_MILLIS) {
					showNotification(context, nId, msg, subMsg);
					FileHelper.touchFile(file, RENOTIFY_GRACE_MILLIS);
				}
			}
			catch (final IOException e) {
				LOG.w("Marker file error: " + file.getAbsolutePath(), e);
			}
	}

	private static void showNotification (final Context context, final int nId, final int msg, final int subMsg) {
		final Builder nb = new NotificationCompat.Builder(context)
				.setOnlyAlertOnce(true)
				.setSmallIcon(R.drawable.exclamation_red) // TODO better icon.
				.setContentTitle(context.getString(msg))
				.setContentText(context.getString(subMsg))
				.setTicker(context.getString(msg))
				.setAutoCancel(true)
				.setWhen(System.currentTimeMillis());
		getManager(context).notify(nId, nb.build());
	}

	private static NotificationManager getManager (final Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

}

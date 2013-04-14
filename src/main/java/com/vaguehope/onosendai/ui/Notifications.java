package com.vaguehope.onosendai.ui;

import java.util.Collection;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.Ui;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.storage.DbInterface;

public final class Notifications {

	private static final int BASE_NOTIFICATION_ID = 12000;

	private Notifications () {
		throw new AssertionError();
	}

	public static void update (final Context context, final DbInterface db, final Collection<Column> columns) {
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		for (Column col : columns) {
			final int nId = BASE_NOTIFICATION_ID + col.getId();
			final int count = db.getScrollUpCount(col);
			if (count > 0) {
				final Intent intent = new Intent(context, MainActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
						.putExtra(MainActivity.ARG_FOCUS_COLUMN_ID, col.getId());
				final PendingIntent pendingIntent = PendingIntent.getActivity(context, col.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
				final Notification n = new NotificationCompat.Builder(context)
						.setOnlyAlertOnce(true)
						.setSmallIcon(Ui.notificationIcon())
						.setContentTitle(col.getTitle())
						.setContentText(String.format("%d new updates.", count))
						.setNumber(count)
						.setContentIntent(pendingIntent)
						.setAutoCancel(true)
						.setWhen(System.currentTimeMillis())
						.build();
				nm.notify(nId, n);
			}
			else {
				nm.cancel(nId);
			}
		}
	}

}

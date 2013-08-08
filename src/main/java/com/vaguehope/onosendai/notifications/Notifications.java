package com.vaguehope.onosendai.notifications;

import java.util.Collection;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.MainActivity;

public final class Notifications {

	private static final Random RAND = new Random(System.currentTimeMillis());

	private Notifications () {
		throw new AssertionError();
	}

	public static int notificationIcon () {
		switch (RAND.nextInt(2)) {
			case 0:
				return R.drawable.ic_ho_meji;
			case 1:
			default:
				return R.drawable.ic_saka_meji;
		}
	}

	public static void update (final Context context, final DbInterface db, final Collection<Column> columns) {
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		for (Column col : columns) {
			if (!col.isNotify()) continue;
			final int nId = NotificationIds.BASE_NOTIFICATION_ID + col.getId();
			final int count = db.getUnreadCount(col);
			if (count > 0) {
				final Intent intent = new Intent(context, MainActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
						.putExtra(MainActivity.ARG_FOCUS_COLUMN_ID, col.getId());
				final PendingIntent pendingIntent = PendingIntent.getActivity(context, col.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
				final Notification n = new NotificationCompat.Builder(context)
						.setOnlyAlertOnce(true)
						.setSmallIcon(notificationIcon())
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

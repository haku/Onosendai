package com.vaguehope.onosendai.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.NotificationStyle;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.ui.MainActivity;
import com.vaguehope.onosendai.util.StringHelper;

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
		final NotificationManager nm = getManager(context);
		for (final Column col : columns) {
			if (col.getNotificationStyle() == null) continue;
			updateColumn(context, db, col, nm);
		}
	}

	public static void clearColumn (final Context context, final Column col) {
		getManager(context).cancel(idForColumn(col));
	}

	private static NotificationManager getManager (final Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private static void updateColumn (final Context context, final DbInterface db, final Column col, final NotificationManager nm) {
		final int nId = idForColumn(col);
		final int count = db.getUnreadCount(col);
		if (count > 0) {
			final List<Tweet> tweets = db.getTweets(col.getId(), Math.min(count, 5), col.getExcludeColumnIds());
			// https://stackoverflow.com/questions/14602072/styling-notification-inboxstyle
			final InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			for (final Tweet tweet : tweets) {
				final String username = StringHelper.firstLine(tweet.getUsername());
				final Spannable s = new SpannableString(String.format("%s: %s", username, tweet.getBody()));
				s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				inboxStyle.addLine(s);
			}
			if (tweets.size() < count) {
				inboxStyle.setSummaryText(String.format("+%s more", count - tweets.size()));
			}

			final Intent intent = new Intent(context, MainActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
					.putExtra(MainActivity.ARG_FOCUS_COLUMN_ID, col.getId());
			final PendingIntent pendingIntent = PendingIntent.getActivity(context, col.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
			final Builder nb = new NotificationCompat.Builder(context)
					.setOnlyAlertOnce(true)
					.setSmallIcon(notificationIcon())
					.setContentTitle(col.getTitle())
					.setContentText(String.format("%d new updates.", count))
					.setNumber(count)
					.setContentIntent(pendingIntent)
					.setAutoCancel(true)
					.setWhen(System.currentTimeMillis())
					.setStyle(inboxStyle);
			applyStyle(nb, col.getNotificationStyle());
			nm.notify(nId, nb.build());
		}
		else {
			nm.cancel(nId);
		}
	}

	private static int idForColumn (final Column col) {
		return NotificationIds.BASE_NOTIFICATION_ID + col.getId();
	}

	private static void applyStyle (final Builder nb, final NotificationStyle ns) {
		int defaults = 0;
		if (ns.isLights()) defaults |= Notification.DEFAULT_LIGHTS;
		if (ns.isVibrate()) defaults |= Notification.DEFAULT_VIBRATE;
		if (ns.isSound()) defaults |= Notification.DEFAULT_SOUND;
		nb.setDefaults(defaults);
	}

}

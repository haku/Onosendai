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
import android.support.v4.app.NotificationCompat.Style;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.InlineMediaStyle;
import com.vaguehope.onosendai.config.NotificationStyle;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.storage.DbInterface;
import com.vaguehope.onosendai.storage.DbInterface.Selection;
import com.vaguehope.onosendai.storage.SaveScrollNow;
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
		SaveScrollNow.requestAndWaitForUiToSaveScroll(db);

		final NotificationManager nm = getManager(context);
		for (final Column col : columns) {
			if (col.getNotificationStyle() == null) continue;
			updateColumn(context, db, col, nm);
		}
	}

	public static void clearColumn (final Context context, final Column col) {
		clearColumn(context, col.getId());
	}

	public static void clearColumn (final Context context, final int colId) {
		getManager(context).cancel(idForColumn(colId));
	}

	private static NotificationManager getManager (final Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	private static void updateColumn (final Context context, final DbInterface db, final Column col, final NotificationManager nm) {
		final int nId = idForColumn(col);
		final int count = db.getUnreadCount(col);
		if (count > 0) {
			final Intent showMainActI = new Intent(context, MainActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
					.putExtra(MainActivity.ARG_FOCUS_COLUMN_ID, col.getId());
			final PendingIntent showMainActPi = PendingIntent.getActivity(context, col.getId(), showMainActI, PendingIntent.FLAG_CANCEL_CURRENT);

			final List<Tweet> tweets = db.getTweets(col.getId(), Math.min(count, 5),
					Selection.FILTERED, col.getExcludeColumnIds(),
					col.getInlineMediaStyle() == InlineMediaStyle.SEAMLESS,
					col.getNotificationStyle().isExcludeRetweets(),
					true);

			final String msg = makeMsg(col, tweets, count);
			final Style style = makePreview(tweets, count);
			final PendingIntent markAsReadPi = MarkAsReadReceiver.makePi(context, col, tweets);

			final Builder nb = new NotificationCompat.Builder(context)
					.setOnlyAlertOnce(true)
					.setSmallIcon(notificationIcon())
					.setContentTitle(col.getTitle())
					.setContentText(msg)
					.setTicker(msg)
					.setNumber(count)
					.setContentIntent(showMainActPi)
					.setAutoCancel(true)
					.setWhen(System.currentTimeMillis())
					.setStyle(style);
			if (markAsReadPi != null) nb.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Mark as read", markAsReadPi); //ES
			applyStyle(nb, col.getNotificationStyle());
			nm.notify(nId, nb.build());
		}
		else {
			nm.cancel(nId);
		}
	}

	private static int idForColumn (final Column col) {
		return idForColumn(col.getId());
	}

	private static int idForColumn (final int colId) {
		return NotificationIds.BASE_NOTIFICATION_ID + colId;
	}

	private static void applyStyle (final Builder nb, final NotificationStyle ns) {
		int defaults = 0;
		if (ns.isLights()) defaults |= Notification.DEFAULT_LIGHTS;
		if (ns.isVibrate()) defaults |= Notification.DEFAULT_VIBRATE;
		if (ns.isSound()) defaults |= Notification.DEFAULT_SOUND;
		nb.setDefaults(defaults);
	}

	private static String makeMsg (final Column col, final List<Tweet> tweets, final int count) {
		if (count == 1 && tweets != null && tweets.size() == 1) {
			final Tweet t = tweets.get(0);
			return String.format("%s: %s", readName(t), t.getBody());
		}
		return String.format("%s: %s new updates.", col.getTitle(), count); //ES
	}

	// https://stackoverflow.com/questions/14602072/styling-notification-inboxstyle
	private static Style makePreview (final List<Tweet> tweets, final int count) {
		if (tweets == null || tweets.size() < 1) return null;

		if (tweets.size() == 1) return new NotificationCompat.BigTextStyle()
				.bigText(tweetToSpanable(tweets.iterator().next()));

		final InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		for (final Tweet tweet : tweets) {
			inboxStyle.addLine(tweetToSpanable(tweet));
		}
		if (tweets.size() < count) {
			inboxStyle.setSummaryText(String.format("+%s more", count - tweets.size()));
		}
		return inboxStyle;
	}

	private static Spannable tweetToSpanable (final Tweet tweet) {
		final String name = readName(tweet);
		final Spannable s = new SpannableString(String.format("%s: %s", name, tweet.getBody()));
		s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return s;
	}

	private static String readName (final Tweet tweet) {
		return !StringHelper.isEmpty(tweet.getUsername())
				? StringHelper.firstLine(tweet.getUsername())
				: StringHelper.firstLine(tweet.getFullname());
	}

}

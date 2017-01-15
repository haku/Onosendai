package com.vaguehope.onosendai.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.Context;

public final class DateHelper {

	protected static final long ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1);

	private DateHelper () {
		throw new AssertionError();
	}

	public static DateFormat standardDateTimeFormat () {
		return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH);
	}

	public static String formatDateTime (final Context context, final long timeMillis) {
		final Date date = new Date(timeMillis);
		return String.format("%s %s",
				android.text.format.DateFormat.getDateFormat(context).format(date),
				android.text.format.DateFormat.getTimeFormat(context).format(date));
	}

	public static class FriendlyDateTimeFormat {
		private final DateFormat dateFormat;
		private final DateFormat timeFormat;

		public FriendlyDateTimeFormat (final Context context) {
			this.dateFormat = android.text.format.DateFormat.getDateFormat(context);
			this.timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		}

		public String format (final long now, final long time) {
			final Date date = new Date(time);
			final String sT = this.timeFormat.format(date);
			if (now - time < ONE_DAY_MILLIS) return sT;
			return String.format("%s %s", this.dateFormat.format(date), sT);
		}
	}

	public static String formatDurationSeconds (final long seconds) {
		if (seconds >= 3600) {
			return String.format(Locale.ENGLISH, "%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
		}
		return String.format(Locale.ENGLISH, "%d:%02d", (seconds % 3600) / 60, (seconds % 60));
	}

	public static String formatDurationMillis (final long millis) {
		return formatDurationSeconds(TimeUnit.MILLISECONDS.toSeconds(millis));
	}

}

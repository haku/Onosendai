package com.vaguehope.onosendai.notifications;

import java.util.List;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.LogWrapper;

public class MarkAsReadReceiver extends BroadcastReceiver {

	protected static final String EXTRA_COL_ID = "col_id";
	protected static final String EXTRA_UP_TO_TIME = "up_to_time";

	protected static final LogWrapper LOG = new LogWrapper("MAR");

	public static PendingIntent makePi (final Context context, final Column col, final List<Tweet> tweets) {
		if (tweets == null || tweets.size() < 1) return null;
		return PendingIntent.getBroadcast(context, col.getId(),
				new Intent(context, MarkAsReadReceiver.class)
						.putExtra(EXTRA_COL_ID, col.getId())
						.putExtra(EXTRA_UP_TO_TIME, tweets.iterator().next().getTime()),
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public void onReceive (final Context context, final Intent intent) {
		context.startService(new Intent(context, MarkAsReadService.class).putExtras(intent.getExtras()));
	}

}

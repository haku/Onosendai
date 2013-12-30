package com.vaguehope.onosendai.update;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

import com.vaguehope.onosendai.C;
import com.vaguehope.onosendai.provider.SendOutboxService;
import com.vaguehope.onosendai.util.BatteryHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class AlarmReceiver extends BroadcastReceiver {

	private static final int TEMP_WAKELOCK_TIMEOUT_MILLIS = 3000;

	private static final int BASE_ALARM_ID = 11000;
	private static final String KEY_ACTION = "action";
	private static final int ACTION_UPDATE = 1;
	private static final int ACTION_CLEANUP = 2;

	private static final LogWrapper LOG = new LogWrapper("AR");

	@Override
	public void onReceive (final Context context, final Intent intent) {
		final int action = intent.getExtras().getInt(KEY_ACTION, -1);
		final float bl = BatteryHelper.level(context.getApplicationContext());
		LOG.i("AlarmReceiver invoked: action=%s bl=%s.", action, bl);
		switch (action) {
			case ACTION_UPDATE:
				final boolean doUpdate = (bl > C.MIN_BAT_UPDATE);
				final boolean doSend = (bl > C.MIN_BAT_SEND);
				if (doUpdate || doSend) {
					final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
					final WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
					wl.acquire(TEMP_WAKELOCK_TIMEOUT_MILLIS);
				}
				if (doUpdate) context.startService(new Intent(context, UpdateService.class));
				if (doSend) context.startService(new Intent(context, SendOutboxService.class));
				break;
			case ACTION_CLEANUP:
				if (bl > C.MIN_BAT_CLEANUP) context.startService(new Intent(context, CleanupService.class));
				break;
			default:
				LOG.e("Unknown action: '%s'.", action);
				break;
		}
	}

	public static void configureAlarms (final Context context) {
		final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		scheduleUpdates(context, am);
		scheduleCleanups(context, am);
		LOG.i("Alarm service configured.");
	}

	private static void scheduleUpdates (final Context context, final AlarmManager am) {
		final PendingIntent i = PendingIntent.getBroadcast(context, BASE_ALARM_ID + ACTION_UPDATE,
				new Intent(context, AlarmReceiver.class).putExtra(KEY_ACTION, ACTION_UPDATE),
				PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(i);
		am.setInexactRepeating(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime(),
				AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				i);
	}

	private static void scheduleCleanups (final Context context, final AlarmManager am) {
		final PendingIntent i = PendingIntent.getBroadcast(context, BASE_ALARM_ID + ACTION_CLEANUP,
				new Intent(context, AlarmReceiver.class).putExtra(KEY_ACTION, ACTION_CLEANUP),
				PendingIntent.FLAG_CANCEL_CURRENT);
		am.cancel(i);
		am.setInexactRepeating(
				AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime(),
				AlarmManager.INTERVAL_DAY,
				i);
	}

}

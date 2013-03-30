package com.vaguehope.onosendai.update;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;

import com.vaguehope.onosendai.C;

public class AlarmReceiver extends BroadcastReceiver {

	private static final int TEMP_WAKELOCK_TIMEOUT_MILLIS = 3000;

	@Override
	public void onReceive (final Context context, final Intent intent) {
		Log.i(C.TAG, "AlarmReceiver invoked.");

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
		wl.acquire(TEMP_WAKELOCK_TIMEOUT_MILLIS);

		context.startService(new Intent(context, UpdateService.class));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void configureAlarm (final Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent i = getPendingIntent(context);
		am.cancel(i);
		am.setInexactRepeating(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime(),
				AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				i);
		Log.i(C.TAG, "Alarm service configured.");
	}

	private static PendingIntent getPendingIntent (final Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
	}

}

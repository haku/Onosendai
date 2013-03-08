package com.vaguehope.onosendai.update;

import java.util.concurrent.TimeUnit;

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

	@Override
	public void onReceive (Context context, Intent intent) {
		Log.i(C.TAG, "AlarmReceiver invoked.");

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
		wl.acquire(3000);

		context.startService(new Intent(context, UpdateService.class));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void configureAlarm (Context context) {
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent i = getPendingIntent(context);
		am.cancel(i);
		am.setInexactRepeating(
				AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(300), // ~ 5 min before first run.
				AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				i);
		Log.i(C.TAG, "Alarm service configured.");
	}

	private static PendingIntent getPendingIntent (Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), 0);
	}

}

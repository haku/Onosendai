package com.vaguehope.onosendai.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public final class BatteryHelper {

	private BatteryHelper () {
		throw new AssertionError();
	}

	/**
	 * return between 0 and 1.
	 */
	public static float level (final Context context) {
		final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		final Intent batteryStatus = context.registerReceiver(null, ifilter);
		final int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		final int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return level / (float)scale;
	}

}

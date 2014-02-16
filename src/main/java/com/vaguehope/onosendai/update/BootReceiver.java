package com.vaguehope.onosendai.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vaguehope.onosendai.C;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive (final Context context, final Intent intent) {
		Log.i(C.TAG, "BootReceiver invoked.");
		AlarmReceiver.configureAlarms(context);
	}

}

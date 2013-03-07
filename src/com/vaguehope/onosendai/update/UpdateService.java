package com.vaguehope.onosendai.update;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.vaguehope.onosendai.C;

public class UpdateService extends IntentService {

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public UpdateService () {
		super("OnosendaiUpdateService");
	}

	@Override
	protected void onHandleIntent (Intent i) {
		Log.i(C.TAG, "UpdateService invoked.");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
		wl.acquire();
		try {
			if (connectionPresent()) {
				fetchTweets();
			}
			else {
				Log.i(C.TAG, "No connection, aborted.");
			}
		}
		finally {
			wl.release();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void fetchTweets () {
		// TODO
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private boolean connectionPresent () {
		ConnectivityManager cMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
		if ((netInfo != null) && (netInfo.getState() != null)) {
			return netInfo.getState().equals(State.CONNECTED);
		}
		return false;
	}

}

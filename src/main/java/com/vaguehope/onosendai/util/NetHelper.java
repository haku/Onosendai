package com.vaguehope.onosendai.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

public final class NetHelper {

	private NetHelper () {
		throw new AssertionError();
	}

	public static boolean connectionPresent (final Context context) {
		final ConnectivityManager cMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
		if ((netInfo != null) && (netInfo.getState() != null)) {
			return netInfo.getState().equals(State.CONNECTED);
		}
		return false;
	}

}

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
		final ConnectivityManager cMgr = getConnectivityManager(context);
		if (cMgr == null) return false;

		final NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
		if (netInfo == null) return false;

		final State state = netInfo.getState();
		if (state == null) return false;

		return state.equals(State.CONNECTED);
	}

	public static boolean isWifi (final Context context) {
		final ConnectivityManager cMgr = getConnectivityManager(context);
		if (cMgr == null) return false;

		final NetworkInfo wifi = cMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifi == null) return false;

		return wifi.isConnected();
	}

	private static ConnectivityManager getConnectivityManager (final Context context) {
		return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

}

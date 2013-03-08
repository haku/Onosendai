package com.vaguehope.onosendai.util;

import android.util.Log;

import com.vaguehope.onosendai.C;

public class LogWrapper {

	private String prefix;

	public LogWrapper () {}

	public LogWrapper (final String prefix) {
		this.prefix = prefix;
	}

	public void setPrefix (final String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix () {
		return this.prefix;
	}

	public void i (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.INFO)) return;
		Log.i(C.TAG, this.prefix == null ? msg : String.format("%s %s", this.prefix, msg));
	}

	public void i (final String msg, final Object... args) {
		if (!Log.isLoggable(C.TAG, Log.INFO)) return;
		Log.i(C.TAG, this.prefix == null ? msg : String.format("%s %s", this.prefix, String.format(msg, args)));
	}

	public void w (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.WARN)) return;
		Log.w(C.TAG, this.prefix == null ? msg : String.format("%s %s", this.prefix, msg));
	}

	public void e (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.ERROR)) return;
		Log.e(C.TAG, this.prefix == null ? msg : String.format("%s %s", this.prefix, msg));
	}

	public void e (final String msg, final Throwable t) {
		if (!Log.isLoggable(C.TAG, Log.ERROR)) return;
		Log.e(C.TAG, this.prefix == null ? msg : String.format("%s %s", this.prefix, msg), t);
	}

}

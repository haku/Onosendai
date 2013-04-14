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

	public void wtf (final String msg, final Throwable t) {
		Log.wtf(C.TAG, addPrefix(msg), t);
	}

	public void e (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.ERROR)) return;
		Log.e(C.TAG, addPrefix(msg));
	}

	public void e (final String msg, final Throwable t) {
		if (!Log.isLoggable(C.TAG, Log.ERROR)) return;
		Log.e(C.TAG, addPrefix(msg), t);
	}

	public void e (final String msg, final Object... args) {
		if (!Log.isLoggable(C.TAG, Log.ERROR)) return;
		Log.e(C.TAG, addPrefix(String.format(msg, args)));
	}

	public void w (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.WARN)) return;
		Log.w(C.TAG, addPrefix(msg));
	}

	public void w (final String msg, final Throwable t) {
		if (!Log.isLoggable(C.TAG, Log.WARN)) return;
		Log.w(C.TAG, addPrefix(msg), t);
	}

	public void w (final String msg, final Object... args) {
		if (!Log.isLoggable(C.TAG, Log.WARN)) return;
		Log.w(C.TAG, addPrefix(String.format(msg, args)));
	}

	public void i (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.INFO)) return;
		Log.i(C.TAG, addPrefix(msg));
	}

	public void i (final String msg, final Object... args) {
		if (!Log.isLoggable(C.TAG, Log.INFO)) return;
		Log.i(C.TAG, addPrefix(String.format(msg, args)));
	}

	public void d (final String msg) {
		if (!Log.isLoggable(C.TAG, Log.DEBUG)) return;
		Log.d(C.TAG, addPrefix(msg));
	}

	public void d (final String msg, final Object... args) {
		if (!Log.isLoggable(C.TAG, Log.DEBUG)) return;
		Log.d(C.TAG, addPrefix(String.format(msg, args)));
	}

	private String addPrefix (final String msg) {
		if (this.prefix == null) return msg;
		return String.format("%s %s", this.prefix, msg);
	}

}

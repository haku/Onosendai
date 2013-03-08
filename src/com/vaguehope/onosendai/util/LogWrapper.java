package com.vaguehope.onosendai.util;

import android.util.Log;

import com.vaguehope.onosendai.C;

public class LogWrapper {

	private String prefix;

	public LogWrapper () {
	}

	public void setPrefix (final String prefix) {
		this.prefix = prefix;
	}

	public void i(final String msg) {
		Log.i(C.TAG, this.prefix == null ? msg : String.format("%s %s", this.prefix, msg));
	}

}

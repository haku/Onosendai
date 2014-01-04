package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.net.UnknownHostException;

import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.StringHelper;


public class SuccessWhaleException extends Exception {

	private static final long serialVersionUID = -2004908955108746560L;

	private final boolean permanent;

	public SuccessWhaleException (final String msg) {
		this(msg, false);
	}

	public SuccessWhaleException (final String msg, final boolean permanent) {
		super(msg);
		this.permanent = permanent;
	}

	public SuccessWhaleException (final String msg, final Exception e) {
		this(msg, e, false);
	}

	public SuccessWhaleException (final String msg, final Exception e, final boolean permanent) {
		super(msg, e);
		this.permanent = permanent;
	}

	public boolean isPermanent () {
		return this.permanent;
	}

	public String friendlyMessage () {
		final Throwable cause = getCause();
		if (cause != null) {
			if (cause instanceof UnknownHostException) {
				return "Network error: " + cause.getMessage();
			}
			else if (cause instanceof IOException && StringHelper.safeContainsIgnoreCase(cause.getMessage(), "connection timed out")) {
				return "Network error: Connection timed out.";
			}
			else if (cause instanceof IOException) {
				return "Network error: " + String.valueOf(cause);
			}
		}
		return ExcpetionHelper.causeTrace(this);
	}

}

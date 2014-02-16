package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;

import android.net.http.AndroidHttpClient;

import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.StringHelper;


public class SuccessWhaleException extends IOException {

	private static final long serialVersionUID = -2004908955108746560L;

	private final boolean permanent;

	public SuccessWhaleException (final HttpResponse response) throws IOException {
		this(String.format("HTTP %s %s: %s",
				response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(),
				IoHelper.toString(AndroidHttpClient.getUngzippedContent(response.getEntity()))
				));
	}

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
		if (cause == null) {
			// In theory we should parse the XML properly for this, but this will do for now.
			if (getMessage().contains("Koala::Facebook::AuthenticationError")) {
				return "SuccessWhale error: Please use web UI to authorise access to Facebook.";
			}
		}
		else {
			if (cause instanceof UnknownHostException) {
				return "Network error: " + cause.getMessage();
			}
			else if (cause instanceof IOException && StringHelper.safeContainsIgnoreCase(cause.getMessage(), "connection timed out")) {
				return "Network error: Connection timed out.";
			}
			else if (cause instanceof IOException) {
				return "Network error: " + cause;
			}
		}
		return ExcpetionHelper.causeTrace(this);
	}

}

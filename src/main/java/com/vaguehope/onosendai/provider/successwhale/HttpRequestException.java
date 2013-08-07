package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;

import org.apache.http.HttpResponse;

import android.net.http.AndroidHttpClient;

import com.vaguehope.onosendai.util.IoHelper;

class HttpRequestException extends IOException {

	private static final long serialVersionUID = 3920538685970298544L;

	public HttpRequestException (final HttpResponse response) throws IOException {
		super(String.format("HTTP %s %s: %s",
				response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(),
				IoHelper.toString(AndroidHttpClient.getUngzippedContent(response.getEntity()))
				));
	}

}

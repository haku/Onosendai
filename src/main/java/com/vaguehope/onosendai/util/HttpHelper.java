/*
 * Copyright 2013 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpResponseException;

public final class HttpHelper {

	public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	public static final int HTTP_READ_TIMEOUT_SECONDS = 60;

	private static final int MAX_REDIRECTS = 3;

	public interface HttpStreamHandler<R, T extends Exception> {

		R handleStream (InputStream is, int contentLength) throws IOException, T;

	}

	private HttpHelper () {
		throw new AssertionError();
	}

	public static <R, T extends Exception> R get (final String sUrl, final HttpStreamHandler<R, T> streamHandler) throws IOException, T { // NOSONAR Not redundant throws.
		return get(sUrl, streamHandler, 0);
	}

	private static <R, T extends Exception> R get (final String sUrl, final HttpStreamHandler<R, T> streamHandler, final int redirectCount) throws IOException, T { // NOSONAR Not redundant throws.
		final URL url = new URL(sUrl);
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try {
			connection.setRequestMethod("GET");
			connection.setInstanceFollowRedirects(true);
			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_CONNECT_TIMEOUT_SECONDS));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_READ_TIMEOUT_SECONDS));

			InputStream is = null;
			try {
				final int responseCode = connection.getResponseCode();

				// For some reason some devices do not follow redirects. :(
				if (responseCode == 301 || responseCode == 302 || responseCode == 307) { // NOSONAR not magic numbers.  Its HTTP spec.
					if (redirectCount >= MAX_REDIRECTS) throw new HttpResponseException(responseCode, "Max redirects of " + MAX_REDIRECTS + " exceeded.");
					final String location = connection.getHeaderField("Location");
					if (location == null) throw new HttpResponseException(responseCode, "Location header missing.  Headers present: "
							+ connection.getHeaderFields() + ".");
					return get(location, streamHandler, redirectCount + 1);
				}

				if (responseCode < 200 || responseCode >= 300) { // NOSONAR not magic numbers.  Its HTTP spec.
					throw new HttpResponseException(responseCode, IoHelper.toString(connection.getErrorStream()));
				}

				is = connection.getInputStream();
				return streamHandler.handleStream(is, connection.getContentLength());
			}
			finally {
				IoHelper.closeQuietly(is);
			}
		}
		finally {
			connection.disconnect();
		}
	}

}

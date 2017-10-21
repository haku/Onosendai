/*
 * Copyright 2013 Fae Hutter
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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpResponseException;

public final class HttpHelper {

	private static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	private static final int HTTP_READ_TIMEOUT_SECONDS = 60;
	private static final int MAX_REDIRECTS = 5;
	private static final int MAX_ERR_BODY_LENGTH_CHAR = 100;

	private static final int HTTP_NOT_FOUND = 404;

	private static final LogWrapper LOG = new LogWrapper("HH");

	public enum Method {
		GET,
		HEAD;
	}

	public interface HttpStreamHandler<R> {

		/**
		 * Does not need to do anything, will still be thrown.
		 */
		void onError (Exception e);

		R handleStream (URLConnection connection, InputStream is, int contentLength) throws IOException;

	}

	public static class FinalUrlHandler<R> implements HttpStreamHandler<R> {

		private final HttpStreamHandler<R> delagate;
		private URL url;
		private int contentLength;

		public FinalUrlHandler (final HttpStreamHandler<R> delagate) {
			this.delagate = delagate;
		}

		public URL getUrl () {
			return this.url;
		}

		public int getContentLength () {
			return this.contentLength;
		}

		@Override
		public void onError (final Exception e) {
			this.delagate.onError(e);
		}

		@Override
		public R handleStream (final URLConnection connection, final InputStream is, final int contentLength) throws IOException {
			this.url = connection.getURL();
			this.contentLength = contentLength;
			return this.delagate.handleStream(connection, is, contentLength);
		}

	}

	private HttpHelper () {
		throw new AssertionError();
	}

	public static <R> R fetch (final Method method, final String sUrl, final HttpStreamHandler<R> streamHandler) throws IOException {
		try {
			return fetchWithFollowRedirects(method, new URL(sUrl), streamHandler, 0);
		}
		catch (final Exception e) { // NOSONAR need to report failures to onError().
			streamHandler.onError(e);
			if (e instanceof RuntimeException) throw (RuntimeException) e;
			if (e instanceof IOException) throw (IOException) e;
			throw new IllegalStateException(e);
		}
	}

	private static <R> R fetchWithFollowRedirects (final Method method, final URL url, final HttpStreamHandler<R> streamHandler, final int redirectCount) throws IOException, URISyntaxException {
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		try {
			connection.setRequestMethod(method.toString());
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_CONNECT_TIMEOUT_SECONDS));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_READ_TIMEOUT_SECONDS));
			connection.setRequestProperty("User-Agent", "curl/1"); // Make it really clear this is not a browser.
			//connection.setRequestProperty("Accept-Encoding", "identity"); This fixes missing Content-Length headers but feels wrong.
			connection.connect();

			InputStream is = null;
			try {
				final int responseCode = connection.getResponseCode();

				// For some reason some devices do not follow redirects. :(
				if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307) { // NOSONAR not magic numbers.  Its HTTP spec.
					if (redirectCount >= MAX_REDIRECTS) throw new TooManyRedirectsException(responseCode, url, MAX_REDIRECTS);
					final String locationHeader = connection.getHeaderField("Location");
					if (locationHeader == null) throw new HttpResponseException(responseCode, "Location header missing.  Headers present: "
							+ connection.getHeaderFields() + ".");
					connection.disconnect();

					final URL locationUrl;
					if (locationHeader.toLowerCase(Locale.ENGLISH).startsWith("http")) {
						locationUrl = new URL(locationHeader);
					}
					else {
						locationUrl = url.toURI().resolve(locationHeader).toURL();
					}
					return fetchWithFollowRedirects(method, locationUrl, streamHandler, redirectCount + 1);
				}

				if (responseCode < 200 || responseCode >= 300) { // NOSONAR not magic numbers.  Its HTTP spec.
					throw new NotOkResponseException(responseCode, connection, url);
				}

				is = connection.getInputStream();
				final int contentLength = connection.getContentLength();
				if (contentLength < 1) LOG.w("Content-Length=%s for %s.", contentLength, url);
				return streamHandler.handleStream(connection, is, contentLength);
			}
			finally {
				IoHelper.closeQuietly(is);
			}
		}
		finally {
			connection.disconnect();
		}
	}

	private static String summariseHttpErrorResponse (final HttpURLConnection connection) throws IOException {
		final int responseCode = connection.getResponseCode();
		if (responseCode == HTTP_NOT_FOUND) return String.format("HTTP %s %s", responseCode, connection.getResponseMessage());
		return String.format("HTTP %s %s: %s",
				responseCode, connection.getResponseMessage(),
				IoHelper.toString(connection.getErrorStream(), MAX_ERR_BODY_LENGTH_CHAR));
	}

	public abstract static class FinalUrlException extends HttpResponseException {

		private static final long serialVersionUID = -5533900604169920207L;

		private final URL lastUrl;

		public FinalUrlException (final String message, final int responseCode, final URL lastUrl) {
			super(responseCode, message);
			this.lastUrl = lastUrl;
		}

		public URL getLastUrl () {
			return this.lastUrl;
		}

	}

	public static class TooManyRedirectsException extends FinalUrlException {

		private static final long serialVersionUID = -7981953690474511370L;

		public TooManyRedirectsException (final int responseCode, final URL lastUrl, final int redirectCount) {
			super("Max redirects of " + redirectCount + " exceeded.",
					responseCode, lastUrl);
		}

	}

	public static class NotOkResponseException extends FinalUrlException {

		private static final long serialVersionUID = 2450727724111193765L;

		public NotOkResponseException (final int responseCode, final HttpURLConnection connection, final URL lastUrl) throws IOException {
			super(summariseHttpErrorResponse(connection),
					responseCode, lastUrl);
		}

	}

}

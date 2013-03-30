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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpResponseException;

public final class HttpHelper {

	public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	public static final int HTTP_READ_TIMEOUT_SECONDS = 60;

	public interface HttpStreamHandler<R, T extends Exception> {

		R handleStream (InputStream is, int contentLength) throws IOException, T;

	}

	private HttpHelper () {
		throw new AssertionError();
	}

	public static <R, T extends Exception> R get (final String sUrl, final HttpStreamHandler<R, T> streamHandler) throws IOException, T { // NOSONAR Not redundant throws.
		URL url = new URL(sUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_CONNECT_TIMEOUT_SECONDS));
		connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(HTTP_READ_TIMEOUT_SECONDS));

		InputStream is = null;
		try {
			int responseCode = connection.getResponseCode();
			if (responseCode >= 400) {
				throw new HttpResponseException(responseCode, streamToString(connection.getErrorStream()));
			}

			is = connection.getInputStream();
			return streamHandler.handleStream(is, connection.getContentLength());
		}
		finally {
			if (is != null) is.close();
		}
	}

	private static String streamToString (final InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		try {
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString();
		}
		finally {
			is.close();
		}
	}

}

/*
 * Copyright 2010 Fae Hutter
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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.HttpResponseException;

import android.util.Base64;

public class HttpHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final int HTTP_CONNECT_TIMEOUT_SECONDS = 20;
	public static final int HTTP_READ_TIMEOUT_SECONDS = 60;

	private static final String HEADER_AUTHORISATION = "Authorization";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public interface HttpStreamHandler<T extends Exception> {

		void handleStream (InputStream is, int contentLength) throws IOException, T;

	}

	public interface HttpCreds {

		String getUser ();

		String getPass ();

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getUrlContent (final String sUrl, final String httpRequestMethod, final String encodedData) throws IOException {
		return getUrlContent(sUrl, httpRequestMethod, encodedData, null, (HttpStreamHandler<RuntimeException>) null, null);
	}

	public static String getUrlContent (final String sUrl, final String httpRequestMethod, final String encodedData, final String contentType, final HttpCreds creds) throws IOException {
		return getUrlContent(sUrl, httpRequestMethod, encodedData, contentType, (HttpStreamHandler<RuntimeException>) null, creds);
	}

	public static <T extends Exception> String getUrlContent (final String sUrl, final HttpStreamHandler<T> streamHandler, final HttpCreds creds) throws IOException, T {
		return getUrlContent(sUrl, null, null, null, streamHandler, creds);
	}

	public static <T extends Exception> String getUrlContent (
			final String sUrl,
			final String httpRequestMethod,
			final String encodedData,
			final String contentType,
			final HttpStreamHandler<T> streamHandler,
			final HttpCreds creds
			) throws IOException, T {
		URL url = new URL(sUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setUseCaches(false);
		connection.setRequestMethod(httpRequestMethod != null ? httpRequestMethod : "GET");
		connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS * 1000);
		connection.setReadTimeout(HTTP_READ_TIMEOUT_SECONDS * 1000);
		if (creds != null) connection.setRequestProperty(HEADER_AUTHORISATION, authHeader(creds));

		if (encodedData != null) {
			if (contentType != null) connection.setRequestProperty("Content-Type", contentType);
			connection.setDoOutput(true);
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			try {
				out.write(encodedData);
				out.flush();
			}
			finally {
				out.close();
			}
		}

		StringBuilder sb = null;
		InputStream is = null;
		try {
			int responseCode = connection.getResponseCode();
			if (responseCode >= 400) {
				sb = new StringBuilder();
				buildString(connection.getErrorStream(), sb);
				throw new HttpResponseException(responseCode, sb.toString());
			}

			is = connection.getInputStream();

			if (streamHandler != null) {
				streamHandler.handleStream(is, connection.getContentLength());
			}
			else {
				sb = new StringBuilder();
				buildString(is, sb);
			}
		}
		finally {
			if (is != null) is.close();
		}

		return sb == null ? null : sb.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String authHeader (final HttpCreds creds) {
		String raw = creds.getUser() + ":" + creds.getPass();
		String enc = Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP);
		return "Basic " + enc;
	}

	public static void buildString (final InputStream is, final StringBuilder sb) throws IOException {
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}

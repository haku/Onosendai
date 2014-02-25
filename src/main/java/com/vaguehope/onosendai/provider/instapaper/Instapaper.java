package com.vaguehope.onosendai.provider.instapaper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.util.Base64;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.HttpClientFactory;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://www.instapaper.com/api
 */
public class Instapaper {

	private static final String BASE_URL = "https://www.instapaper.com:443";
	private static final String API_TEST_AUTH = "/api/authenticate";
	private static final String API_ADD = "/api/add";

	static final LogWrapper LOG = new LogWrapper("IP");

	private final Account account;
	private final HttpClientFactory httpClientFactory;

	public Instapaper (final Account account, final HttpClientFactory httpClientFactory) {
		if (account == null) throw new IllegalArgumentException("account can not be null.");
		if (httpClientFactory == null) throw new IllegalArgumentException("httpClientFactory can not be null.");
		this.account = account;
		this.httpClientFactory = httpClientFactory;
	}

	protected Account getAccount () {
		return this.account;
	}

	protected HttpClientFactory getHttpClientFactory () {
		return this.httpClientFactory;
	}

	protected HttpClient getHttpClient () throws IOException {
		return this.httpClientFactory.getHttpClient();
	}

	public void testLogin () throws IOException {
		final HttpPost post = new HttpPost(BASE_URL + API_TEST_AUTH);
		addAuth(post);
		getHttpClient().execute(post, new CheckStatusOnlyHandler());
	}

	public void add (final String url, final String title, final String body) throws IOException {
		final HttpPost post = new HttpPost(BASE_URL + API_ADD);
		addAuth(post);
		final List<NameValuePair> params = new ArrayList<NameValuePair>(3);
		params.add(new BasicNameValuePair("url", url));
		if (title != null) params.add(new BasicNameValuePair("title", title));
		params.add(new BasicNameValuePair("selection", body));
		post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		getHttpClient().execute(post, new CheckStatusOnlyHandler());
	}

	private void addAuth (final HttpPost post) {
		try {
			post.setHeader("Authorization", "Basic " + Base64.encodeToString(
					(this.account.getAccessToken() + ":" + this.account.getAccessSecret()).getBytes("UTF-8"),
					Base64.NO_WRAP));
		}
		catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	static void checkReponseCode (final StatusLine statusLine) throws IOException {
		final int code = statusLine.getStatusCode();
		if (code < 200 || code >= 300) { // NOSONAR not magic numbers.
			throw new IOException("HTTP " + code + ": " + statusLine.getReasonPhrase());
		}
	}

	private static class CheckStatusOnlyHandler implements ResponseHandler<Void> {

		public CheckStatusOnlyHandler () {}

		@Override
		public Void handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine());
			return null;
		}

	}

}

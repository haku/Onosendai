package com.vaguehope.onosendai.provider.hosaka;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Base64;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.HttpClientFactory;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * TODO refactor copypasta with Instapaper.
 */
public class Hosaka {

	private static final String BASE_URL = "https://hosaka.herokuapp.com:443";
	private static final String API_TEST_AUTH = "/me";
	private static final String API_COLUMNS = "/me/columns";

	static final LogWrapper LOG = new LogWrapper("HS");

	private final Account account;
	private final HttpClientFactory httpClientFactory;

	public Hosaka (final Account account, final HttpClientFactory httpClientFactory) {
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
		final HttpGet get = new HttpGet(BASE_URL + API_TEST_AUTH);
		addAuth(get);
		getHttpClient().execute(get, new CheckStatusOnlyHandler());
	}

	public Map<String, HosakaColumn> sendColumns (final Map<String, HosakaColumn> columns) throws IOException, JSONException {
		final HttpPost post = new HttpPost(BASE_URL + API_COLUMNS);
		addAuth(post);

		final JSONObject columnsJson = new JSONObject();
		for (final Entry<String, HosakaColumn> e : columns.entrySet()) {
			columnsJson.put(e.getKey(), e.getValue().toJson());
		}
		post.setEntity(new StringEntity(columnsJson.toString(), "UTF-8"));

		return getHttpClient().execute(post, new HosakaColumnsHandler());
	}

	private void addAuth (final AbstractHttpMessage req) {
		try {
			req.setHeader("Authorization", "Basic " + Base64.encodeToString(
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

	private static class HosakaColumnsHandler implements ResponseHandler<Map<String, HosakaColumn>> {

		public HosakaColumnsHandler () {}

		@Override
		public Map<String, HosakaColumn> handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine());
			try {
				final JSONObject o = (JSONObject) new JSONTokener(EntityUtils.toString(response.getEntity(), "UTF-8")).nextValue();
				final Map<String, HosakaColumn> ret = new HashMap<String, HosakaColumn>();
				final Iterator<String> keys = o.keys();
				while (keys.hasNext()) {
					final String key = keys.next();
					ret.put(key, HosakaColumn.parseJson(o.getJSONObject(key)));
				}
				return ret;
			}
			catch (final JSONException e) {
				throw new IOException(e); // FIXME
			}
		}

	}

}

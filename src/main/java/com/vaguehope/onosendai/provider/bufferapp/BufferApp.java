package com.vaguehope.onosendai.provider.bufferapp;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.successwhale.HttpClientFactory;
import com.vaguehope.onosendai.provider.successwhale.NotAuthorizedException;
import com.vaguehope.onosendai.provider.successwhale.PostToAccount;
import com.vaguehope.onosendai.provider.successwhale.ServiceRef;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * TODO: - Move SW classes to generic package.
 */
public class BufferApp {

	private static final String BASE_URL = "https://api.bufferapp.com:443";
	private static final String API_PROFILES = "/1/profiles.json";
	private static final String API_UPDATES_CREATE = "/1/updates/create.json";

	static final LogWrapper LOG = new LogWrapper("BF");

	private final Account account;
	private final HttpClientFactory httpClientFactory;

	public BufferApp (final Account account, final HttpClientFactory httpClientFactory) {
		if (account == null) throw new IllegalArgumentException("account can not be null.");
		if (httpClientFactory == null) throw new IllegalArgumentException("httpClientFactory can not be null.");
		this.account = account;
		this.httpClientFactory = httpClientFactory;
	}

	private HttpClient getHttpClient () throws BufferAppException {
		try {
			return this.httpClientFactory.getHttpClient();
		}
		catch (final SuccessWhaleException e) { // FIXME make http client more generic.
			throw new BufferAppException(e.getMessage(), e);
		}
	}

	Account getAccount () {
		return this.account;
	}

	private interface BufferCall<T> {
		T invoke (HttpClient client) throws BufferAppException, IOException;

		String describeFailure (Exception e);
	}

	private <T> T authenticated (final BufferCall<T> call) throws BufferAppException {
		try {
			return call.invoke(getHttpClient());
		}
		catch (final IOException e) {
			throw new BufferAppException(call.describeFailure(e), e);
		}
	}

	public List<PostToAccount> getPostToAccounts () throws BufferAppException {
		return authenticated(new BufferCall<List<PostToAccount>>() {
			@Override
			public List<PostToAccount> invoke (final HttpClient client) throws BufferAppException, IOException {
				return client.execute(new HttpGet(makeAuthedUrl(API_PROFILES)), new ProfilesHandler());
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch post to accounts: " + e.toString();
			}
		});
	}

	public void post (final Set<ServiceRef> postToSvc, final String body) throws BufferAppException {
		authenticated(new BufferCall<Void>() {
			@Override
			public Void invoke (final HttpClient client) throws BufferAppException, IOException {
				final HttpPost post = new HttpPost(BASE_URL + API_UPDATES_CREATE);
				final List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				addAuthParams(params);
				params.add(new BasicNameValuePair("text", body));

				for (final ServiceRef svc : postToSvc) {
					params.add(new BasicNameValuePair("profile_ids[]", svc.getId()));
				}

				post.setEntity(new UrlEncodedFormEntity(params));
				client.execute(post, new CheckStatusOnlyHandler());
				return null;
			}

			@Override
			public String describeFailure (final Exception e) {
				return e.toString();
			}
		});
	}

	String makeAuthedUrl (final String api, final String... params) throws IOException {
		final StringBuilder u = new StringBuilder().append(BASE_URL).append(api).append("?")
				.append("&access_token=").append(URLEncoder.encode(getAccount().getAccessToken(), "UTF-8"));
		if (params != null) {
			for (final String param : params) {
				u.append(param);
			}
		}
		return u.toString();
	}

	void addAuthParams (final List<NameValuePair> params) {
		params.add(new BasicNameValuePair("access_token", getAccount().getAccessToken()));
	}

	static void checkReponseCode (final StatusLine statusLine) throws IOException {
		final int code = statusLine.getStatusCode();
		if (code == 401) {
			throw new NotAuthorizedException();
		}
		else if (code < 200 || code >= 300) {
			throw new IOException("HTTP " + code + ": " + statusLine.getReasonPhrase());
		}
	}

	private static class ProfilesHandler implements ResponseHandler<List<PostToAccount>> {

		public ProfilesHandler () {}

		@Override
		public List<PostToAccount> handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine());
			try {
				final List<PostToAccount> accounts = new ArrayList<PostToAccount>();
				final JSONArray arr = (JSONArray) new JSONTokener(EntityUtils.toString(response.getEntity())).nextValue();
				for (int i = 0; i < arr.length(); i++) {
					final JSONObject prof = arr.getJSONObject(i);
					final String id = prof.getString("id");
					final String service = prof.getString("service");
					final String username = prof.getString("service_username");
					final String uid = prof.getString("service_id");
					final boolean enabled = prof.getBoolean("default");
					accounts.add(new PostToAccount(id, service, username, uid, enabled));
				}
				return accounts;
			}
			catch (final JSONException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
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

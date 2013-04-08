package com.vaguehope.onosendai.provider.successwhale;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.storage.KvStore;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://github.com/ianrenton/successwhale-api/blob/master/APIDOCS.md
 */
public class SuccessWhale {

	private static final String BASE_URL = "https://successwhale-api.herokuapp.com:443";
	private static final String API_AUTH = "/v3/authenticate.json";
	private static final String API_FEED = "/v3/feed.xml";
	private static final String API_THREAD = "/v3/thread.xml";
	private static final String API_POSTTOACCOUNTS = "/v3/posttoaccounts.xml";
	private static final String API_ITEM = "/v3/item";
	private static final String API_ACTION = "/v3/action";

	private static final String AUTH_TOKEN_PREFIX = "SW_AUTH_TOKEN_";
	private static final String PTA_PREFIX = "SW_PTA_";

	private static final LogWrapper LOG = new LogWrapper("SW");

	private final KvStore kvStore;
	private final Account account;
	private final HttpClientFactory httpClientFactory;

	private String token;

	public SuccessWhale (final KvStore kvStore, final Account account, final HttpClientFactory httpClientFactory) {
		this.kvStore = kvStore;
		this.account = account;
		this.httpClientFactory = httpClientFactory;
	}

	private HttpClient getHttpClient () throws SuccessWhaleException {
		return this.httpClientFactory.getHttpClient();
	}

	Account getAccount () {
		return this.account;
	}

	private interface SwCall<T> {
		T invoke (HttpClient client) throws SuccessWhaleException, IOException;

		String describeFailure (Exception e);
	}

	private <T> T authenticated (final SwCall<T> call) throws SuccessWhaleException {
		if (this.token == null) readAuthFromKvStore();
		if (this.token == null) authenticate();
		try {
			try {
				return call.invoke(getHttpClient());
			}
			catch (NotAuthorizedException e) {
				LOG.i("Stored auth token rejected, reauthenticating.");
				this.token = null;
				writeAuthToKvStore();
				authenticate();
				return call.invoke(getHttpClient());
			}
		}
		catch (IOException e) {
			throw new SuccessWhaleException(call.describeFailure(e), e);
		}
	}

	private void readAuthFromKvStore () {
		final String t = this.kvStore.getValue(AUTH_TOKEN_PREFIX + getAccount().getId());
		if (t != null && !t.isEmpty()) this.token = t;
	}

	private void writeAuthToKvStore () {
		this.kvStore.storeValue(AUTH_TOKEN_PREFIX + getAccount().getId(), this.token);
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

	/**
	 * FIXME lock against multiple calls.
	 */
	public void authenticate () throws SuccessWhaleException {
		final String username = this.account.getAccessToken();
		final String password = this.account.getAccessSecret();
		try {
			final HttpPost post = new HttpPost(BASE_URL + API_AUTH);
			final List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(params));
			this.token = getHttpClient().execute(post, new AuthHandler());
			LOG.i("Authenticated username='%s'.", username);
			writeAuthToKvStore();
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Auth failed for user '" + username + "': " + e.toString(), e);
		}
	}

	public TweetList getFeed (final SuccessWhaleFeed feed) throws SuccessWhaleException {
		return authenticated(new SwCall<TweetList>() {
			@Override
			public TweetList invoke (final HttpClient client) throws SuccessWhaleException, IOException {
				String url = makeAuthedUrl(API_FEED, "&sources=", URLEncoder.encode(feed.getSources(), "UTF-8"));
				return client.execute(new HttpGet(url), new FeedHandler(getAccount()));
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch feed '" + feed + "': " + e.toString();
			}
		});
	}

	public TweetList getThread (final String serviceType, final String serviceSid, final String forSid) throws SuccessWhaleException {
		return authenticated(new SwCall<TweetList>() {
			@Override
			public TweetList invoke (final HttpClient client) throws SuccessWhaleException, IOException {
				String url = makeAuthedUrl(API_THREAD, "&service=", serviceType, "&uid=" + serviceSid, "&postid=", forSid);
				final TweetList thread = client.execute(new HttpGet(url), new FeedHandler(getAccount()));
				return removeItem(thread, forSid);
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch thread for sid='" + forSid + "': " + e.toString();
			}
		});
	}

	public List<PostToAccount> getPostToAccounts () throws SuccessWhaleException {
		return authenticated(new SwCall<List<PostToAccount>>() {
			@Override
			public List<PostToAccount> invoke (final HttpClient client) throws SuccessWhaleException, IOException {
				return client.execute(new HttpGet(makeAuthedUrl(API_POSTTOACCOUNTS)), new PostToAccountsHandler(SuccessWhale.this));
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch post to accounts: " + e.toString();
			}
		});
	}

	public List<PostToAccount> getPostToAccountsCached () {
		final String key = PTA_PREFIX + getAccount().getId();
		try {
			String cached = this.kvStore.getValue(key);
			if (cached == null || cached.isEmpty()) return null;
			return new PostToAccountsXml(new StringReader(cached)).getAccounts();
		}
		catch (SAXException e) {
			LOG.e("Failed to parse cached post to accounts.  Clearing cache.", e);
			this.kvStore.storeValue(key, null);
			return null;
		}
	}

	protected void writePostToAccountsToCache (final String data) {
		this.kvStore.storeValue(PTA_PREFIX + getAccount().getId(), data);
	}

	public void post (final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid) throws SuccessWhaleException {
		authenticated(new SwCall<Void>() {
			@Override
			public Void invoke (final HttpClient client) throws SuccessWhaleException, IOException {
				final HttpPost post = new HttpPost(BASE_URL + API_ITEM);
				final List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				addAuthParams(params);
				params.add(new BasicNameValuePair("text", body));

				StringBuilder accounts = new StringBuilder();
				for (ServiceRef svc : postToSvc) {
					if (accounts.length() > 0) accounts.append(":");
					accounts.append(svc.getRawType()).append("/").append(svc.getUid());
				}
				params.add(new BasicNameValuePair("accounts", accounts.toString()));

				if (inReplyToSid != null && !inReplyToSid.isEmpty()) {
					params.add(new BasicNameValuePair("in_reply_to_id", inReplyToSid));
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

	public void itemAction (final ServiceRef svc, final String itemSid, final ItemAction itemAction) throws SuccessWhaleException {
		authenticated(new SwCall<Void>() {
			@Override
			public Void invoke (final HttpClient client) throws SuccessWhaleException, IOException {
				final HttpPost post = new HttpPost(BASE_URL + API_ACTION);
				final List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				addAuthParams(params);
				params.add(new BasicNameValuePair("service", svc.getRawType()));
				params.add(new BasicNameValuePair("uid", svc.getUid()));
				params.add(new BasicNameValuePair("postid", itemSid));
				params.add(new BasicNameValuePair("action", itemAction.getAction()));

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

	String makeAuthedUrl (final String api, final String... params) {
		StringBuilder u = new StringBuilder().append(BASE_URL).append(api).append("?")
				.append("&token=").append(this.token);
		if (params != null) {
			for (String param : params) {
				u.append(param);
			}
		}
		return u.toString();
	}

	void addAuthParams (final List<NameValuePair> params) {
		params.add(new BasicNameValuePair("token", this.token));
	}

	static TweetList removeItem (final TweetList thread, final String sid) {
		if (thread.count() < 1 || sid == null) return thread;
		for (Tweet t : thread.getTweets()) {
			if (sid.equals(t.getSid())) {
				final List<Tweet> newList = new ArrayList<Tweet>(thread.getTweets());
				newList.remove(t);
				return new TweetList(newList);
			}
		}
		return thread;
	}

	private static class AuthHandler implements ResponseHandler<String> {

		public AuthHandler () {}

		@Override
		public String handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine());
			try {
				final String authRespRaw = EntityUtils.toString(response.getEntity());
				final JSONObject authResp = (JSONObject) new JSONTokener(authRespRaw).nextValue();
				if (!authResp.getBoolean("success")) {
					throw new IOException("Auth rejected: " + authResp.getString("error"));
				}
				return authResp.getString("token");
			}
			catch (final JSONException e) {
				throw new IOException("Response unparsable: " + e.toString(), e);
			}
		}

	}

	private static class PostToAccountsHandler implements ResponseHandler<List<PostToAccount>> {

		private final SuccessWhale sw;

		public PostToAccountsHandler (final SuccessWhale sw) {
			this.sw = sw;
		}

		@Override
		public List<PostToAccount> handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine());
			try {
				final byte[] data = EntityUtils.toByteArray(response.getEntity());
				List<PostToAccount> accounts = new PostToAccountsXml(new ByteArrayInputStream(data)).getAccounts();
				if (this.sw != null) this.sw.writePostToAccountsToCache(new String(data, Charset.forName("UTF-8")));
				return accounts;
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
		}

	}

	private static class FeedHandler implements ResponseHandler<TweetList> {

		private final Account account;

		public FeedHandler (final Account account) {
			this.account = account;
		}

		@Override
		public TweetList handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine());
			try {
				return new SuccessWhaleFeedXml(this.account, response.getEntity().getContent()).getTweets();
			}
			catch (final SAXException e) {
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

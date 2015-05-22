package com.vaguehope.onosendai.provider.successwhale;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import local.apache.InputStreamPart;
import local.apache.MultipartEntity;
import local.apache.Part;
import local.apache.StringPart;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import org.xml.sax.SAXException;

import android.net.http.AndroidHttpClient;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.storage.KvStore;
import com.vaguehope.onosendai.util.HttpClientFactory;
import com.vaguehope.onosendai.util.ImageMetadata;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://github.com/ianrenton/successwhale-api/blob/master/APIDOCS.md
 */
public class SuccessWhale {

	private static final String BASE_URL = "https://api2.successwhale.com:443";
	private static final String API_AUTH = "/v3/authenticate.json";
	private static final String API_COLUMNS = "/v3/columns.xml";
	private static final String API_SOURCES = "/v3/sources.xml";
	private static final String API_FEED = "/v3/feed.xml";
	private static final String API_THREAD = "/v3/thread.xml";
	private static final String API_POSTTOACCOUNTS = "/v3/posttoaccounts.xml";
	private static final String API_ITEM = "/v3/item";
	private static final String API_ACTION = "/v3/action";
	private static final String API_BANNED_PHRASES = "/v3/bannedphrases.json";

	private static final String AUTH_TOKEN_PREFIX = "SW_AUTH_TOKEN_";
	private static final String PTA_PREFIX = "SW_PTA_";

	static final LogWrapper LOG = new LogWrapper("SW");

	private final KvStore kvStore;
	private final Account account;
	private final HttpClientFactory httpClientFactory;

	private String token;

	public SuccessWhale (final KvStore kvStore, final Account account, final HttpClientFactory httpClientFactory) {
		if (kvStore == null) throw new IllegalArgumentException("kvStore can not be null.");
		if (account == null) throw new IllegalArgumentException("account can not be null.");
		if (httpClientFactory == null) throw new IllegalArgumentException("httpClientFactory can not be null.");
		this.kvStore = kvStore;
		this.account = account;
		this.httpClientFactory = httpClientFactory;
	}

	private HttpClient getHttpClient () throws IOException {
		return this.httpClientFactory.getHttpClient();
	}

	Account getAccount () {
		return this.account;
	}

	private interface SwCall<T> {
		T invoke (HttpClient client) throws IOException;

		String describeFailure (Exception e);
	}

	private <T> T authenticated (final SwCall<T> call) throws SuccessWhaleException {
		if (this.token == null) readAuthFromKvStore();
		if (this.token == null) authenticate();
		try {
			try {
				return call.invoke(getHttpClient());
			}
			catch (final NotAuthorizedException e) {
				LOG.i("Stored auth token rejected, reauthenticating.");
				this.token = null;
				writeAuthToKvStore();
				authenticate();
				return call.invoke(getHttpClient());
			}
		}
		catch (final InvalidRequestException e) {
			throw new SuccessWhaleException(call.describeFailure(e), e, true);
		}
		catch (final IOException e) {
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

	static void checkReponseCode (final HttpResponse response) throws IOException {
		final int code = response.getStatusLine().getStatusCode();
		if (code == 401) { // NOSONAR not a magic number.
			throw new NotAuthorizedException();
		}
		else if (code >= 400 && code < 500) { // NOSONAR not a magic number.
			throw new InvalidRequestException(response);
		}
		else if (code < 200 || code >= 300) { // NOSONAR not a magic number.
			throw new SuccessWhaleException(response);
		}
	}

	/**
	 * FIXME lock against multiple calls.
	 */
	private void authenticate () throws SuccessWhaleException {
		final String username = this.account.getAccessToken();
		final String password = this.account.getAccessSecret();
		try {
			final HttpPost post = new HttpPost(BASE_URL + API_AUTH);
			final List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			this.token = getHttpClient().execute(post, new AuthHandler());
			LOG.i("Authenticated username='%s'.", username);
			writeAuthToKvStore();
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Auth failed for user '" + username + "': " + e.toString(), e);
		}
	}

	public void testLogin () throws SuccessWhaleException {
		getPostToAccounts();
	}

	public SuccessWhaleColumns getColumns () throws SuccessWhaleException {
		return authenticated(new SwCall<SuccessWhaleColumns>() {
			@Override
			public SuccessWhaleColumns invoke (final HttpClient client) throws IOException {
				final HttpGet req = new HttpGet(makeAuthedUrl(API_COLUMNS));
				AndroidHttpClient.modifyRequestToAcceptGzipResponse(req);
				return client.execute(req, new ColumnsHandler(getAccount()));
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch columns: " + e.toString();
			}
		});
	}

	public SuccessWhaleSources getSources () throws SuccessWhaleException {
		return authenticated(new SwCall<SuccessWhaleSources>() {
			@Override
			public SuccessWhaleSources invoke (final HttpClient client) throws IOException {
				final HttpGet req = new HttpGet(makeAuthedUrl(API_SOURCES));
				AndroidHttpClient.modifyRequestToAcceptGzipResponse(req);
				return client.execute(req, SourcesHandler.INSTANCE);
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch sources: " + e.toString();
			}
		});
	}

	public TweetList getFeed (final SuccessWhaleFeed feed, final String sinceId) throws SuccessWhaleException {
		return authenticated(new SwCall<TweetList>() {
			private String url;

			@Override
			public TweetList invoke (final HttpClient client) throws IOException {
				this.url = makeAuthedUrl(API_FEED, "&sources=", URLEncoder.encode(feed.getSources(), "UTF-8"));

				// FIXME disabling this until SW finds a way to accept it on mixed feeds [issue 89].
//				if (sinceId != null) this.url += "&since_id=" + sinceId;

				final HttpGet req = new HttpGet(this.url);
				AndroidHttpClient.modifyRequestToAcceptGzipResponse(req);
				return client.execute(req, new FeedHandler(getAccount()));
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch feed '" + feed + "' from '" + this.url + "': " + e.toString();
			}
		});
	}

	public TweetList getThread (final String serviceType, final String serviceSid, final String forSid) throws SuccessWhaleException {
		return authenticated(new SwCall<TweetList>() {
			@Override
			public TweetList invoke (final HttpClient client) throws IOException {
				final String url = makeAuthedUrl(API_THREAD, "&service=", serviceType, "&uid=" + serviceSid, "&postid=", forSid);
				final HttpGet req = new HttpGet(url);
				AndroidHttpClient.modifyRequestToAcceptGzipResponse(req);
				final TweetList thread = client.execute(req, new FeedHandler(getAccount()));
				return removeItem(thread, forSid);
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch thread for sid='" + forSid + "': " + e.toString();
			}
		});
	}

	public List<ServiceRef> getPostToAccounts () throws SuccessWhaleException {
		return authenticated(new SwCall<List<ServiceRef>>() {
			@Override
			public List<ServiceRef> invoke (final HttpClient client) throws IOException {
				return client.execute(new HttpGet(makeAuthedUrl(API_POSTTOACCOUNTS)), new PostToAccountsHandler(SuccessWhale.this));
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch post to accounts: " + e.toString();
			}
		});
	}

	public List<ServiceRef> getPostToAccountsCached () {
		final String key = PTA_PREFIX + getAccount().getId();
		try {
			final String cached = this.kvStore.getValue(key);
			if (cached == null || cached.isEmpty()) return null;
			return new PostToAccountsXml(new StringReader(cached)).getAccounts();
		}
		catch (final SAXException e) {
			LOG.e("Failed to parse cached post to accounts.  Clearing cache.", e);
			this.kvStore.storeValue(key, null);
			return null;
		}
	}

	protected void writePostToAccountsToCache (final String data) {
		this.kvStore.storeValue(PTA_PREFIX + getAccount().getId(), data);
	}

	public void post (final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid, final ImageMetadata image) throws SuccessWhaleException {
		authenticated(new SwCall<Void>() {
			@Override
			public Void invoke (final HttpClient client) throws IOException {
				attemptPost(client, postToSvc, body, inReplyToSid, image);
				return null;
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to post via SuccessWhale: " + e.toString();
			}
		});
	}

	protected void attemptPost (final HttpClient client, final Set<ServiceRef> postToSvc, final String body, final String inReplyToSid, final ImageMetadata image) throws IOException {
		InputStream attachmentIs = null;
		try {
			final HttpPost post = new HttpPost(BASE_URL + API_ITEM);
			final List<Part> parts = new ArrayList<Part>();
			parts.add(new StringPart("token", SuccessWhale.this.token));
			parts.add(new StringPart("text", body));

			final StringBuilder accounts = new StringBuilder();
			for (final ServiceRef svc : postToSvc) {
				if (accounts.length() > 0) accounts.append(":");
				accounts.append(svc.getRawType()).append("/").append(svc.getUid());
			}
			parts.add(new StringPart("accounts", accounts.toString()));

			if (inReplyToSid != null && !inReplyToSid.isEmpty()) {
				parts.add(new StringPart("in_reply_to_id", inReplyToSid));
			}

			if (image != null && image.exists()) {
				attachmentIs = image.open();
				parts.add(new InputStreamPart("file", image.getName(), image.getSize(), attachmentIs));
			}

			post.setEntity(new MultipartEntity(parts.toArray(new Part[] {})));
			client.execute(post, new CheckStatusOnlyHandler());
		}
		finally {
			IoHelper.closeQuietly(attachmentIs);
		}
	}

	public void itemAction (final ServiceRef svc, final String itemSid, final ItemAction itemAction) throws SuccessWhaleException {
		authenticated(new SwCall<Void>() {
			@Override
			public Void invoke (final HttpClient client) throws IOException {
				attemptItemAction(client, svc, itemSid, itemAction);
				return null;
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Item action failed: " + e.toString();
			}
		});
	}

	protected void attemptItemAction (final HttpClient client, final ServiceRef svc, final String itemSid, final ItemAction itemAction) throws IOException {
		final HttpPost post = new HttpPost(BASE_URL + API_ACTION);
		final List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		addAuthParams(params);
		params.add(new BasicNameValuePair("service", svc.getRawType()));
		params.add(new BasicNameValuePair("uid", svc.getUid()));
		params.add(new BasicNameValuePair("postid", itemSid));
		params.add(new BasicNameValuePair("action", itemAction.getAction()));

		post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		client.execute(post, new CheckStatusOnlyHandler());
	}

	public List<String> getBannedPhrases () throws SuccessWhaleException {
		return authenticated(new SwCall<List<String>>() {
			@Override
			public List<String> invoke (final HttpClient client) throws IOException {
				final HttpGet req = new HttpGet(makeAuthedUrl(API_BANNED_PHRASES));
				AndroidHttpClient.modifyRequestToAcceptGzipResponse(req);
				return client.execute(req, BannedPhrasesHandler.INSTANCE);
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to fetch banned phrases: " + e.toString();
			}
		});
	}

	public void setBannedPhrases (final List<String> bannedPhrases) throws SuccessWhaleException {
		authenticated(new SwCall<Void>() {
			@Override
			public Void invoke (final HttpClient client) throws IOException {
				final HttpPost post = new HttpPost(BASE_URL + API_BANNED_PHRASES);
				final List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				addAuthParams(params);
				params.add(new BasicNameValuePair("bannedphrases", new JSONArray(bannedPhrases).toString()));

				post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
				client.execute(post, new CheckStatusOnlyHandler());
				return null;
			}

			@Override
			public String describeFailure (final Exception e) {
				return "Failed to set banned phrases: " + e.toString();
			}
		});
	}

	String makeAuthedUrl (final String api, final String... params) {
		final StringBuilder u = new StringBuilder().append(BASE_URL).append(api).append("?")
				.append("&token=").append(this.token);
		if (params != null) {
			for (final String param : params) {
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
		for (final Tweet t : thread.getTweets()) {
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
			checkReponseCode(response);
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

	private static class PostToAccountsHandler implements ResponseHandler<List<ServiceRef>> {

		private final SuccessWhale sw;

		public PostToAccountsHandler (final SuccessWhale sw) {
			this.sw = sw;
		}

		@Override
		public List<ServiceRef> handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response);
			try {
				final byte[] data = EntityUtils.toByteArray(response.getEntity());
				final List<ServiceRef> accounts = new PostToAccountsXml(new ByteArrayInputStream(data)).getAccounts();
				if (this.sw != null) this.sw.writePostToAccountsToCache(new String(data, Charset.forName("UTF-8")));
				return accounts;
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
		}

	}

	private static class ColumnsHandler implements ResponseHandler<SuccessWhaleColumns> {

		private final Account account;

		public ColumnsHandler (final Account account) {
			this.account = account;
		}

		@Override
		public SuccessWhaleColumns handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response);
			try {
				return new ColumnsXml(this.account, AndroidHttpClient.getUngzippedContent(response.getEntity())).getColumns();
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
		}

	}

	private enum SourcesHandler implements ResponseHandler<SuccessWhaleSources> {
		INSTANCE;

		@Override
		public SuccessWhaleSources handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response);
			try {
				return new SourcesXml(AndroidHttpClient.getUngzippedContent(response.getEntity())).getSources();
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
			checkReponseCode(response);
			try {
				final HttpEntity entity = response.getEntity();
				LOG.d("Feed content encoding: '%s', headers: %s.", entity.getContentEncoding(), Arrays.asList(response.getAllHeaders()));
				return new SuccessWhaleFeedXml(this.account, AndroidHttpClient.getUngzippedContent(entity)).getTweets();
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
		}

	}

	private enum BannedPhrasesHandler implements ResponseHandler<List<String>> {
		INSTANCE;

		@Override
		public List<String> handleResponse (final HttpResponse response) throws ClientProtocolException, IOException {
			checkReponseCode(response);
			final String raw = IoHelper.toString(AndroidHttpClient.getUngzippedContent(response.getEntity()));
			try {
				final JSONArray arr = ((JSONObject) new JSONTokener(raw).nextValue()).getJSONArray("bannedphrases");
				final List<String> ret = new ArrayList<String>();
				for (int i = 0; i < arr.length(); i++) {
					ret.add(arr.getString(i));
				}
				return ret;
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
			checkReponseCode(response);
			return null;
		}

	}

}

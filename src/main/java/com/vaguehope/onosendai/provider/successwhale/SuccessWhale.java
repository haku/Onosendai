package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
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
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://github.com/ianrenton/successwhale-api/blob/master/APIDOCS.md
 */
public class SuccessWhale {

	private static final String BASE_URL = "https://api.successwhale.com:443";
	private static final String API_AUTH = "/v3/authenticate.json";
	private static final String API_FEED = "/v3/feed.xml";
	private static final String API_POSTTOACCOUNTS = "/v3/posttoaccounts.xml";
	private static final String API_ITEM = "/v3/item";

	private final LogWrapper log = new LogWrapper("SW");
	private final Account account;
	private final HttpClientFactory httpClientFactory;

	private SuccessWhaleAuth auth;

	public SuccessWhale (final Account account, final HttpClientFactory httpClientFactory) {
		this.account = account;
		this.httpClientFactory = httpClientFactory;
	}

	private boolean authenticated () {
		return this.auth != null;
	}

	private void ensureAuthenticated () throws SuccessWhaleException {
		if (!authenticated()) authenticate();
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
			this.auth = this.httpClientFactory.getHttpClient().execute(post, new AuthHandler());
			this.log.i("Authenticated username='%s' userid='%s'.", username, this.auth.getUserid());
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Auth failed for user '" + username + "': " + e.toString(), e);
		}
	}

	public TweetList getFeed (final SuccessWhaleFeed feed) throws SuccessWhaleException {
		ensureAuthenticated();
		try {
			String url = makeAuthedUrl(API_FEED, "&sources=", URLEncoder.encode(feed.getSources(), "UTF-8"));
			return this.httpClientFactory.getHttpClient().execute(new HttpGet(url), new FeedHandler());
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Failed to fetch feed '" + feed.toString() + "': " + e.toString(), e); // FIXME does feed have good toString()?
		}
	}

	public List<PostToAccount> getPostToAccounts () throws SuccessWhaleException {
		ensureAuthenticated();
		try {
			return this.httpClientFactory.getHttpClient().execute(new HttpGet(makeAuthedUrl(API_POSTTOACCOUNTS)), new PostToAccountsHandler());
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Failed to fetch post to accounts: " + e.toString(), e);
		}
	}

	public void post (final Set<PostToAccount> postToAccounts, final String body, final String inReplyToSid) throws SuccessWhaleException {
		ensureAuthenticated();
		try {
			final HttpPost post = new HttpPost(BASE_URL + API_ITEM);
			final List<NameValuePair> params = new ArrayList<NameValuePair>(4);
			params.add(new BasicNameValuePair("sw_uid", this.auth.getUserid()));
			params.add(new BasicNameValuePair("secret", this.auth.getSecret()));
			params.add(new BasicNameValuePair("text", body));

			StringBuilder accounts = new StringBuilder();
			for (PostToAccount pta : postToAccounts) {
				if (accounts.length() > 0) accounts.append(":");
				accounts.append(pta.getService()).append("/").append(pta.getUid());
			}
			params.add(new BasicNameValuePair("accounts", accounts.toString()));

			if (inReplyToSid != null && !inReplyToSid.isEmpty()) {
				params.add(new BasicNameValuePair("in_reply_to_id", inReplyToSid));
			}

			post.setEntity(new UrlEncodedFormEntity(params));
			this.httpClientFactory.getHttpClient().execute(post, new PostHandler());
		}
		catch (final IOException e) {
			throw new SuccessWhaleException(e.toString(), e);
		}
	}

	private String makeAuthedUrl (final String api, final String... params) {
		StringBuilder u = new StringBuilder().append(BASE_URL).append(api).append("?")
				.append("sw_uid=").append(this.auth.getUserid())
				.append("&secret=").append(this.auth.getSecret());
		if (params != null) {
			for (String param : params) {
				u.append(param);
			}
		}
		return u.toString();
	}

	static void checkReponseCode (final StatusLine statusLine, final int code) throws IOException {
		if (statusLine.getStatusCode() != code) {
			throw new IOException("HTTP " + statusLine.getStatusCode() + ": " + statusLine.getReasonPhrase());
		}
	}

	private static class AuthHandler implements ResponseHandler<SuccessWhaleAuth> {

		public AuthHandler () {}

		@Override
		public SuccessWhaleAuth handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine(), HttpStatus.SC_OK);
			try {
				final String authRespRaw = EntityUtils.toString(response.getEntity());
				final JSONObject authResp = (JSONObject) new JSONTokener(authRespRaw).nextValue();
				if (!authResp.getBoolean("success")) {
					throw new IOException("Auth rejected: " + authResp.getString("error"));
				}
				return new SuccessWhaleAuth(authResp.getString("userid"), authResp.getString("secret"));
			}
			catch (final JSONException e) {
				throw new IOException("Response unparsable: " + e.toString(), e);
			}
		}

	}

	private static class PostToAccountsHandler implements ResponseHandler<List<PostToAccount>> {

		public PostToAccountsHandler () {}

		@Override
		public List<PostToAccount> handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine(), HttpStatus.SC_OK);
			try {
				return new PostToAccountsXml(response.getEntity().getContent()).getAccounts();
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
		}

	}

	private static class FeedHandler implements ResponseHandler<TweetList> {

		public FeedHandler () {}

		@Override
		public TweetList handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine(), HttpStatus.SC_OK);
			try {
				return new SuccessWhaleFeedXml(response.getEntity().getContent()).getTweets();
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
		}

	}

	private static class PostHandler implements ResponseHandler<Void> {

		public PostHandler () {}

		@Override
		public Void handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine(), HttpStatus.SC_OK);
			return null;
		}

	}

	private static class SuccessWhaleAuth {

		private final String userid;
		private final String secret;

		public SuccessWhaleAuth (final String userid, final String secret) {
			this.userid = userid;
			this.secret = secret;
		}

		public String getUserid () {
			return this.userid;
		}

		public String getSecret () {
			return this.secret;
		}

	}

}

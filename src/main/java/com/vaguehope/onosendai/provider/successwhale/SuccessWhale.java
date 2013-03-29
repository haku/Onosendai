package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
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
			this.auth = this.httpClientFactory.getHttpClient().execute(post, new SuccessWhaleAuthHandler());
			this.log.i("Authenticated username='%s' userid='%s'.", username, this.auth.getUserid());
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Auth failed for user '" + username + "': " + e.toString(), e);
		}
	}

	public TweetList getFeed (final SuccessWhaleFeed feed) throws SuccessWhaleException {
		ensureAuthenticated();
		try {
			final StringBuilder url = new StringBuilder();
			url.append(BASE_URL).append(API_FEED).append("?")
					.append("sw_uid=").append(this.auth.getUserid())
					.append("&secret=").append(this.auth.getSecret())
					.append("&sources=").append(URLEncoder.encode(feed.getSources(), "UTF-8"));
			return this.httpClientFactory.getHttpClient().execute(new HttpGet(url.toString()), new SuccessWhaleFeedHandler());
		}
		catch (final IOException e) {
			throw new SuccessWhaleException("Failed to fetch feed '" + feed.toString() + "': " + e.toString(), e); // FIXME does feed have good toString()?
		}
	}

	static void checkReponseCode (final StatusLine statusLine, final int code) throws IOException {
		if (statusLine.getStatusCode() != code) {
			throw new IOException("Server returned " + statusLine.getStatusCode() + ": " + statusLine.getReasonPhrase());
		}
	}

	private class SuccessWhaleAuthHandler implements ResponseHandler<SuccessWhaleAuth> {

		public SuccessWhaleAuthHandler () {}

		@Override
		public SuccessWhaleAuth handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine(), 200);
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

	private class SuccessWhaleFeedHandler implements ResponseHandler<TweetList> {

		public SuccessWhaleFeedHandler () {}

		@Override
		public TweetList handleResponse (final HttpResponse response) throws IOException {
			checkReponseCode(response.getStatusLine(), 200);
			try {
				return new SuccessWhaleFeedXml(response.getEntity().getContent()).getTweets();
			}
			catch (final SAXException e) {
				throw new IOException("Failed to parse response: " + e.toString(), e);
			}
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

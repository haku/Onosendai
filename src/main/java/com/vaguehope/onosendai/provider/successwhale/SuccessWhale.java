package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
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

	/**
	 * FIXME lock against multiple calls.
	 */
	public void authenticate () throws SuccessWhaleException {
		final String username = this.account.accessToken;
		final String password = this.account.accessSecret;
		try {
			HttpPost post = new HttpPost(BASE_URL + API_AUTH);
			List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("username", username));
			params.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(params));
			this.auth = this.httpClientFactory.getHttpClient().execute(post, new ResponseHandler<SuccessWhaleAuth>() {
				@Override
				public SuccessWhaleAuth handleResponse (final HttpResponse response) throws ClientProtocolException, IOException {
					checkReponseCode(response.getStatusLine(), 200);
					try {
						String authRespRaw = EntityUtils.toString(response.getEntity());
						JSONObject authResp = (JSONObject) new JSONTokener(authRespRaw).nextValue();
						if (!authResp.getBoolean("success")) {
							throw new IOException("Auth rejected: " + authResp.getString("error"));
						}
						return new SuccessWhaleAuth(authResp.getString("userid"), authResp.getString("secret"));
					}
					catch (JSONException e) {
						throw new IOException("Response unparsable: " + e.toString(), e);
					}
				}
			});
			this.log.i("Authenticated username='%s' userid='%s'.", username, this.auth.userid);
		}
		catch (IOException e) {
			throw new SuccessWhaleException("Auth failed for user '" + username + "': " + e.toString(), e);
		}
	}

	public TweetList getFeed (final SuccessWhaleFeed feed) throws SuccessWhaleException {
		ensureAuthenticated();
		try {
			StringBuilder url = new StringBuilder();
			url.append(BASE_URL).append(API_FEED).append("?")
					.append("sw_uid=").append(this.auth.userid)
					.append("&secret=").append(this.auth.secret)
					.append("&sources=").append(URLEncoder.encode(feed.getSources(), "UTF-8"));
			return this.httpClientFactory.getHttpClient().execute(new HttpGet(url.toString()), new ResponseHandler<TweetList>() {
				@Override
				public TweetList handleResponse (final HttpResponse response) throws ClientProtocolException, IOException {
					checkReponseCode(response.getStatusLine(), 200);
					try {
						return new SuccessWhaleFeedXml(response.getEntity().getContent()).getTweets();
					}
					catch (SAXException e) {
						throw new IOException("Failed to parse response: " + e.toString(), e);
					}
				}
			});
		}
		catch (IOException e) {
			throw new SuccessWhaleException("Failed to fetch feed '" + feed.toString() + "': " + e.toString(), e); // FIXME does feed have good toString()?
		}
	}

	private boolean authenticated () {
		return this.auth != null;
	}

	private void ensureAuthenticated () throws SuccessWhaleException {
		if (!authenticated()) authenticate();
	}

	public static void checkReponseCode(final StatusLine statusLine, final int code) throws IOException {
		if (statusLine.getStatusCode() != code) {
			throw new IOException("Server returned " + statusLine.getStatusCode() + ": " + statusLine.getReasonPhrase());
		}
	}

	private static class SuccessWhaleAuth {

		public final String userid;
		public final String secret;

		public SuccessWhaleAuth (final String userid, final String secret) {
			this.userid = userid;
			this.secret = secret;
		}

	}

}

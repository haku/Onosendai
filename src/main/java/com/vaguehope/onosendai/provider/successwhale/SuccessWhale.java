package com.vaguehope.onosendai.provider.successwhale;

import java.io.IOException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.TweetList;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * https://github.com/ianrenton/successwhale-api/blob/master/APIDOCS.md
 */
public class SuccessWhale {

	private static final String BASE_URL = "http://api.successwhale.com"; // FIXME this should really be HTTPS with a preset trust store.
	private static final String API_AUTH = "/v3/authenticate.json";
	private static final String API_FEED = "/v3/feed.xml";

	private final LogWrapper log = new LogWrapper("SW");
	private final Account account;

	private String userid = null;
	private String secret = null;

	public SuccessWhale (final Account account) {
		this.account = account;
	}

	/**
	 * FIXME lock against multiple calls.
	 */
	public void authenticate () throws SuccessWhaleException {
		String username = this.account.accessToken;
		String password = this.account.accessSecret;
		try {
			StringBuilder params = new StringBuilder();
			params
					.append("username=").append(URLEncoder.encode(username, "UTF-8"))
					.append("&password=").append(URLEncoder.encode(password, "UTF-8"));
			String authRespRaw = HttpHelper.getUrlContent(BASE_URL + API_AUTH, "POST", params.toString());

			JSONObject authResp = (JSONObject) new JSONTokener(authRespRaw).nextValue();

			// FIXME it would be better if the API used HTTP status codes to indicate failure.
			// HTTP 200 with 'failed' in the body is somewhat untidy.
			if (!authResp.getBoolean("success")) throw new SuccessWhaleException("Auth rejected for user '" + username + "'.");

			this.userid = authResp.getString("userid");
			this.secret = authResp.getString("secret");
			this.log.i("Authenticated username='%s' userid='%s'.", username, this.userid);
		}
		catch (IOException e) {
			throw new SuccessWhaleException("Auth failed for user '" + username + "'.", e);
		}
		catch (JSONException e) {
			throw new SuccessWhaleException("Auth response unparsable.", e);
		}
	}

	public TweetList getFeed (final SuccessWhaleFeed feed) throws SuccessWhaleException {
		ensureAuthenticated();
		try {
			StringBuilder params = new StringBuilder();
			params
					// TODO how is auth passed?
					.append("sources=").append(URLEncoder.encode(feed.getSources(), "UTF-8"));

			// TODO use SAX parser.
			String feedRespRaw = HttpHelper.getUrlContent(BASE_URL + API_FEED, "GET", params.toString());

			// TODO

			throw new UnsupportedOperationException("Not impl.");
		}
		catch (IOException e) {
			throw new SuccessWhaleException("Failed to fetch feed '" + feed.toString() + "'.", e); // FIXME does feed have good toString()?
		}
	}

	private boolean authenticated () {
		return this.userid != null && this.secret != null;
	}

	private void ensureAuthenticated () throws SuccessWhaleException {
		if (!authenticated()) authenticate();
	}

}

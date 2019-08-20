package com.vaguehope.onosendai.provider.twitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.StringHelper;

public final class TwitterOauth {

	public static final String CALLBACK_URL = "http://vaguehope.com/onosendai/nullcallback";

	public static final String IEXTRA_AUTH_URL = "auth_url";
	public static final String IEXTRA_OAUTH_VERIFIER = "oauth_verifier";
	public static final String IEXTRA_OAUTH_TOKEN = "oauth_token";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String RES_PATH = "/api_twitter";
	private static final String DEF_KEY = "API_TWITTER_CONSUMER_KEY";
	private static final String DEF_SECRET = "API_TWITTER_CONSUMER_SECRET";

	private static boolean read = false;
	private static String key;
	private static String secret;

	private TwitterOauth () {
		throw new AssertionError();
	}

	public static String getConsumerKey () {
		read();
		return key;
	}

	public static String getConsumerSecret () {
		read();
		return secret;
	}

	private static void read () {
		if (read) return;
		final BufferedReader r = new BufferedReader(new InputStreamReader(TwitterOauth.class.getResourceAsStream(RES_PATH), Charset.forName("UTF-8")));
		try {
			key = r.readLine();
			secret = r.readLine();
			if (StringHelper.isEmpty(key) || StringHelper.isEmpty(secret)
					|| DEF_KEY.equals(key) || DEF_SECRET.equals(secret))
				throw new IllegalStateException("API keys are missing.");
			read = true;
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read internal API resource.", e);
		}
		finally {
			IoHelper.closeQuietly(r);
		}
	}

}

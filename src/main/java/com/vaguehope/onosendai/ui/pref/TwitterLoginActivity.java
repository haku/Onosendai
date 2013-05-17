package com.vaguehope.onosendai.ui.pref;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.vaguehope.onosendai.provider.twitter.TwitterOauth;

/**
 * With thanks to Yoshiki for example at:
 * https://github.com/yoshiki/android-twitter-oauth-demo
 */
public class TwitterLoginActivity extends Activity {

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate (final Bundle bundle) {
		super.onCreate(bundle);

		final WebView webView = new WebView(this);
		setContentView(webView);

		final WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading (final WebView view, final String url) {
				boolean result = true;
				if (url != null && url.startsWith(TwitterOauth.CALLBACK_URL)) {
					final Uri uri = Uri.parse(url);
					if (uri.getQueryParameter("denied") != null) {
						setResult(RESULT_CANCELED);
						finish();
					}
					else {
						final String oauthToken = uri.getQueryParameter("oauth_token");
						final String oauthVerifier = uri.getQueryParameter("oauth_verifier");

						final Intent intent = getIntent();
						intent.putExtra(TwitterOauth.IEXTRA_OAUTH_TOKEN, oauthToken);
						intent.putExtra(TwitterOauth.IEXTRA_OAUTH_VERIFIER, oauthVerifier);

						setResult(RESULT_OK, intent);
						finish();
					}
				}
				else {
					result = super.shouldOverrideUrlLoading(view, url);
				}
				return result;
			}
		});
		webView.loadUrl(this.getIntent().getExtras().getString("auth_url"));
	}

}

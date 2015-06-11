package com.vaguehope.onosendai.ui.pref;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.vaguehope.onosendai.provider.twitter.TwitterOauth;
import com.vaguehope.onosendai.util.LogWrapper;

/**
 * With thanks to Yoshiki for example at:
 * https://github.com/yoshiki/android-twitter-oauth-demo
 */
public class TwitterLoginActivity extends Activity {

	private static final int MAX_PRG = 100;
	private static final LogWrapper LOG = new LogWrapper("TLA");

	public static LogWrapper getLog () {
		return LOG;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate (final Bundle bundle) {
		super.onCreate(bundle);

		CookieSyncManager.createInstance(this);

		final LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);

		final ProgressBar prgBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
		prgBar.setMax(MAX_PRG);
		prgBar.setProgress(0);
		prgBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(prgBar);

		final WebView webView = new WebView(this);
		layout.addView(webView);

		setContentView(layout);

		final WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(false);
		webView.setWebChromeClient(new LoginWebChromeClient(prgBar));
		webView.setWebViewClient(new LoginWebViewClient(this));
		webView.loadUrl(this.getIntent().getExtras().getString(TwitterOauth.IEXTRA_AUTH_URL));
	}

	@Override
	protected void onDestroy () {
		CookieManager.getInstance().removeAllCookie();
		LOG.i("Cleared cookies.");
		super.onDestroy();
	}

	private static class LoginWebChromeClient extends WebChromeClient {

		private final ProgressBar prgBar;

		public LoginWebChromeClient (final ProgressBar prgBar) {
			this.prgBar = prgBar;
		}

		@Override
		public void onProgressChanged (final WebView view, final int newProgress) {
			super.onProgressChanged(view, newProgress);
			if (newProgress < MAX_PRG && this.prgBar.getVisibility() == View.GONE) {
				this.prgBar.setVisibility(View.VISIBLE);
			}
			this.prgBar.setProgress(newProgress);
			if (newProgress == MAX_PRG) {
				this.prgBar.setVisibility(View.GONE);
			}
		}

	}

	private static class LoginWebViewClient extends WebViewClient {

		private final Activity activity;

		public LoginWebViewClient (final Activity activity) {
			this.activity = activity;
		}

		@Override
		public boolean shouldOverrideUrlLoading (final WebView view, final String url) {
			boolean result = true;
			if (url != null && url.startsWith(TwitterOauth.CALLBACK_URL)) {
				final Uri uri = Uri.parse(url);
				if (uri.getQueryParameter("denied") != null) {
					this.activity.setResult(RESULT_CANCELED);
					getLog().i("Twitter login canceled.");
					this.activity.finish();
				}
				else {
					final String oauthToken = uri.getQueryParameter("oauth_token");
					final String oauthVerifier = uri.getQueryParameter("oauth_verifier");

					final Intent intent = this.activity.getIntent();
					intent.putExtra(TwitterOauth.IEXTRA_OAUTH_TOKEN, oauthToken);
					intent.putExtra(TwitterOauth.IEXTRA_OAUTH_VERIFIER, oauthVerifier);

					this.activity.setResult(RESULT_OK, intent);
					getLog().i("Twitter login successful.");
					this.activity.finish();
				}
			}
			else {
				result = super.shouldOverrideUrlLoading(view, url);
			}
			return result;
		}
	}

}

package com.vaguehope.onosendai.provider.instapaper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.NetworkType;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.util.HttpClientFactory;
import com.vaguehope.onosendai.util.StringHelper;

public class InstapaperProvider {

	private final AtomicReference<Instapaper> clientRef;
	private final HttpClientFactory httpClientFactory;

	public InstapaperProvider () {
		this.clientRef = new AtomicReference<Instapaper>();
		this.httpClientFactory = new HttpClientFactory();
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

	private Instapaper getAccount (final Account account) {
		final Instapaper client = this.clientRef.get();
		if (client != null && client.getAccount().equals(account)) {
			return client;
		}
		else if (client == null) {
			final Instapaper instpaper = new Instapaper(account, this.httpClientFactory);
			if (!this.clientRef.compareAndSet(null, instpaper)) throw new IllegalStateException("Only one instapaper account at a time is supported.");
			return instpaper;
		}
		throw new IllegalStateException("Only one instapaper account at a time is supported.");
	}

	public void add (final Account account, final Tweet tweet) throws IOException {
		final Meta linkMeta = findLink(tweet);
		final String linkUrl = linkMeta != null && linkMeta.hasData() ? linkMeta.getData() : null;
		final String linkTitle = linkMeta != null && linkMeta.hasTitle() ? linkMeta.getTitle() : null;

		final NetworkType networkType = findNetworkType(tweet);
		final String title;
		final String tweetUrl;
		if (networkType == NetworkType.FACEBOOK) {
			title = (linkTitle != null ? linkTitle + " via " : "Post by ")
					+ StringHelper.firstLine(tweet.getFullname());
			tweetUrl = facebookUrlOf(tweet);
		}
		else if (tweet.getSid() != null) {
			title = (linkTitle != null ? linkTitle + " via " : "Tweet by ")
					+ StringHelper.firstLine(tweet.getFullname()) + " (@" + tweet.getUsername() + ")";
			tweetUrl = twitterUrlOf(tweet);
		}
		else {
			title = linkUrl != null ? linkTitle : "Text saved by you"; // Leave title null so Instapaper fills it in.
			tweetUrl = null;
		}

		final String url;
		final String summary;
		if (linkUrl != null) {
			url = linkUrl;
			summary = tweet.getBody() + (tweetUrl != null ? "\n[" + tweetUrl + "]" : "");
		}
		else {
			url = tweetUrl != null ? tweetUrl : "http://localhost/nourl/" + System.currentTimeMillis();
			summary = tweet.getBody();
		}

		getAccount(account).add(url, title, summary);
	}

	private static Meta findLink (final Tweet tweet) {
		final Meta meta = tweet.getFirstMetaOfType(MetaType.URL);
		if (meta != null) return meta;
		final Matcher m = StringHelper.URL_PATTERN.matcher(tweet.getBody());
		while (m.find()) {
			String g = m.group();
			if (g.startsWith("(") && g.endsWith(")")) g = g.substring(1, g.length() - 1);
			return new Meta(MetaType.URL, g);
		}
		return null;
	}

	private static NetworkType findNetworkType (final Tweet tweet) {
		final Meta svcMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
		final ServiceRef serviceRef = svcMeta != null ? ServiceRef.parseServiceMeta(svcMeta) : null;
		return serviceRef != null ? serviceRef.getType() : null;
	}

	private static String twitterUrlOf (final Tweet tweet) {
		return "https://twitter.com/" + tweet.getUsername() + "/status/" + tweet.getSid();
	}

	private static String facebookUrlOf (final Tweet tweet) {
		final String tweetSid = tweet.getSid();
		final int x = tweetSid.indexOf('_');
		if (x > 0) {
			final String userId = tweetSid.substring(0, x);
			final String statusId = tweetSid.substring(x + 1);
			return "https://www.facebook.com/" + userId + "/posts/" + statusId;
		}
		return "http://localhost/invalidfacebookpostid/uid/" + tweet.getSid();
	}

}

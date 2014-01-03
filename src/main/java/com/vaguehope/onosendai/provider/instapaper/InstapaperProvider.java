package com.vaguehope.onosendai.provider.instapaper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

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
		final Meta svcMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
		final ServiceRef serviceRef = svcMeta != null ? ServiceRef.parseServiceMeta(svcMeta) : null;
		final NetworkType networkType = serviceRef != null ? serviceRef.getType() : null;

		final String tweetUrl;
		if (serviceRef != null && networkType == NetworkType.FACEBOOK) {
			final String serviceUid = serviceRef.getUid();
			final String tweetSid = tweet.getSid();
			if (tweetSid.length() > serviceUid.length() + 2 && tweetSid.startsWith(serviceUid + "_")) {
				final String statusId = tweetSid.substring(serviceUid.length() + 1);
				tweetUrl = "https://www.facebook.com/" + serviceUid + "/post/" + statusId;
			}
			else {
				tweetUrl = "http://example.com/" + serviceRef.getRawType() + "/uid/" + tweet.getSid();
			}
		}
		else {
			tweetUrl = "https://twitter.com/" + tweet.getUsername() + "/status/" + tweet.getSid();
		}

		final Meta linkMeta = tweet.getFirstMetaOfType(MetaType.URL);

		final String url = linkMeta != null && !StringHelper.isEmpty(linkMeta.getData())
				? linkMeta.getData()
				: tweetUrl;

		final String title = (linkMeta != null && !StringHelper.isEmpty(linkMeta.getTitle())
				? linkMeta.getTitle() + " via "
				: (networkType == NetworkType.FACEBOOK ? "Post" : "Tweet") + " by ")
				+ StringHelper.firstLine(tweet.getFullname()) + (tweet.getUsername() != null ? " (@" + tweet.getUsername() + ")" : "");

		final String summary = url.equals(tweetUrl)
				? tweet.getBody()
				: tweet.getBody() + "\n[" + tweetUrl + "]";

		getAccount(account).add(url, title, summary);
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

}

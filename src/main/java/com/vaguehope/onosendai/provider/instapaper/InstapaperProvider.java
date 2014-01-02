package com.vaguehope.onosendai.provider.instapaper;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.HttpClientFactory;

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

	public void add (final Account account, final String url, final String title, final String body) throws IOException {
		getAccount(account).add(url, title, body);
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

}

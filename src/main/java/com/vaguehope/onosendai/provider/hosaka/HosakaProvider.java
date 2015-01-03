package com.vaguehope.onosendai.provider.hosaka;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONException;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.HttpClientFactory;

public class HosakaProvider {

	private final AtomicReference<Hosaka> clientRef;
	private final HttpClientFactory httpClientFactory;

	public HosakaProvider () {
		this.clientRef = new AtomicReference<Hosaka>();
		this.httpClientFactory = new HttpClientFactory();
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

	private Hosaka getAccount (final Account account) {
		final Hosaka client = this.clientRef.get();
		if (client != null && client.getAccount().equals(account)) {
			return client;
		}
		else if (client == null) {
			final Hosaka hosaka = new Hosaka(account, this.httpClientFactory);
			if (!this.clientRef.compareAndSet(null, hosaka)) throw new IllegalStateException("Only one Hosaka account at a time is supported.");
			return hosaka;
		}
		throw new IllegalStateException("Only one Hosaka account at a time is supported.");
	}

	public void testAccountLogin (final Account account) throws IOException {
		getAccount(account).testLogin();
	}

	public Map<String, HosakaColumn> sendColumns (final Account account, final Map<String, HosakaColumn> columns) throws IOException, JSONException {
		return getAccount(account).sendColumns(columns);
	}

}

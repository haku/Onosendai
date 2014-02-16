package com.vaguehope.onosendai.provider.bufferapp;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.ServiceRef;
import com.vaguehope.onosendai.util.HttpClientFactory;

public class BufferAppProvider {

	private final ConcurrentMap<String, BufferApp> accounts;
	private final HttpClientFactory httpClientFactory;

	public BufferAppProvider () {
		this.accounts = new ConcurrentHashMap<String, BufferApp>();
		this.httpClientFactory = new HttpClientFactory();
	}

	public void addAccount (final Account account) {
		if (this.accounts.containsKey(account.getId())) return;
		final BufferApp b = new BufferApp(account, this.httpClientFactory);
		this.accounts.putIfAbsent(account.getId(), b);
	}

	private BufferApp getAccount (final Account account) {
		final BufferApp a = this.accounts.get(account.getId());
		if (a != null) return a;
		addAccount(account);
		return this.accounts.get(account.getId());
	}

	public List<ServiceRef> getPostToAccounts (final Account account) throws BufferAppException {
		return getAccount(account).getPostToAccounts();
	}

	public void post (final Account account, final Set<ServiceRef> postToSvc, final String body) throws BufferAppException {
		getAccount(account).post(postToSvc, body);
	}

	public void shutdown () {
		this.httpClientFactory.shutdown();
	}

}

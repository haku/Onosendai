package com.vaguehope.onosendai.config;

public class Account {

	private final String id;
	private final AccountProvider provider;
	private final String consumerKey;
	private final String consumerSecret;
	private final String accessToken;
	private final String accessSecret;

	public Account (final String id, final AccountProvider provider, final String consumerKey, final String consumerSecret, final String accessToken, final String accessSecret) {
		this.id = id;
		this.provider = provider;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessSecret = accessSecret;
	}

	public String toHumanString() {
		return String.format("%s (%s)", this.getProvider().toHumanString(), this.getId());
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Account{").append(this.getId())
				.append(",").append(this.getProvider())
				.append("}");
		return s.toString();
	}

	public String getId () {
		return this.id;
	}

	public AccountProvider getProvider () {
		return this.provider;
	}

	public String getConsumerKey () {
		return this.consumerKey;
	}

	public String getConsumerSecret () {
		return this.consumerSecret;
	}

	public String getAccessToken () {
		return this.accessToken;
	}

	public String getAccessSecret () {
		return this.accessSecret;
	}

}
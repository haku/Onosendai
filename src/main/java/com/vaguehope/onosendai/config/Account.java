package com.vaguehope.onosendai.config;

public class Account {

	public final String id;
	public final AccountProvider provider;
	public final String consumerKey;
	public final String consumerSecret;
	public final String accessToken;
	public final String accessSecret;

	public Account (final String id, final AccountProvider provider, final String consumerKey, final String consumerSecret, final String accessToken, final String accessSecret) {
		this.id = id;
		this.provider = provider;
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessSecret = accessSecret;
	}

	public String toHumanString() {
		return String.format("%s (%s)", this.provider.toHumanString(), this.id);
	}

	@Override
	public String toString () {
		StringBuilder s = new StringBuilder();
		s.append("Account{").append(this.id)
				.append(",").append(this.provider)
				.append("}");
		return s.toString();
	}

}
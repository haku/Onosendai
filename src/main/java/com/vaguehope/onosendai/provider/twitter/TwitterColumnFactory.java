package com.vaguehope.onosendai.provider.twitter;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;

public final class TwitterColumnFactory {

	private TwitterColumnFactory () {
		throw new AssertionError();
	}

	public static Column homeTimeline (final int id, final Account account) {
		checkAccount(account);
		return new Column(id, "Home Timeline", account.getId(), MainFeeds.TIMELINE.name(), 30, null, false);
	}

	public static Column mentions (final int id, final Account account) {
		checkAccount(account);
		return new Column(id, "Mentions", account.getId(), MainFeeds.MENTIONS.name(), 30, null, false);
	}

	private static void checkAccount (final Account account) {
		if (account == null) throw new IllegalArgumentException("Account must not be null.");
		if (account.getProvider() != AccountProvider.TWITTER) throw new IllegalArgumentException("Account must be of type Twitter.");
	}

}

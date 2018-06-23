package com.vaguehope.onosendai.provider.twitter;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.InlineMediaStyle;

public final class TwitterColumnFactory {

	private static final int DEFAULT_REFRESH_MINS = 30;

	private TwitterColumnFactory () {
		throw new AssertionError();
	}

	public static Column homeTimeline (final int id, final Account account) {
		checkAccount(account);
		return new Column(id, "Home Timeline", new ColumnFeed(account.getId(), MainFeeds.TIMELINE.name()), DEFAULT_REFRESH_MINS, null, false, null, InlineMediaStyle.NONE, false); //ES
	}

	public static Column mentions (final int id, final Account account) {
		checkAccount(account);
		return new Column(id, "Mentions", new ColumnFeed(account.getId(), MainFeeds.MENTIONS.name()), DEFAULT_REFRESH_MINS, null, false, null, InlineMediaStyle.NONE, false); //ES
	}

	private static void checkAccount (final Account account) {
		if (account == null) throw new IllegalArgumentException("Account must not be null.");
		if (account.getProvider() != AccountProvider.TWITTER) throw new IllegalArgumentException("Account must be of type Twitter.");
	}

}

package com.vaguehope.onosendai.provider.mastodon;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.ColumnFeed;
import com.vaguehope.onosendai.config.InlineMediaStyle;

public class MastodonColumnFactory {

	private static final int DEFAULT_REFRESH_MINS = 30;

	public static Column homeTimeline (final int id, final Account account) {
		checkAccount(account);
		return new Column(id, "Home Timeline", new ColumnFeed(account.getId(), MastodonColumnType.TIMELINE.getResource()), //ES
				DEFAULT_REFRESH_MINS, null, false, null, InlineMediaStyle.INLINE, true);
	}

	public static Column mentions (final int id, final Account account) {
		checkAccount(account);
		return new Column(id, "Mentions", new ColumnFeed(account.getId(), MastodonColumnType.MENTIONS.getResource()), //ES
				DEFAULT_REFRESH_MINS, null, false, null, InlineMediaStyle.INLINE, true);
	}

	private static void checkAccount (final Account account) {
		if (account == null) throw new IllegalArgumentException("Account must not be null.");
		if (account.getProvider() != AccountProvider.MASTODON) throw new IllegalArgumentException("Account must be of type Mastodon.");
	}

}

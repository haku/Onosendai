package com.vaguehope.onosendai.provider.mastodon;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.StringHelper;

public class MastodonUrls {

	/**
	 * Links to toot on user's instance so they can interact with it.
	 */
	public static String toot(final Account account, final Tweet toot) {
		if (account.getProvider() != AccountProvider.MASTODON) {
			throw new IllegalArgumentException("Not a mastodon account: " + account);
		}

		// Valid examples:
		// https://mastodon.lol/@fae/109327493362448378
		// https://mastodon.lol/@fae@mastodon.lol/109327493362448378
		// https://mastodon.lol/@user@example.com/111222333444555666

		final String instanceName = account.getConsumerKey();

		String username = StringHelper.firstLine(toot.getUsername());
		if (!username.startsWith("@")) username = "@" + username;

		return String.format("https://%s/%s/%s",
				instanceName,
				username,
				toot.getSid());
	}

}

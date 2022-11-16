package com.vaguehope.onosendai.provider;

import java.util.Collections;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.OutboxTweet;
import com.vaguehope.onosendai.model.OutboxTweet.OutboxAction;
import com.vaguehope.onosendai.model.Tweet;

public final class OutboxActionFactory {

	private OutboxActionFactory () {
		throw new AssertionError();
	}

	public static OutboxTweet newRt (final Account account, final Tweet tweet) {
		return newAction(OutboxAction.RT, account, tweet.getSid(), tweet);
	}

	public static OutboxTweet newFav (final Account account, final Tweet tweet) {
		return newAction(OutboxAction.FAV, account, tweet.getSid(), tweet);
	}

	public static OutboxTweet newDelete (final Account account, final Tweet tweet) {
		final Meta editSidMeta = tweet.getFirstMetaOfType(MetaType.EDIT_SID);
		if (editSidMeta == null) throw new IllegalStateException("Tried to delete a tweet with out EDIT_SID set: " + tweet);
		final String editSid = editSidMeta.getData();
		return newAction(OutboxAction.DELETE, account, editSid, tweet);
	}

	private static OutboxTweet newAction (final OutboxAction action, final Account account, final String sid, final Tweet tweet) {
		switch (account.getProvider()) {
			case TWITTER:
			case MASTODON:
				return new OutboxTweet(action, account, null, actionBody(action, tweet), sid, null)
						.setPending();
			case SUCCESSWHALE:
				final Meta svcMeta = tweet.getFirstMetaOfType(MetaType.SERVICE);
				if (svcMeta != null) {
					final ServiceRef svc = ServiceRef.parseServiceMeta(svcMeta);
					if (svc != null) {
						return new OutboxTweet(action, account, Collections.singleton(svc), actionBody(action, tweet), sid, null)
								.setPending();
					}
					throw new IllegalStateException("Invalid service metadata: " + svcMeta.getData());
				}
				throw new IllegalStateException("Service metadata missing from: " + tweet);
			default:
				throw new UnsupportedOperationException("Do not know how to RT via account type: " + account.getUiTitle());
		}
	}

	private static String actionBody (final OutboxAction action, final Tweet tweet) {
		return String.format("%s:\n%s", action, tweet.getBody());
	}

}

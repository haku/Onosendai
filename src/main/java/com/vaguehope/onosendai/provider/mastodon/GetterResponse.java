package com.vaguehope.onosendai.provider.mastodon;

import java.util.Collection;
import java.util.Collections;

import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Notification;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;

public abstract class GetterResponse<T> {

	protected final Pageable<T> pageable;

	public GetterResponse (final Pageable<T> pageable) {
		this.pageable = pageable;
	}

	public Range nextRange (final int limit) {
		return this.pageable.nextRange(limit);
	}

	public int size() {
		return this.pageable.getPart().size();
	}

	public abstract void addTweetsTo (final Collection<Tweet> tweets, final Account account, long ownId);

	public static class StatusGetterResponse extends GetterResponse<Status> {
		public StatusGetterResponse (final Pageable<Status> pageable) {
			super(pageable);
		}

		@Override
		public void addTweetsTo (final Collection<Tweet> addTo, final Account account, final long ownId) {
			for (final Status status : this.pageable.getPart()) {
				addTo.add(MastodonUtils.convertStatusToTweet(account, status, ownId, null));
			}
		}
	}

	public static class NotificationGetterResponse extends GetterResponse<Notification> {
		public NotificationGetterResponse (final Pageable<Notification> pageable) {
			super(pageable);
		}

		@Override
		public void addTweetsTo (final Collection<Tweet> addTo, final Account account, final long ownId) {
			for (final Notification notif : this.pageable.getPart()) {
				final Status s = notif.getStatus();
				if (s == null) continue;

				final Meta notifIdMeta = new Meta(MetaType.NOTIFICAITON_ID, notif.getId());
				addTo.add(MastodonUtils.convertStatusToTweet(account, s, ownId, Collections.singleton(notifIdMeta)));
			}
		}
	}

}

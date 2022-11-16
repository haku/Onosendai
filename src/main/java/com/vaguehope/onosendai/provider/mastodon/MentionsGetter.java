package com.vaguehope.onosendai.provider.mastodon;

import java.util.ArrayList;
import java.util.List;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Notification;
import com.sys1yagi.mastodon4j.api.entity.Notification.Type;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Notifications;

// https://docs.joinmastodon.org/methods/notifications/

public class MentionsGetter implements MastodonFeedGetter {

	private Notifications notifications;

	@Override
	public void setClient (final MastodonClient client) {
		this.notifications = new Notifications(client);
	}

	@Override
	public Pageable<Status> makeRequest (final Range range) throws Mastodon4jRequestException {
		if (this.notifications == null) throw new IllegalStateException("setClient() not called.");

		final List<Type> excludeTypes = new ArrayList<Type>();
		excludeTypes.add(Type.Reblog);
		excludeTypes.add(Type.Favourite);
		excludeTypes.add(Type.Follow);

		final MastodonRequest<Pageable<Notification>> req = this.notifications.getNotifications(range, excludeTypes);
		final Pageable<Notification> resp = req.execute();

		final List<Status> statuses = new ArrayList<Status>();
		for (final Notification n : resp.getPart()) {
			final Status s = n.getStatus();
			if (s != null) statuses.add(s);
		}

		return new Pageable<Status>(statuses, resp.getLink());
	}

	@Override
	public String toString () {
		return "MentionsGetter()";
	}

}

package com.vaguehope.onosendai.provider.mastodon;

import java.util.ArrayList;
import java.util.List;

import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Notification;
import com.sys1yagi.mastodon4j.api.entity.Notification.Type;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Notifications;
import com.vaguehope.onosendai.model.SinceIdType;

// https://docs.joinmastodon.org/methods/notifications/

public class MentionsGetter implements MastodonFeedGetter {

	private Notifications notifications;

	@Override
	public void setClient (final MastodonClient client) {
		this.notifications = new Notifications(client);
	}

	@Override
	public GetterResponse<?> makeRequest (final Range range, final boolean manualRefresh) throws Mastodon4jRequestException {
		if (this.notifications == null) throw new IllegalStateException("setClient() not called.");

		final List<Type> excludeTypes = new ArrayList<Type>();
		excludeTypes.add(Type.Reblog);
		excludeTypes.add(Type.Favourite);
		excludeTypes.add(Type.Follow);

		final MastodonRequest<Pageable<Notification>> req = this.notifications.getNotifications(range, excludeTypes);
		final Pageable<Notification> pageable = req.execute();
		return new GetterResponse.NotificationGetterResponse(pageable);
	}

	@Override
	public SinceIdType getSinceIdType () {
		return SinceIdType.NOTIFICAITON_ID_META;
	}

	@Override
	public String toString () {
		return "MentionsGetter()";
	}

}

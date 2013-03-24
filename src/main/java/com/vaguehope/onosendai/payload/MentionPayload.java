package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.ui.PostActivity;
import com.vaguehope.onosendai.util.EqualHelper;

public class MentionPayload extends Payload {

	private final int columnId;
	private final String screenName;

	public MentionPayload (final int columnId, final Tweet ownerTweet, final Meta meta) {
		this(columnId, ownerTweet, '@' + meta.getData());
	}

	public MentionPayload (final int columnId, final Tweet ownerTweet, final String screenName) {
		super(ownerTweet, PayloadType.MENTION);
		this.columnId = columnId;
		this.screenName = screenName;
	}

	public String getScreenName () {
		return this.screenName;
	}

	@Override
	public String getTitle () {
		return this.screenName;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		final Intent intent = new Intent(context, PostActivity.class);
		intent.putExtra(PostActivity.ARG_COLUMN_ID, this.columnId);
		intent.putExtra(PostActivity.ARG_IN_REPLY_TO, getOwnerTweet().getId());
		return intent;
	}

	@Override
	public int hashCode () {
		return this.screenName.hashCode();
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof MentionPayload)) return false;
		MentionPayload that = (MentionPayload) o;
		return EqualHelper.equal(this.screenName, that.screenName);
	}

}

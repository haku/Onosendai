package com.vaguehope.onosendai.payload;

import java.util.Arrays;

import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.ui.PostActivity;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.EqualHelper;

/**
 * Note that usernames in here never contain the '@'.
 */
public class MentionPayload extends Payload {

	private final int columnId;
	private final String screenName;
	private final String[] alsoMentions;

	private String titleCache;

	public MentionPayload (final int columnId, final Tweet ownerTweet, final Meta meta) {
		this(columnId, ownerTweet, meta.getData());
	}

	public MentionPayload (final int columnId, final Tweet ownerTweet, final String screenName) {
		this(columnId, ownerTweet, screenName, (String[]) null);
	}

	public MentionPayload (final int columnId, final Tweet ownerTweet, final String screenName, final String... alsoMentions) {
		super(ownerTweet, PayloadType.MENTION);
		this.columnId = columnId;
		this.screenName = screenName;
		this.alsoMentions = alsoMentions;
	}

	@Override
	public String getTitle () {
		if (this.titleCache == null) {
			StringBuilder sb = new StringBuilder("@").append(this.screenName);
			if (this.alsoMentions != null) {
				for (String mention : this.alsoMentions) {
					sb.append(", @").append(mention);
				}
			}
			this.titleCache = sb.toString();
		}
		return this.titleCache;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		final Intent intent = new Intent(context, PostActivity.class);
		intent.putExtra(PostActivity.ARG_COLUMN_ID, this.columnId);

		if (this.screenName.equalsIgnoreCase(getOwnerTweet().getUsername())) {
			intent.putExtra(PostActivity.ARG_IN_REPLY_TO, getOwnerTweet().getId());
			if (this.alsoMentions != null) intent.putExtra(PostActivity.ARG_ALSO_MENTIONS, this.alsoMentions);
		}
		else {
			final String[] mentions = ArrayHelper.joinArrays(String.class, new String[] { this.screenName }, this.alsoMentions);
			intent.putExtra(PostActivity.ARG_ALSO_MENTIONS, mentions);
		}

		return intent;
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.screenName == null) ? 0 : this.screenName.hashCode());
		result = prime * result + ((this.alsoMentions == null) ? 0 : Arrays.hashCode(this.alsoMentions));
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof MentionPayload)) return false;
		MentionPayload that = (MentionPayload) o;
		return EqualHelper.equal(this.screenName, that.screenName)
				&& Arrays.equals(this.alsoMentions, that.alsoMentions);
	}

}

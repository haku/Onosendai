package com.vaguehope.onosendai.payload;

import java.util.Arrays;

import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.ui.PostActivity;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.EqualHelper;

/**
 * Note that usernames in here never contain the '@'.
 */
public class MentionPayload extends Payload {

	private final Account account;
	private final String screenName;
	private final String fullName;
	private final String[] alsoMentions;

	private String titleCache;

	public MentionPayload (final Account account, final Tweet ownerTweet, final Meta meta) {
		this(account, ownerTweet, meta.getData(), meta.getTitle());
	}

	public MentionPayload (final Account account, final Tweet ownerTweet, final String screenName, final String fullName) {
		this(account, ownerTweet, screenName, fullName, (String[]) null);
	}

	public MentionPayload (final Account account, final Tweet ownerTweet, final String screenName, final String fullName, final String... alsoMentions) {
		super(ownerTweet, PayloadType.MENTION);
		this.account = account;
		this.screenName = screenName;
		this.fullName = fullName;
		this.alsoMentions = alsoMentions;
	}

	@Override
	public String getTitle () {
		if (this.titleCache == null) {
			final StringBuilder sb = new StringBuilder("@").append(this.screenName);
			if (this.alsoMentions != null) {
				for (String mention : this.alsoMentions) {
					sb.append(", @").append(mention);
				}
			}
			else if (this.fullName != null) {
				sb.append(" (").append(this.fullName).append(")");
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
		intent.putExtra(PostActivity.ARG_ACCOUNT_ID, this.account.getId());

		if (this.screenName.equalsIgnoreCase(getOwnerTweet().getUsername())) {
			intent.putExtra(PostActivity.ARG_IN_REPLY_TO_UID, getOwnerTweet().getUid());
			intent.putExtra(PostActivity.ARG_IN_REPLY_TO_SID, getOwnerTweet().getSid());
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

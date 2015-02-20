package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.ui.PostActivity;
import com.vaguehope.onosendai.util.EqualHelper;

public class AddCommentPayload extends Payload {

	private final Account account;

	public AddCommentPayload (final Account account, final Tweet ownerTweet) {
		super(ownerTweet, null, PayloadType.COMMENT);
		this.account = account;
	}

	@Override
	public String getTitle () {
		return "Add Comment"; //ES
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		final Intent intent = new Intent(context, PostActivity.class);
		intent.putExtra(PostActivity.ARG_ACCOUNT_ID, this.account.getId());
		intent.putExtra(PostActivity.ARG_IN_REPLY_TO_UID, getOwnerTweet().getUid());
		intent.putExtra(PostActivity.ARG_IN_REPLY_TO_SID, getOwnerTweet().getSid());

		final Meta replyToId = getOwnerTweet().getFirstMetaOfType(MetaType.REPLYTO);
		if (replyToId != null) intent.putExtra(PostActivity.ARG_ALT_REPLY_TO_SID, replyToId.getData());

		return intent;
	}

	@Override
	public int hashCode () {
		return this.account != null ? this.account.getId() != null ? this.account.getId().hashCode() : 0 : 0;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof AddCommentPayload)) return false;
		final AddCommentPayload that = (AddCommentPayload) o;
		return EqualHelper.equal(this.getOwnerTweet(), that.getOwnerTweet()) &&
				EqualHelper.equal(this.account, that.account);
	}
}

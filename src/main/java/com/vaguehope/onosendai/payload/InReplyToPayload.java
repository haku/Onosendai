package com.vaguehope.onosendai.payload;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.EqualHelper;

public class InReplyToPayload extends Payload {

	private final Tweet inReplyToTweet;

	public InReplyToPayload (final Tweet ownerTweet, final Tweet inReplyToTweet) {
		super(ownerTweet, PayloadType.INREPLYTO);
		this.inReplyToTweet = inReplyToTweet;
	}

	public Tweet getInReplyToTweet () {
		return this.inReplyToTweet;
	}

	@Override
	public String getTitle () {
		return String.format("tweet[%s]", this.inReplyToTweet.getSid());
	}

	@Override
	public PayloadLayout getLayout () {
		return PayloadLayout.TWEET;
	}

	@Override
	public PayloadRowView makeRowView (final View view) {
		return new PayloadRowView(
				(TextView) view.findViewById(R.id.txtTweet),
				(ImageView) view.findViewById(R.id.imgMain),
				(TextView) view.findViewById(R.id.txtName));
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		rowView.setText(this.inReplyToTweet.getBody());
		rowView.setSecondaryText(this.inReplyToTweet.getUsername() != null ? this.inReplyToTweet.getUsername() : this.inReplyToTweet.getFullname());

		final String avatarUrl = this.inReplyToTweet.getAvatarUrl();
		if (avatarUrl != null) {
			imageLoader.loadImage(new ImageLoadRequest(avatarUrl, rowView.getImage()));
		}
		else {
			rowView.getImage().setImageResource(R.drawable.question_blue);
		}
	}

	@Override
	public int hashCode () {
		return this.inReplyToTweet.hashCode();
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof InReplyToPayload)) return false;
		final InReplyToPayload that = (InReplyToPayload) o;
		return EqualHelper.equal(this.inReplyToTweet, that.inReplyToTweet);
	}

}

package com.vaguehope.onosendai.payload;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.EqualHelper;

public class PlaceholderPayload extends Payload {

	private final CharSequence msg;
	private final boolean showSpinner;

	public PlaceholderPayload (final Tweet ownerTweet, final CharSequence msg) {
		this(ownerTweet, msg, false);
	}

	public PlaceholderPayload (final Tweet ownerTweet, final CharSequence msg, final boolean showSpinner) {
		super(ownerTweet, null, PayloadType.PLACEHOLDER);
		this.msg = msg;
		this.showSpinner = showSpinner;
	}

	@Override
	public CharSequence getTitle () {
		return this.msg;
	}

	@Override
	public PayloadLayout getLayout () {
		if (this.showSpinner) return PayloadLayout.TEXT_SPINNER;
		return PayloadLayout.TEXT_ONLY;
	}

	@Override
	public int hashCode () {
		return this.msg.hashCode();
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof PlaceholderPayload)) return false;
		final PlaceholderPayload that = (PlaceholderPayload) o;
		return EqualHelper.equal(this.msg, that.msg);
	}

}

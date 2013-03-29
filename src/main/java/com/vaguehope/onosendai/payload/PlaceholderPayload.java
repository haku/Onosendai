package com.vaguehope.onosendai.payload;

import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.EqualHelper;

public class PlaceholderPayload extends Payload {

	private final String msg;

	public PlaceholderPayload (final Tweet ownerTweet, final String msg) {
		super(ownerTweet, PayloadType.PLACEHOLDER);
		this.msg = msg;
	}

	@Override
	public String getTitle () {
		return this.msg;
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
		PlaceholderPayload that = (PlaceholderPayload) o;
		return EqualHelper.equal(this.msg, that.msg);
	}

}

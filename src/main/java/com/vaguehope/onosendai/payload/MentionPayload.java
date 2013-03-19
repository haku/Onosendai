package com.vaguehope.onosendai.payload;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.util.EqualHelper;

public class MentionPayload extends Payload {

	private final String screenName;

	public MentionPayload (final Meta meta) {
		this('@' + meta.getData());
	}

	public MentionPayload (final String screenName) {
		super(PayloadType.MENTION);
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

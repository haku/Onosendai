package com.vaguehope.onosendai.payload;

import com.vaguehope.onosendai.R;

public enum PayloadLayout {

	TEXT_ONLY(0, R.layout.payloadlistrow),
	TEXT_IMAGE(1, R.layout.payloadmedialistrow),
	TWEET(2, R.layout.tweetlistrow);

	private final int index;
	private final int layout;

	private PayloadLayout (final int index, final int layout) {
		this.index = index;
		this.layout = layout;
	}

	public int getIndex () {
		return this.index;
	}

	public int getLayout () {
		return this.layout;
	}

}

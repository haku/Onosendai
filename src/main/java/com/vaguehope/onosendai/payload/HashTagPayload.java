package com.vaguehope.onosendai.payload;

import android.content.Intent;
import android.net.Uri;

public class HashTagPayload extends Payload {

	private static final String HASHTAG_URL_TEMPLATE = "https://twitter.com/search?q=%s";

	private final String hashtag;

	public HashTagPayload (final String hashtag) {
		super(PayloadType.HASHTAG);
		this.hashtag = hashtag;
	}

	public String getHashtag () {
		return this.hashtag;
	}

	@Override
	public String getTitle () {
		return this.hashtag;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent () {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(String.format(HASHTAG_URL_TEMPLATE, Uri.encode(this.hashtag))));
		return i;
	}

}

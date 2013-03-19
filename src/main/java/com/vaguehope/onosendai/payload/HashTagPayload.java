package com.vaguehope.onosendai.payload;

import android.content.Intent;
import android.net.Uri;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.util.EqualHelper;

public class HashTagPayload extends Payload {

	private static final String HASHTAG_URL_TEMPLATE = "https://twitter.com/search?q=%s";

	private final String hashtag;

	public HashTagPayload (final Meta meta) {
		this('#' + meta.getData());
	}

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

	@Override
	public int hashCode () {
		return this.hashtag.hashCode();
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof HashTagPayload)) return false;
		HashTagPayload that = (HashTagPayload) o;
		return EqualHelper.equal(this.hashtag, that.hashtag);
	}

}

package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.twitter.TwitterUrls;
import com.vaguehope.onosendai.util.EqualHelper;

public class HashTagPayload extends Payload {

	private final String hashtag;

	public HashTagPayload (final Tweet ownerTweet, final Meta meta) {
		this(ownerTweet, meta, '#' + meta.getData());
	}

	public HashTagPayload (final Tweet ownerTweet, final String hashtag) {
		this(ownerTweet, null, hashtag);
	}

	private HashTagPayload (final Tweet ownerTweet, final Meta meta, final String hashtag) {
		super(ownerTweet, meta, PayloadType.HASHTAG);
		this.hashtag = hashtag;
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
	public Intent toIntent (final Context context) {
		final Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(TwitterUrls.hashtag(this.hashtag)));
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
		final HashTagPayload that = (HashTagPayload) o;
		return EqualHelper.equal(this.hashtag, that.hashtag);
	}

}

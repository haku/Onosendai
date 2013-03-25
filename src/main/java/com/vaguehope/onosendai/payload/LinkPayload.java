package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.EqualHelper;

public class LinkPayload extends Payload {

	private final String url;

	public LinkPayload (final Tweet ownerTweet, final Meta meta) {
		this(ownerTweet, meta.getData());
	}

	public LinkPayload (final Tweet ownerTweet, final String url) {
		super(ownerTweet, PayloadType.LINK);
		this.url = url;
	}

	@Override
	public String getTitle () {
		return this.url;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(this.url));
		return i;
	}

	@Override
	public int hashCode () {
		return this.url.hashCode();
	}


	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof LinkPayload)) return false;
		LinkPayload that = (LinkPayload) o;
		return EqualHelper.equal(this.url, that.url);
	}

}

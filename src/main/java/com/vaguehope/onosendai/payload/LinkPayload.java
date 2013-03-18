package com.vaguehope.onosendai.payload;

import android.content.Intent;
import android.net.Uri;

public class LinkPayload extends Payload {

	private final String url;

	public LinkPayload (final String url) {
		super(PayloadType.LINK);
		this.url = url;
	}

	public String getUrl () {
		return this.url;
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
	public Intent toIntent () {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(this.url));
		return i;
	}

}

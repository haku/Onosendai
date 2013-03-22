package com.vaguehope.onosendai.images;

import android.widget.ImageView;

public class ImageLoadRequest {

	private final String url;
	private final ImageView imageView;

	public ImageLoadRequest (final String url, final ImageView imageView) {
		if (url == null) throw new IllegalArgumentException("Missing arg: url.");
		if (imageView == null) throw new IllegalArgumentException("Missing arg: imageView.");
		this.url = url;
		this.imageView = imageView;
	}

	public String getUrl () {
		return this.url;
	}

	public ImageView getImageView () {
		return this.imageView;
	}

}
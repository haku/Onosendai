package com.vaguehope.onosendai.images;

import android.widget.ImageView;

public class ImageLoadRequest {

	private final String url;
	private final ImageView imageView;
	private final ImageLoadListener listener;

	public ImageLoadRequest (final String url, final ImageView imageView) {
		this(url, imageView, null);
	}

	public ImageLoadRequest (final String url, final ImageView imageView, final ImageLoadListener listener) {
		if (url == null) throw new IllegalArgumentException("Missing arg: url.");
		if (imageView == null) throw new IllegalArgumentException("Missing arg: imageView.");
		this.url = url;
		this.imageView = imageView;
		this.listener = listener;
	}

	public String getUrl () {
		return this.url;
	}

	public ImageView getImageView () {
		return this.imageView;
	}

	public void notifyListener() {
		if (this.listener == null) return;
		this.listener.imageLoaded(this);
	}

	public interface ImageLoadListener {
		void imageLoaded (ImageLoadRequest req);
	}

}

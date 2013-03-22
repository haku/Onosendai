package com.vaguehope.onosendai.images;

import android.app.Activity;

public final class ImageLoaderUtils {

	private ImageLoaderUtils () {
		throw new AssertionError();
	}

	public static ImageLoader fromActivity(final Activity activity) {
		if (!(activity instanceof ImageLoader)) throw new IllegalArgumentException("Not an ImageLoader: " + activity);
		return (ImageLoader) activity;
	}

}

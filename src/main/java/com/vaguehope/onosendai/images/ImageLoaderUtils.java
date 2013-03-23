package com.vaguehope.onosendai.images;

import android.app.Activity;
import android.graphics.Bitmap;

import com.vaguehope.onosendai.R;

public final class ImageLoaderUtils {

	private ImageLoaderUtils () {
		throw new AssertionError();
	}

	public static ImageLoader fromActivity (final Activity activity) {
		if (!(activity instanceof ImageLoader)) throw new IllegalArgumentException("Not an ImageLoader: " + activity);
		return (ImageLoader) activity;
	}

	public static void loadImage (final BitmapCache<String> cache, final ImageLoadRequest req) {
		final Bitmap bmp = cache.get(req.getUrl());
		if (bmp != null) {
			req.getImageView().setImageBitmap(bmp);
		}
		else {
			req.getImageView().setImageResource(R.drawable.question_blue);
			new ImageFetcherTask(cache).execute(req);
		}
	}

}

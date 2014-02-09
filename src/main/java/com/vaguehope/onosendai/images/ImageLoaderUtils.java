package com.vaguehope.onosendai.images;

import java.util.concurrent.Executor;

import android.app.Activity;
import android.graphics.Bitmap;

public final class ImageLoaderUtils {

	private ImageLoaderUtils () {
		throw new AssertionError();
	}

	public static ImageLoader fromActivity (final Activity activity) {
		if (!(activity instanceof ImageLoader)) throw new IllegalArgumentException("Activity is not an ImageLoader: " + activity);
		return (ImageLoader) activity;
	}

	public static void loadImage (final HybridBitmapCache cache, final ImageLoadRequest req, final Executor exec) {
		if (exec == null) throw new IllegalArgumentException("Must specificy an executor.");
		final Bitmap bmp = cache.quickGet(req.getUrl());
		if (bmp != null) {
			req.setImageBitmap(bmp);
		}
		else {
			req.setImagePending();
			new ImageFetcherTask(cache).executeOnExecutor(exec, req);
		}
	}

}

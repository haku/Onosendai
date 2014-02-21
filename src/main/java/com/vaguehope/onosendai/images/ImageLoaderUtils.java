package com.vaguehope.onosendai.images;

import java.util.concurrent.Executor;

import android.app.Activity;
import android.graphics.Bitmap;

import com.vaguehope.onosendai.util.exec.ExecutorEventListener;

public final class ImageLoaderUtils {

	private ImageLoaderUtils () {
		throw new AssertionError();
	}

	public static ImageLoader fromActivity (final Activity activity) {
		if (!(activity instanceof ImageLoader)) throw new IllegalArgumentException("Activity is not an ImageLoader: " + activity);
		return (ImageLoader) activity;
	}

	public static void loadImage (final HybridBitmapCache cache, final ImageLoadRequest req, final Executor es) {
		loadImage(cache, req, es, es, null);
	}

	public static void loadImage (final HybridBitmapCache cache, final ImageLoadRequest req, final Executor localEs, final Executor netEs, final ExecutorEventListener eventListener) {
		if (localEs == null) throw new IllegalArgumentException("Must specificy a local executor.");
		if (netEs == null) throw new IllegalArgumentException("Must specificy a network executor.");
		final Bitmap bmp = cache.quickGet(req.getUrl());
		if (bmp != null) {
			req.setImageBitmap(bmp);
		}
		else {
			req.setImagePending();
			new ImageLoaderTask(eventListener, cache, netEs, req).executeOnExecutor(localEs);
		}
	}

}

package com.vaguehope.onosendai.images;

import java.util.concurrent.Executor;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.vaguehope.onosendai.images.ImageFetcherTask.ImageFetchResult;
import com.vaguehope.onosendai.util.LogWrapper;

public class ImageLoaderTask extends AsyncTask<ImageLoadRequest, Void, ImageFetchResult> {

	private static final LogWrapper LOG = new LogWrapper("IF");

	private final HybridBitmapCache cache;
	private final Executor netEs;

	public ImageLoaderTask (final HybridBitmapCache cache, final Executor netEs) {
		super();
		this.cache = cache;
		this.netEs = netEs;
	}

	@Override
	protected ImageFetchResult doInBackground (final ImageLoadRequest... reqs) {
		if (reqs.length != 1) throw new IllegalArgumentException("Only one request per task.");
		final ImageLoadRequest req = reqs[0];
		if (!req.isRequired()) return null;
		try {
			final String url = req.getUrl();
			final Bitmap bmp = this.cache.get(url);
			if (bmp != null) return new ImageFetchResult(req, bmp);
			new ImageFetcherTask(this.cache).executeOnExecutor(this.netEs, req);
			return null;
		}
		catch (final Exception e) { // NOSONAR To report errors.
			return new ImageFetchResult(req, e);
		}
	}

	@Override
	protected void onPostExecute (final ImageFetchResult result) {
		if (result == null) return; // Request was no longer required.
		if (result.isSuccess()) {
			result.getRequest().setImageBitmapIfRequired(result.getBmp());
		}
		else {
			LOG.w("Failed to load image '%s': %s", result.getRequest().getUrl(), result.getEmsg());
			result.getRequest().setImageUnavailableIfRequired();
		}
	}

}

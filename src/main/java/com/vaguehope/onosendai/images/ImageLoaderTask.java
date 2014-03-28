package com.vaguehope.onosendai.images;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import android.graphics.Bitmap;

import com.vaguehope.onosendai.images.ImageFetcherTask.ImageFetchResult;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;

public class ImageLoaderTask extends TrackingAsyncTask<Void, Void, ImageFetchResult> {

	private static final LogWrapper LOG = new LogWrapper("IL");

	private final HybridBitmapCache cache;
	private final Executor netEs;
	private final ImageLoadRequest req;

	public ImageLoaderTask (final ExecutorEventListener eventListener, final HybridBitmapCache cache, final Executor netEs, final ImageLoadRequest req) {
		super(eventListener);
		this.cache = cache;
		this.netEs = netEs;
		this.req = req;
	}

	@Override
	public String toString () {
		return "load:" + StringHelper.maxLengthEnd(this.req.getUrl(), 40);
	}

	@Override
	protected ImageFetchResult doInBackgroundWithTracking (final Void... unused) {
		if (!this.req.isRequired()) return null;
		try {
			final String url = this.req.getUrl();
			final Bitmap bmp = this.cache.get(url);
			if (bmp != null) return new ImageFetchResult(this.req, bmp);
			new ImageFetcherTask(getEventListener(), this.cache, this.req).executeOnExecutor(this.netEs);
			return null;
		}
		catch (final Exception e) { // NOSONAR To report errors.
			return new ImageFetchResult(this.req, e);
		}
		catch (final Throwable e) { // NOSONAR To report errors.
			return new ImageFetchResult(this.req, new ExecutionException("Failed to load image.", e));
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

package com.vaguehope.onosendai.images;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import android.graphics.Bitmap;

import com.vaguehope.onosendai.images.HybridBitmapCache.LoadListener;
import com.vaguehope.onosendai.images.ImageFetcherTask.ImageFetchResult;
import com.vaguehope.onosendai.util.IoHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;

public class ImageLoaderTask extends TrackingAsyncTask<Void, String, ImageFetchResult> implements LoadListener {

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
	protected void onPreExecute () {
		this.req.setLoadingProgressIfRequired("loading");
	}

	@Override
	protected void onProgressUpdate (final String... values) {
		if (values == null || values.length < 1) return;
		this.req.setLoadingProgressIfRequired(values[0]);
	}

	/**
	 * Called on BG thread.
	 */
	@Override
	public void onContentLengthToLoad (final long contentLength) {
		publishProgress("loading " + IoHelper.readableFileSize(contentLength));
	}

	@Override
	public void onContentLengthToFetch (final long contentLength) {/* Unused */}

	@Override
	public void onContentFetching (final int bytesFetched, final int contentLength) {/* Unused */}

	@Override
	protected ImageFetchResult doInBackgroundWithTracking (final Void... unused) {
		if (!this.req.isRequired()) return null;
		try {
			publishProgress("loading");
			final String url = this.req.getUrl();
			final Bitmap bmp = this.cache.get(url, this.req.getReqWidth(), this);
			if (bmp != null) return new ImageFetchResult(this.req, bmp);
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
		if (result == null) {
			if (this.req.isRequired()) new ImageFetcherTask(getEventListener(), this.cache, this.req).executeOnExecutor(this.netEs);
			return;
		}
		if (result.isSuccess()) {
			result.getRequest().setImageBitmapIfRequired(result.getBmp());
		}
		else {
			LOG.w("Failed to load image '%s': %s", result.getRequest().getUrl(), result.getEmsg());
			result.getRequest().setImageUnavailableIfRequired(result.getShortEmsg());
		}
	}

}

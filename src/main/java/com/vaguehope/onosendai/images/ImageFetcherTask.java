package com.vaguehope.onosendai.images;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import android.graphics.Bitmap;

import com.vaguehope.onosendai.images.ImageFetcherTask.ImageFetchResult;
import com.vaguehope.onosendai.util.ExcpetionHelper;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.LogWrapper;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.exec.ExecutorEventListener;
import com.vaguehope.onosendai.util.exec.TrackingAsyncTask;

public class ImageFetcherTask extends TrackingAsyncTask<Void, Void, ImageFetchResult> {

	private static final LogWrapper LOG = new LogWrapper("IF");

	private final HybridBitmapCache cache;
	private final ImageLoadRequest req;

	public ImageFetcherTask (final ExecutorEventListener eventListener, final HybridBitmapCache cache, final ImageLoadRequest req) {
		super(eventListener);
		this.cache = cache;
		this.req = req;
	}

	@Override
	public String toString () {
		return "fetch:" + StringHelper.maxLengthEnd(this.req.getUrl(), 40);
	}

	@Override
	protected ImageFetchResult doInBackgroundWithTracking (final Void... unused) {
		if (!this.req.isRequired()) return null;
		try {
			final String url = this.req.getUrl();
			Bitmap bmp = this.cache.get(url, this.req.getReqWidth());
			if (bmp == null) {
				final Object sync = this.cache.getSyncMgr().getSync(url);
				try {
					synchronized (sync) {
						bmp = this.cache.get(url, this.req.getReqWidth());
						if (bmp == null) bmp = fetchImage(url);
					}
				}
				finally {
					this.cache.getSyncMgr().returnSync(url);
				}
			}
			return new ImageFetchResult(this.req, bmp);
		}
		catch (Exception e) { // NOSONAR To report errors.
			return new ImageFetchResult(this.req, e);
		}
		catch (final Throwable e) { // NOSONAR To report errors.
			return new ImageFetchResult(this.req, new ExecutionException("Failed to fetch or load image.", e));
		}
	}

	private Bitmap fetchImage (final String url) throws IOException {
		LOG.d("Fetching image: '%s'...", url);
		return HttpHelper.get(url, this.cache.fromHttp(url, this.req.getReqWidth()));
	}

	@Override
	protected void onPostExecute (final ImageFetchResult result) {
		if (result == null) return; // Request was no longer required.
		if (result.isSuccess()) {
			result.getRequest().setImageBitmapIfRequired(result.getBmp());
		}
		else {
			LOG.w("Failed to fetch image '%s': %s", result.getRequest().getUrl(), result.getEmsg());
			result.getRequest().setImageUnavailableIfRequired();
		}
	}

	protected static class ImageFetchResult {

		private final boolean success;
		private final ImageLoadRequest request;
		private final Bitmap bmp;
		private final Exception e;

		public ImageFetchResult (final ImageLoadRequest request, final Bitmap bmp) {
			if (request == null) throw new IllegalArgumentException("Missing arg: request.");
			this.success = (bmp != null);
			this.request = request;
			this.bmp = bmp;
			this.e = null;
		}

		public ImageFetchResult (final ImageLoadRequest request, final Exception e) {
			if (request == null) throw new IllegalArgumentException("Missing arg: request.");
			if (e == null) throw new IllegalArgumentException("Missing arg: e.");
			this.success = false;
			this.request = request;
			this.bmp = null;
			this.e = e;
		}

		public boolean isSuccess () {
			return this.success;
		}

		public ImageLoadRequest getRequest () {
			return this.request;
		}

		public Bitmap getBmp () {
			return this.bmp;
		}

		public String getEmsg () {
			if (this.e != null) {
				return ExcpetionHelper.causeTrace(this.e, "|");
			}
			return "Invalid response.";
		}

	}

}

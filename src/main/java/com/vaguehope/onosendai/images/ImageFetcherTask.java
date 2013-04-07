package com.vaguehope.onosendai.images;

import java.io.IOException;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.vaguehope.onosendai.images.ImageFetcherTask.ImageFetchResult;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.LogWrapper;

public class ImageFetcherTask extends AsyncTask<ImageLoadRequest, Void, ImageFetchResult> {

	private static final LogWrapper LOG = new LogWrapper("IF");

	private final HybridBitmapCache cache;

	public ImageFetcherTask (final HybridBitmapCache cache) {
		super();
		this.cache = cache;
	}

	@Override
	protected ImageFetchResult doInBackground (final ImageLoadRequest... reqs) {
		if (reqs.length != 1) throw new IllegalArgumentException("Only one request per task.");
		final ImageLoadRequest req = reqs[0];
		if (!req.isRequired()) return null;
		try {
			final String url = req.getUrl();
			Bitmap bmp = this.cache.get(url);
			if (bmp == null) bmp = fetchImage(url);
			return new ImageFetchResult(req, bmp);
		}
		catch (Exception e) { // NOSONAR To report errors.
			return new ImageFetchResult(req, e);
		}
	}

	private Bitmap fetchImage (final String url) throws IOException {
		// FIXME Prevent multiple concurrent fetches for same image?
		LOG.d("Fetching image: '%s'...", url);
		return HttpHelper.get(url, this.cache.fromHttp(url));
	}

	@Override
	protected void onPostExecute (final ImageFetchResult result) {
		if (result == null) return; // Request was no longer required.
		if (result.isSuccess()) {
			result.getRequest().setImageBitmapIfRequired(result.getBmp());
		}
		else {
			LOG.w("Failed to fetch image '%s': %s", result.getRequest().getUrl(), result.getE().toString());
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
			if (bmp == null) throw new IllegalArgumentException("Missing arg: bmp.");
			this.success = true;
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

		public Exception getE () {
			return this.e;
		}

	}

}

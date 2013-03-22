package com.vaguehope.onosendai.images;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageFetcherTask.ImageFetchResult;
import com.vaguehope.onosendai.util.HttpHelper;
import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;
import com.vaguehope.onosendai.util.LogWrapper;

public class ImageFetcherTask extends AsyncTask<ImageLoadRequest, Void, ImageFetchResult> {

	private static final LogWrapper LOG = new LogWrapper("IF");

	private final BitmapCache<String> imageCache;

	public ImageFetcherTask (final BitmapCache<String> imageCache) {
		this.imageCache = imageCache;
	}

	@Override
	protected ImageFetchResult doInBackground (final ImageLoadRequest... reqs) {
		if (reqs.length != 1) throw new IllegalArgumentException("Only one request per task.");
		ImageLoadRequest req = reqs[0];
		try {
			final String url = req.getUrl();
			Bitmap bmp = this.imageCache.get(url);
			if (bmp == null) {
				LOG.d("Fetching image: '%s'...", url);
				bmp = HttpHelper.get(url, ImageStreamHandler.INSTNACE);
				this.imageCache.put(url, bmp);
			}
			return new ImageFetchResult(req, bmp);
		}
		catch (Exception e) { // NOSONAR To report errors.
			return new ImageFetchResult(req, e);
		}
	}

	@Override
	protected void onPostExecute (final ImageFetchResult result) {
		if (result.isSuccess()) {
			result.getRequest().getImageView().setImageBitmap(result.getBmp());
		}
		else {
			LOG.w("Failed to fetch image '%s': %s", result.getRequest().getUrl(), result.getE().toString());
			result.getRequest().getImageView().setImageResource(R.drawable.exclamation_red);
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

	private static enum ImageStreamHandler implements HttpStreamHandler<Bitmap, RuntimeException> {
		INSTNACE;

		@Override
		public Bitmap handleStream (final InputStream is, final int contentLength) throws IOException, RuntimeException {
			return BitmapFactory.decodeStream(is);
		}

	}

}

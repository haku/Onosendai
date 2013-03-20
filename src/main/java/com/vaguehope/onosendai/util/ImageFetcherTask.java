package com.vaguehope.onosendai.util;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.util.HttpHelper.HttpStreamHandler;
import com.vaguehope.onosendai.util.ImageFetcherTask.ImageFetchRequest;
import com.vaguehope.onosendai.util.ImageFetcherTask.ImageFetchResult;

public class ImageFetcherTask extends AsyncTask<ImageFetchRequest, Void, ImageFetchResult> {

	private static final LogWrapper LOG = new LogWrapper("IF");

	@Override
	protected ImageFetchResult doInBackground (final ImageFetchRequest... reqs) {
		if (reqs.length != 1) throw new IllegalArgumentException("Only one request per task.");
		ImageFetchRequest req = reqs[0];
		try {
			LOG.d("Fetching image: '%s'...", req.getUrl());
			Bitmap bmp = HttpHelper.get(req.getUrl(), ImageStreamHandler.INSTNACE);
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

	public static class ImageFetchRequest {

		private final String url;
		private final ImageView imageView;

		public ImageFetchRequest (final String url, final ImageView imageView) {
			if (url == null) throw new IllegalArgumentException("Missing arg: url.");
			if (imageView == null) throw new IllegalArgumentException("Missing arg: imageView.");
			this.url = url;
			this.imageView = imageView;
		}

		public String getUrl () {
			return this.url;
		}

		public ImageView getImageView () {
			return this.imageView;
		}

	}

	protected static class ImageFetchResult {

		private final boolean success;
		private final ImageFetchRequest request;
		private final Bitmap bmp;
		private final Exception e;

		public ImageFetchResult (final ImageFetchRequest request, final Bitmap bmp) {
			if (request == null) throw new IllegalArgumentException("Missing arg: request.");
			if (bmp == null) throw new IllegalArgumentException("Missing arg: bmp.");
			this.success = true;
			this.request = request;
			this.bmp = bmp;
			this.e = null;
		}

		public ImageFetchResult (final ImageFetchRequest request, final Exception e) {
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

		public ImageFetchRequest getRequest () {
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

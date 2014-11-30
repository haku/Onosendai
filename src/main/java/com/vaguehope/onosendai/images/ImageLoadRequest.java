package com.vaguehope.onosendai.images;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.vaguehope.onosendai.R;

public class ImageLoadRequest {

	private final String url;
	private final ImageView imageView;
	private final int reqWidth;
	private final ImageLoadListener listener;
	private final boolean retry;

	public ImageLoadRequest (final String url, final ImageView imageView) {
		this(url, imageView, -1);
	}

	public ImageLoadRequest (final String url, final ImageView imageView, final int reqWidth) {
		this(url, imageView, reqWidth, null);
	}

	public ImageLoadRequest (final String url, final ImageView imageView, final int reqWidth, final ImageLoadListener listener) {
		this(url, imageView, reqWidth, listener, false);
	}

	private ImageLoadRequest (final String url, final ImageView imageView, final int reqWidth, final ImageLoadListener listener, final boolean retry) {
		if (url == null) throw new IllegalArgumentException("Missing arg: url.");
		if (imageView == null) throw new IllegalArgumentException("Missing arg: imageView.");
		this.url = url;
		this.imageView = imageView;
		this.reqWidth = reqWidth;
		this.listener = listener;
		this.retry = retry;
	}

	public ImageLoadRequest withRetry () {
		if (this.retry) return this;
		return new ImageLoadRequest(this.url, this.imageView, this.reqWidth, this.listener, true);
	}

	public String getUrl () {
		return this.url;
	}

	public int getReqWidth () {
		return this.reqWidth;
	}

	public boolean isRetry () {
		return this.retry;
	}

	public void setImagePending () {
		this.imageView.setImageResource(R.drawable.question_blue);
		this.imageView.setTag(R.id.imageLoading, this.url);
		this.imageView.setTag(R.id.imageLoaded, null);
		if (this.listener != null) this.listener.imageFetchProgress(0, 0);
	}

	public void setLoadingProgressIfRequired (final String msg) {
		if (!shouldFinishLoading()) return;
		if (this.listener != null) this.listener.imageLoadProgress(msg);
	}

	public void setFetchingProgressIfRequired (final int progress, final int total) {
		if (!shouldFinishLoading()) return;
		if (this.listener != null) this.listener.imageFetchProgress(progress, total);
	}

	public void setImageUnavailable (final String errMsg) {
		this.imageView.setImageResource(R.drawable.exclamation_red);
		this.imageView.setTag(R.id.imageLoading, null);
		this.imageView.setTag(R.id.imageLoaded, null);
		if (this.listener != null) this.listener.imageLoadFailed(this, errMsg);
	}

	public void setImageUnavailableIfRequired (final String errMsg) {
		if (!shouldFinishLoading()) return;
		setImageUnavailable(errMsg);
	}

	public void setImageBitmap (final Bitmap bmp) {
		this.imageView.setImageBitmap(bmp);
		this.imageView.setTag(R.id.imageLoading, null);
		this.imageView.setTag(R.id.imageLoaded, this.url);
		if (this.listener != null) this.listener.imageLoaded(this);
	}

	public void setImageBitmapIfRequired (final Bitmap bmp) {
		if (!shouldFinishLoading()) return;
		setImageBitmap(bmp);
	}

	public boolean shouldStartLoading () {
		if (this.imageView.getDrawable() == null) return true; // In case something else set imageView to a resource.
		return !this.url.equals(this.imageView.getTag(R.id.imageLoaded));
	}

	public boolean shouldFinishLoading () {
		return this.url.equals(this.imageView.getTag(R.id.imageLoading));
	}

	@Override
	public String toString () {
		return new StringBuilder("ImageLoadRequest{").append(this.url)
				.append(",").append(this.imageView)
				.append(",").append(this.reqWidth)
				.append(",").append(this.listener)
				.append(",").append(this.retry)
				.append("}").toString();
	}

	public interface ImageLoadListener {

		/**
		 * Called with short updates during image loading. Must be called on the
		 * UI thread.
		 */
		void imageLoadProgress (String msg);

		/**
		 * Must be called on the UI thread.
		 */
		void imageFetchProgress (int progress, int total);

		/**
		 * Called only after the image has been successfully loaded. Must be
		 * called on the UI thread.
		 */
		void imageLoaded (ImageLoadRequest req);

		/**
		 * Called with a short error message (like imageLoadProgress). Must be
		 * called on the UI thread.
		 */
		void imageLoadFailed (ImageLoadRequest req, String errMsg);

	}

}

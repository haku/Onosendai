package com.vaguehope.onosendai.images;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.vaguehope.onosendai.R;

public class ImageLoadRequest {

	private final String url;
	private final ImageView imageView;
	private final int reqWidth;
	private final ImageLoadListener listener;

	public ImageLoadRequest (final String url, final ImageView imageView) {
		this(url, imageView, -1);
	}

	public ImageLoadRequest (final String url, final ImageView imageView, final int reqWidth) {
		this(url, imageView, reqWidth, null);
	}

	public ImageLoadRequest (final String url, final ImageView imageView, final int reqWidth, final ImageLoadListener listener) {
		if (url == null) throw new IllegalArgumentException("Missing arg: url.");
		if (imageView == null) throw new IllegalArgumentException("Missing arg: imageView.");
		this.url = url;
		this.imageView = imageView;
		this.reqWidth = reqWidth;
		this.listener = listener;
	}

	public String getUrl () {
		return this.url;
	}

	public int getReqWidth () {
		return this.reqWidth;
	}

	public void setImagePending () {
		this.imageView.setImageResource(R.drawable.question_blue);
		this.imageView.setTag(this.url);
	}

	public void setLoadingProgressIfRequired (final String msg) {
		if (!isRequired()) return;
		if (this.listener != null) this.listener.imageLoadProgress(msg);
	}

	public void setImageUnavailableIfRequired (final String errMsg) {
		if (!isRequired()) return;
		this.imageView.setImageResource(R.drawable.exclamation_red);
		if (this.listener != null) this.listener.imageLoadProgress(errMsg);
	}

	public void setImageBitmap (final Bitmap bmp) {
		this.imageView.setImageBitmap(bmp);
		this.imageView.setTag(null);
		if (this.listener != null) this.listener.imageLoaded(this);
	}

	public void setImageBitmapIfRequired (final Bitmap bmp) {
		if (!isRequired()) return;
		this.imageView.setImageBitmap(bmp);
		if (this.listener != null) this.listener.imageLoaded(this);
	}

	public boolean isRequired () {
		return this.url.equals(this.imageView.getTag());
	}

	public interface ImageLoadListener {

		/**
		 * Called with short updates during image loading.
		 * Must be called on the UI thread.
		 */
		void imageLoadProgress(String msg);

		/**
		 * Called only after the image has been successfully loaded.
		 * Must be called on the UI thread.
		 */
		void imageLoaded (ImageLoadRequest req);

	}

}

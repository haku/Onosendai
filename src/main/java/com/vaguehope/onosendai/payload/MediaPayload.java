package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoadRequest;
import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.EqualHelper;
import com.vaguehope.onosendai.widget.PendingImage;

public class MediaPayload extends Payload {

	private final String imgUrl;
	private final String clickUrl;

	public MediaPayload (final Tweet ownerTweet, final Meta meta) {
		this(ownerTweet, meta.getData(), meta.getTitle());
	}

	public MediaPayload (final Tweet ownerTweet, final String imgUrl, final String clickUrl) {
		super(ownerTweet, PayloadType.MEDIA);
		this.imgUrl = imgUrl;
		this.clickUrl = clickUrl;
	}

	@Override
	public String getTitle () {
		return this.clickUrl != null ? this.clickUrl : this.imgUrl;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		final Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(this.clickUrl != null ? this.clickUrl : this.imgUrl));
		return i;
	}

	@Override
	public PayloadLayout getLayout () {
		return PayloadLayout.TEXT_IMAGE;
	}

	@Override
	public PayloadRowView makeRowView (final View view) {
		return new PayloadRowView(
				(TextView) view.findViewById(R.id.txtMain),
				(PendingImage) view.findViewById(R.id.imgMain),
				(Button) view.findViewById(R.id.btnRetry));
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		super.applyTo(rowView, imageLoader, reqWidth, clickListener);
		rowView.getButton().setVisibility(View.GONE);
		imageLoader.loadImage(new ImageLoadRequest(this.imgUrl, rowView.getImage(), reqWidth, new ImageLoadCallbacks(rowView, imageLoader)));
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.imgUrl == null ? 0 : this.imgUrl.hashCode());
		result = prime * result + (this.clickUrl == null ? 0 : this.clickUrl.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof MediaPayload)) return false;
		final MediaPayload that = (MediaPayload) o;
		return EqualHelper.equal(this.imgUrl, that.imgUrl)
				&& EqualHelper.equal(this.clickUrl, that.clickUrl);
	}

	private static class ImageLoadCallbacks implements ImageLoadListener {

		private final PayloadRowView rowView;
		private final ImageLoader imageLoader;

		public ImageLoadCallbacks (final PayloadRowView rowView, final ImageLoader imageLoader) {
			this.rowView = rowView;
			this.imageLoader = imageLoader;
		}

		@Override
		public void imageLoadProgress (final String msg) {
			final ImageLoadListener listener = this.rowView.getImageLoadListener();
			if (listener != null) listener.imageLoadProgress(msg);
		}

		@Override
		public void imageFetchProgress (final int progress, final int total) {
			final ImageLoadListener listener = this.rowView.getImageLoadListener();
			if (listener != null) listener.imageFetchProgress(progress, total);
		}

		@Override
		public void imagePreShow (final ImageLoadRequest req) {
			final ImageLoadListener listener = this.rowView.getImageLoadListener();
			if (listener != null) listener.imagePreShow(req);
		}

		@Override
		public void imageLoaded (final ImageLoadRequest req) {
			this.rowView.hideText();
			final ImageLoadListener listener = this.rowView.getImageLoadListener();
			if (listener != null) listener.imageLoaded(req);
		}

		@Override
		public void imageLoadFailed (final ImageLoadRequest req, final String errMsg) {
			final Button button = this.rowView.getButton();
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick (final View v) {
					ImageLoadCallbacks.this.imageLoader.loadImage(req.withRetry());
					v.setVisibility(View.GONE);
				}
			});
			button.setVisibility(View.VISIBLE);

			final ImageLoadListener listener = this.rowView.getImageLoadListener();
			if (listener != null) listener.imageLoadFailed(req, errMsg);
		}

	}

}

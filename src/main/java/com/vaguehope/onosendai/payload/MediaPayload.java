package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
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
		return new PayloadRowView((TextView) view.findViewById(R.id.txtMain), (PendingImage) view.findViewById(R.id.imgMain));
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		super.applyTo(rowView, imageLoader, reqWidth, clickListener);
		imageLoader.loadImage(new ImageLoadRequest(this.imgUrl, rowView.getImage(), reqWidth, new CaptionRemover(rowView)));
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

	private static class CaptionRemover implements ImageLoadListener {

		private final PayloadRowView rowView;

		public CaptionRemover (final PayloadRowView rowView) {
			this.rowView = rowView;
		}

		@Override
		public void imageLoadProgress (final String msg) {
			if (this.rowView.getImageLoadListener() != null) this.rowView.getImageLoadListener().imageLoadProgress(msg);
		}

		@Override
		public void imageLoaded (final ImageLoadRequest req) {
			this.rowView.hideText();
			if (this.rowView.getImageLoadListener() != null) this.rowView.getImageLoadListener().imageLoaded(req);
		}

	}

}

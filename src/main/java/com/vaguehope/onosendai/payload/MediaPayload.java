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

	private final String url;

	public MediaPayload (final Tweet ownerTweet, final Meta meta) {
		this(ownerTweet, meta.getData());
	}

	public MediaPayload (final Tweet ownerTweet, final String url) {
		super(ownerTweet, PayloadType.MEDIA);
		this.url = url;
	}

	public String getUrl () {
		return this.url;
	}

	@Override
	public String getTitle () {
		return this.url;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		final Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(this.url));
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
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final PayloadClickListener clickListener) {
		super.applyTo(rowView, imageLoader, clickListener);
		imageLoader.loadImage(new ImageLoadRequest(getUrl(), rowView.getImage(), new CaptionRemover(rowView)));
	}

	@Override
	public int hashCode () {
		return this.url.hashCode();
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof MediaPayload)) return false;
		final MediaPayload that = (MediaPayload) o;
		return EqualHelper.equal(this.url, that.url);
	}

	private static class CaptionRemover implements ImageLoadListener {

		private final PayloadRowView rowView;

		public CaptionRemover (final PayloadRowView rowView) {
			this.rowView = rowView;
		}

		@Override
		public void imageLoaded (final ImageLoadRequest req) {
			this.rowView.hideText();
		}

	}

}

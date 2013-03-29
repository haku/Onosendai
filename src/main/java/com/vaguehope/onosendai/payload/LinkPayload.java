package com.vaguehope.onosendai.payload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.util.EqualHelper;

public class LinkPayload extends Payload {

	private final String url;
	private final String title;

	public LinkPayload (final Tweet ownerTweet, final Meta meta) {
		this(ownerTweet, meta.getData(), meta.getTitle());
	}

	public LinkPayload (final Tweet ownerTweet, final String url) {
		this(ownerTweet, url, null);
	}

	public LinkPayload (final Tweet ownerTweet, final String url, final String title) {
		super(ownerTweet, PayloadType.LINK);
		this.url = url;
		this.title = title != null ? title : url;
	}

	private boolean hasSubtext () {
		return !this.title.equals(this.url);
	}

	@Override
	public String getTitle () {
		return this.title;
	}

	@Override
	public boolean intentable () {
		return true;
	}

	@Override
	public Intent toIntent (final Context context) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(this.url));
		return i;
	}

	@Override
	public PayloadLayout getLayout () {
		if (hasSubtext()) return PayloadLayout.TEXT_SUBTEXT;
		return super.getLayout();
	}

	@Override
	public PayloadRowView makeRowView (final View view) {
		if (hasSubtext()) return new PayloadRowView((TextView) view.findViewById(R.id.txtMain), (TextView) view.findViewById(R.id.txtSubtext));
		return super.makeRowView(view);
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final PayloadClickListener clickListener) {
		super.applyTo(rowView, imageLoader, clickListener);
		if (hasSubtext()) rowView.setSecondaryText(this.url);
	}

	@Override
	public int hashCode () {
		return this.url.hashCode();
	}

	@Override
	public boolean equals (final Object o) {
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof LinkPayload)) return false;
		LinkPayload that = (LinkPayload) o;
		return EqualHelper.equal(this.url, that.url);
	}

}

package com.vaguehope.onosendai.payload;

import java.util.Comparator;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.Tweet;

public abstract class Payload {

	private final Tweet ownerTweet;
	private final Meta meta;
	private final PayloadType type;

	public Payload (final Tweet ownerTweet, final Meta meta, final PayloadType type) {
		this.ownerTweet = ownerTweet;
		this.meta = meta;
		this.type = type;
	}

	public Tweet getOwnerTweet () {
		return this.ownerTweet;
	}

	public Meta getMeta () {
		return this.meta;
	}

	public PayloadType getType () {
		return this.type;
	}

	public abstract CharSequence getTitle ();

	/**
	 * This method may be overridden.
	 */
	public boolean intentable () {
		return false;
	}

	/**
	 * This method may be overridden.
	 * @param context
	 */
	public Intent toIntent (final Context context) {
		throw new UnsupportedOperationException("This payload type '" + this.type + "' can not be expressed as an intent.");
	}

	public boolean longClick (final Context context) {
		return false;
	}

	public PayloadLayout getLayout () {
		return PayloadLayout.TEXT_ONLY;
	}

	public PayloadRowView makeRowView (final View view) {
		return new PayloadRowView((TextView) view.findViewById(R.id.txtMain));
	}

	/**
	 * This method may be overridden.
	 * @param imageLoader
	 * @param clickListener
	 */
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		rowView.setText(getTitle());
	}

	@Override
	public String toString () {
		return new StringBuilder("Payload{")
				.append(getType())
				.append(",").append(getTitle())
				.append("}").toString();
	}

	public static final Comparator<Payload> TYPE_COMP = new Comparator<Payload>() {
		@Override
		public int compare (final Payload lhs, final Payload rhs) {
			final int lo = lhs.getType().ordinal();
			final int ro = rhs.getType().ordinal();
			if (lo != ro) return lo - ro;
			return 0;
		}
	};

}

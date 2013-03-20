package com.vaguehope.onosendai.payload;

import java.util.Comparator;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.payload.PayloadListAdapter.RowView;

public abstract class Payload {

	private final PayloadType type;

	public Payload (final PayloadType type) {
		this.type = type;
	}

	public PayloadType getType () {
		return this.type;
	}

	public abstract String getTitle ();

	public boolean intentable () {
		return false;
	}

	public Intent toIntent () {
		throw new UnsupportedOperationException("This payload type '" + this.type + "' can not be expressed as an intent.");
	}

	public PayloadLayout getLayout () {
		return PayloadLayout.TEXT_ONLY;
	}

	public RowView makeRowView (final View view) {
		return new RowView((TextView) view.findViewById(R.id.txtMain));
	}

	public static final Comparator<Payload> TYPE_TITLE_COMP = new Comparator<Payload>() {
		@Override
		public int compare (final Payload lhs, final Payload rhs) {
			int lo = lhs.getType().ordinal();
			int ro = rhs.getType().ordinal();
			if (lo != ro) return lo - ro;
			return lhs.getTitle().compareTo(rhs.getTitle());
		}
	};

}

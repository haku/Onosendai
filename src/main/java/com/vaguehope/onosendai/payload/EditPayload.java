package com.vaguehope.onosendai.payload;

import java.util.Collections;

import android.view.View;
import android.view.View.OnClickListener;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Meta;
import com.vaguehope.onosendai.model.MetaType;
import com.vaguehope.onosendai.model.Tweet;

public class EditPayload extends Payload {

	public EditPayload (final Tweet ownerTweet, final Meta editSidMeta) {
		super(ownerTweet, PayloadType.EDIT);
		if (editSidMeta.getType() != MetaType.EDIT_SID) throw new IllegalArgumentException();
	}

	@Override
	public String getTitle () {
		return "Share";
	}

	@Override
	public PayloadLayout getLayout () {
		return PayloadLayout.EDIT;
	}

	@Override
	public PayloadRowView makeRowView (final View view) {
		return new PayloadRowView(Collections.singletonMap(0, view.findViewById(R.id.btnDelete)));
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		for (int i = 0; i < rowView.getButtons().size(); i++) {
			rowView.getButtons().get(i).setOnClickListener(new BtnListener(this, clickListener, i));
		}
	}

	private static class BtnListener implements OnClickListener {

		private final Payload payload;
		private final PayloadClickListener clickListener;
		private final int index;

		public BtnListener (final Payload payload, final PayloadClickListener clickListener, final int index) {
			this.payload = payload;
			this.clickListener = clickListener;
			this.index = index;
		}

		@Override
		public void onClick (final View v) {
			this.clickListener.subviewClicked(v, this.payload, this.index);
		}

	}

}

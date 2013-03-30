package com.vaguehope.onosendai.payload;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Tweet;

public class SharePayload extends Payload {

	public SharePayload (final Tweet ownerTweet) {
		super(ownerTweet, PayloadType.SHARE);
	}

	@Override
	public String getTitle () {
		return "Share";
	}

	@Override
	public PayloadLayout getLayout () {
		return PayloadLayout.SHARE;
	}

	@Override
	public PayloadRowView makeRowView (final View view) {
		return new PayloadRowView(new Button[] {
				(Button) view.findViewById(R.id.btnShareRt),
				(Button) view.findViewById(R.id.btnShareQuote)
		});
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final PayloadClickListener clickListener) {
		final Button[] btns = rowView.getButtons();
		for (int i = 0; i < btns.length; i++) {
			btns[i].setOnClickListener(new BtnListener(this, clickListener, i));
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
			this.clickListener.subviewClicked(this.payload, this.index);
		}

	}

}

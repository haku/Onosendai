package com.vaguehope.onosendai.payload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.images.ImageLoader;
import com.vaguehope.onosendai.model.Tweet;
import com.vaguehope.onosendai.provider.NetworkType;

public class SharePayload extends Payload {

	public static final int BTN_SHARE_RT = 100;
	public static final int BTN_SHARE_QUOTE = 101;
	public static final int BTN_SHARE_FAV = 102;
	public static final int BTN_SHARE_INTENT = 103;

	private static final Map<Integer, Integer> BTNS;
	static {
		final Map<Integer, Integer> m = new LinkedHashMap<Integer, Integer>();
		m.put(BTN_SHARE_RT, R.id.btnShareRt);
		m.put(BTN_SHARE_QUOTE, R.id.btnShareQuote);
		m.put(BTN_SHARE_FAV, R.id.btnShareFav);
		m.put(BTN_SHARE_INTENT, R.id.btnShareIntent);
		BTNS = Collections.unmodifiableMap(m);
	}

	private final NetworkType networkType;

	public SharePayload (final Tweet ownerTweet) {
		this(ownerTweet, null);
	}

	public SharePayload (final Tweet ownerTweet, final NetworkType networkType) {
		super(ownerTweet, null, PayloadType.SHARE);
		this.networkType = networkType;
	}

	public NetworkType getNetworkType () {
		return this.networkType;
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
		final Map<Integer, View> btns = new LinkedHashMap<Integer, View>();
		for (final Entry<Integer, Integer> btn : BTNS.entrySet()) {
			btns.put(btn.getKey(), view.findViewById(btn.getValue()));
		}
		return new PayloadRowView(btns);
	}

	@Override
	public void applyTo (final PayloadRowView rowView, final ImageLoader imageLoader, final int reqWidth, final PayloadClickListener clickListener) {
		final Map<Integer, View> btns = rowView.getButtons();

		final TextView btnRt = (TextView) btns.get(BTN_SHARE_RT);
		final View btnFav = btns.get(BTN_SHARE_FAV);
		if (this.networkType != null) {
			switch (this.networkType) {
				case TWITTER:
					btnRt.setText(R.string.tweetlist_details_rt);
					btnRt.setVisibility(View.VISIBLE);
					break;
				case MASTODON:
					btnRt.setText(R.string.tweetlist_details_boost);
					btnRt.setVisibility(View.VISIBLE);
					break;
				case FACEBOOK:
					btnRt.setText(R.string.tweetlist_details_like);
					btnRt.setVisibility(View.VISIBLE);
					btnFav.setVisibility(View.GONE);
					break;
				default:
					btnRt.setVisibility(View.GONE);
			}
		}
		else {
			btnRt.setVisibility(View.GONE);
		}

		for (final Entry<Integer, View> btn : btns.entrySet()) {
			btn.getValue().setOnClickListener(new BtnListener(this, clickListener, btn.getKey()));
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

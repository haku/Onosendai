package com.vaguehope.onosendai.payload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.onosendai.images.ImageLoadRequest.ImageLoadListener;
import com.vaguehope.onosendai.widget.PendingImage;

class PayloadRowView {

	private final TextView main;
	private final TextView secondary;
	private final TextView tertiary;
	private final ImageView image;
	private final PendingImage pendingImage;
	private final List<View> buttons;

	public PayloadRowView (final TextView main) {
		this(main, null, null, null);
	}

	public PayloadRowView (final TextView main, final ImageView image) {
		this(main, image, null);
	}

	public PayloadRowView (final TextView main, final TextView secondary) {
		this(main, null, secondary);
	}

	public PayloadRowView (final TextView main, final ImageView image, final TextView secondary) {
		this(main, image, secondary, null);
	}

	public PayloadRowView (final TextView main, final ImageView image, final TextView secondary, final TextView tertiary) {
		this.main = main;
		this.secondary = secondary;
		this.tertiary = tertiary;
		this.image = image;
		this.pendingImage = null;
		this.buttons = null;
	}

	public PayloadRowView (final TextView main, final PendingImage pendingImage, final Button button) {
		this.main = main;
		this.secondary = null;
		this.tertiary = null;
		this.image = null;
		this.pendingImage = pendingImage;
		this.buttons = button != null ? Collections.<View>singletonList(button) : null;
	}

	public PayloadRowView (final List<View> buttons) {
		this.main = null;
		this.secondary = null;
		this.tertiary = null;
		this.image = null;
		this.pendingImage = null;
		this.buttons = Collections.unmodifiableList(new ArrayList<View>(buttons));
	}

	public void setText (final String text) {
		if (this.main == null) return;
		this.main.setText(text);
		this.main.setVisibility(View.VISIBLE);
	}

	public void hideText () {
		this.main.setVisibility(View.GONE);
	}

	public void hideButtons () {
		if (this.buttons == null) return;
		for (final View btn : this.buttons) {
			btn.setVisibility(View.GONE);
		}
	}

	public void setSecondaryText (final String text) {
		if (this.secondary == null) return;
		this.secondary.setText(text);
		this.secondary.setVisibility(View.VISIBLE);
	}

	public void setTertiaryText (final String text) {
		if (this.tertiary == null) return;
		this.tertiary.setText(text);
		this.tertiary.setVisibility(View.VISIBLE);
	}

	public ImageView getImage () {
		if (this.pendingImage != null) return this.pendingImage.getImage();
		return this.image;
	}

	public ImageLoadListener getImageLoadListener () {
		if (this.pendingImage != null) return this.pendingImage.getImageLoadListener();
		return null;
	}

	public Button getButton () {
		return (Button) (this.buttons != null && this.buttons.size() > 0 ? this.buttons.get(0) : null);
	}

	public List<View> getButtons () {
		return this.buttons;
	}

}

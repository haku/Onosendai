package com.vaguehope.onosendai.widget.adaptor;

import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.widget.MultiAutoCompleteTextView;

/**
 * https://stackoverflow.com/questions/12691679
 */
public class PopupPositioniner implements TextWatcher {

	private final MultiAutoCompleteTextView tv;

	public PopupPositioniner (final MultiAutoCompleteTextView tv) {
		this.tv = tv;
	}

	@Override
	public void onTextChanged (final CharSequence s, final int start, final int before, final int count) {
		final Layout layout = this.tv.getLayout();
		this.tv.setDropDownVerticalOffset(layout.getLineBottom(layout.getLineForOffset(this.tv.getSelectionStart())) - this.tv.getHeight());
	}

	@Override
	public void beforeTextChanged (final CharSequence s, final int start, final int count, final int after) {/**/}

	@Override
	public void afterTextChanged (final Editable s) {/**/}

}
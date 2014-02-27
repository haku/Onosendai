package com.vaguehope.onosendai.widget.adaptor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

public class TextCounterWatcher implements TextWatcher {

	private final TextView txtView;
	private final EditText editText;

	public TextCounterWatcher (final TextView txtView, final EditText editText) {
		this.txtView = txtView;
		this.editText = editText;
	}

	@Override
	public void afterTextChanged (final Editable s) {
		this.txtView.setText(String.valueOf(this.editText.getText().length()));
	}

	@Override
	public void onTextChanged (final CharSequence s, final int start, final int before, final int count) {/**/}

	@Override
	public void beforeTextChanged (final CharSequence s, final int start, final int count, final int after) {/**/}
}
package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.EditText;

/*
 * https://developer.android.com/intl/fr/guide/topics/ui/settings.html
 * Useful: http://d.hatena.ne.jp/hidecheck/20100905/1283706015
 */
public class AccountDialogPreference extends DialogPreference {

	private static final String DEFAULT_VALUE = "Default Value desu~";

	private EditText editText;

	public AccountDialogPreference (final Context context) {
		super(context, null);

		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	protected View onCreateDialogView () {
		this.editText = new EditText(getContext());
		this.editText.setSelectAllOnFocus(true);
		this.editText.setHint("username");
		this.editText.setText(getPersistedString(DEFAULT_VALUE));
		return this.editText;
	}

	@Override
	protected void onDialogClosed (final boolean positiveResult) {
		if (positiveResult) persistString(this.editText.getText().toString());
		super.onDialogClosed(positiveResult);
	}

}

package com.vaguehope.onosendai.widget;

import android.content.Context;
import android.preference.ListPreference;

public class ComboPreference extends ListPreference {

	public ComboPreference (final Context context) {
		super(context);
	}

	@Override
	protected void onSetInitialValue (final boolean restoreValue, final Object defaultValue) {
		super.onSetInitialValue(restoreValue, defaultValue);
		refreshSummary();
	}

	@Override
	protected void onDialogClosed (final boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) refreshSummary();
	}

	private void refreshSummary () {
		setSummary(getEntries()[findIndexOfValue(getValue())]);
	}

}

package com.vaguehope.onosendai.ui.pref;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.view.View;

import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

public class FilterDialogPref extends DialogPreference {

	private final FiltersPrefFragment filtersPrefFragment;
	private final String filterId;
	private final String filter;
	private FilterDialog dialog;

	public FilterDialogPref (final Context context, final String filterId, final String filter, final FiltersPrefFragment filtersPrefFragment) {
		super(context, null);
		this.filterId = filterId;
		this.filter = filter;
		this.filtersPrefFragment = filtersPrefFragment;

		setKey(filterId);
		setTitle(filter);

		setDialogTitle("Edit Filter (" + getKey() + ")");
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	protected View onCreateDialogView () {
		this.dialog = new FilterDialog(getContext(), this.filter);
		this.dialog.setOnValidateListener(new Listener<Boolean>() {
			@Override
			public void onAnswer (final Boolean valid) {
				final Dialog d = getDialog();
				if (d != null && d instanceof AlertDialog) ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(valid);
			}
		});
		return this.dialog.getRootView();
	}

	@Override
	protected void onDialogClosed (final boolean positiveResult) {
		if (positiveResult) {
			try {
				if (this.dialog.isDeleteSelected()) {
					this.filtersPrefFragment.askDeleteFilter(this.filterId, this.filter);
				}
				else {
					persistString(this.dialog.getValue());
					this.filtersPrefFragment.refreshFiltersList();
				}
			}
			catch (final Exception e) {
				DialogHelper.alert(getContext(), e);
			}
		}
		super.onDialogClosed(positiveResult);
	}

}

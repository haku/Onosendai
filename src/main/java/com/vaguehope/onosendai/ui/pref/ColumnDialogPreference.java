package com.vaguehope.onosendai.ui.pref;

import org.json.JSONException;

import android.content.Context;
import android.preference.DialogPreference;
import android.view.View;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;

public class ColumnDialogPreference extends DialogPreference {

	private final Column column;
	private final ColumnsPrefFragment columnsPrefFragment;
	private ColumnDialog dialog;

	public ColumnDialogPreference (final Context context, final Column column, final ColumnsPrefFragment columnsPrefFragment) {
		super(context, null);
		this.column = column;
		this.columnsPrefFragment = columnsPrefFragment;

		setKey(Prefs.makeColumnId(column.getId()));
		setTitle(column.getUiTitle());
		setSummary(column.getUiDescription());

		setDialogTitle("Edit Column (" + getKey() + ")");
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	protected View onCreateDialogView () {
		this.dialog = new ColumnDialog(getContext(), this.column);
		return this.dialog.getRootView();
	}

	@Override
	protected void onDialogClosed (final boolean positiveResult) {
		if (positiveResult) {
			try {
				if (this.dialog.isDeleteSelected()) {
					this.columnsPrefFragment.askDeleteColumn(this.dialog.getInitialValue());
				}
				else {
					persistString(this.dialog.getValue().toJson().toString());
					this.columnsPrefFragment.refreshColumnsList();
				}
			}
			catch (JSONException e) {
				DialogHelper.alert(getContext(), e);
			}
		}
		super.onDialogClosed(positiveResult);
	}

}

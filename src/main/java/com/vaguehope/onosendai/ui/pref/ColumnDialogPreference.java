package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;

import android.content.Context;
import android.preference.DialogPreference;
import android.view.View;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.Functions;

public class ColumnDialogPreference extends DialogPreference {

	private final Column column;
	private final ColumnsPrefFragment columnsPrefFragment;
	private ColumnDialog dialog;

	public ColumnDialogPreference (final Context context, final Column column, final Collection<Account> accounts, final ColumnsPrefFragment columnsPrefFragment) {
		super(context, null);

		this.column = column;
		this.columnsPrefFragment = columnsPrefFragment;

		setKey(Prefs.makeColumnId(column.getId()));
		setTitle(column.getUiTitle());
		setSummary(accounts != null ? ArrayHelper.join(CollectionHelper.map(accounts, Functions.TITLE, new ArrayList<String>()), ", ") : null);

		setDialogTitle("Edit Column (" + getKey() + ")"); //ES
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	@Override
	protected View onCreateDialogView () {
		this.dialog = new ColumnDialog(getContext(), this.columnsPrefFragment.getPrefs(), this.column);
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
					final Column newColumn = this.dialog.getValue();
					persistString(newColumn.toJson().toString());
					this.columnsPrefFragment.moveColumnToPosition(newColumn, this.dialog.getPosition());
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

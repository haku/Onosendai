package com.vaguehope.onosendai.ui.pref;

import org.json.JSONException;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.config.TimeParser;
import com.vaguehope.onosendai.util.CollectionHelper;

class ColumnDialog {

	private final int id;
	private final String accountId;
	private final Column initialValue;

	private final View llParent;
	private final EditText txtTitle;
	private final Spinner spnPosition;
	private final TextView lblAccount;
	private final EditText txtResource;
	private final EditText txtRefresh;
	private final CheckBox chkNotify;
	private final CheckBox chkDelete;

	public ColumnDialog (final Context context, final Prefs prefs, final int id, final String accountId) {
		this(context, prefs, id, accountId, null);
	}

	public ColumnDialog (final Context context, final Prefs prefs, final Column initialValue) {
		this(context, prefs,
				initialValue != null ? initialValue.getId() : null,
				initialValue != null ? initialValue.getAccountId() : null,
				initialValue);
	}

	private ColumnDialog (final Context context, final Prefs prefs, final int id, final String accountId, final Column initialValue) {
		if (prefs == null) throw new IllegalArgumentException("Prefs can not be null.");
		if (initialValue != null && initialValue.getId() != id) throw new IllegalStateException("ID and initialValue ID do not match.");
		if (initialValue != null && initialValue.getAccountId() != accountId) throw new IllegalStateException("Account ID and initialValue account ID do not match.");

		this.id = id;
		this.accountId = accountId;
		this.initialValue = initialValue;

		LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.columndialog, null);

		this.txtTitle = (EditText) this.llParent.findViewById(R.id.txtTitle);
		this.spnPosition = (Spinner) this.llParent.findViewById(R.id.spnPosition);
		this.lblAccount = (TextView) this.llParent.findViewById(R.id.lblAccount);
		this.txtResource = (EditText) this.llParent.findViewById(R.id.txtResource);
		this.txtRefresh = (EditText) this.llParent.findViewById(R.id.txtRefresh);
		this.chkNotify = (CheckBox) this.llParent.findViewById(R.id.chkNotify);
		this.chkDelete = (CheckBox) this.llParent.findViewById(R.id.chkDelete);

		final ArrayAdapter<Integer> posAdapter = new ArrayAdapter<Integer>(context, R.layout.numberspinneritem);
		posAdapter.addAll(CollectionHelper.sequence(1, prefs.readColumnIds().size() + (initialValue == null ? 1 : 0)));
		this.spnPosition.setAdapter(posAdapter);

		if (accountId == null || accountId.isEmpty()) {
			this.lblAccount.setText("-"); // System account.
		}
		else {
			try {
				final Account account = prefs.readAccount(accountId);
				this.lblAccount.setText(account.getUiTitle());
			}
			catch (JSONException e) {
				throw new IllegalStateException(e); // TODO this seems like wall-paper.
			}
		}

		if (initialValue != null) {
			this.txtTitle.setText(initialValue.getTitle());
			this.spnPosition.setSelection(posAdapter.getPosition(Integer.valueOf(prefs.readColumnPosition(initialValue.getId()) + 1)));
			this.txtResource.setText(initialValue.getResource());
			this.txtRefresh.setText(initialValue.getRefreshIntervalMins() > 0
					? initialValue.getRefreshIntervalMins() + "mins"
					: ""); // TODO make this a number chooser.
			this.chkNotify.setChecked(initialValue.isNotify());
			this.chkDelete.setVisibility(View.VISIBLE);
		}
		else {
			this.spnPosition.setSelection(posAdapter.getCount() - 1); // Last item.
		}
	}

	public Column getInitialValue () {
		return this.initialValue;
	}

	public View getRootView () {
		return this.llParent;
	}

	public void setResource (final String resource) {
		this.txtResource.setText(resource);
	}

	public void setTitle (final String title) {
		this.txtTitle.setText(title);
	}

	/**
	 * 0 based.
	 */
	public int getPosition () {
		return ((Integer) this.spnPosition.getSelectedItem()) - 1;
	}

	public boolean isDeleteSelected () {
		return this.chkDelete.isChecked();
	}

	public Column getValue () {
		final int mins = TimeParser.parseDuration(this.txtRefresh.getText().toString());
		return new Column(this.id,
				this.txtTitle.getText().toString(),
				this.accountId,
				this.txtResource.getText().toString(),
				mins,
				this.initialValue != null ? this.initialValue.getExcludeColumnIds() : null, // TODO GUI for excludes.
				this.chkNotify.isChecked());
	}

}

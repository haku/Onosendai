package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.TimeParser;

class ColumnDialog {

	private final int id;
	private final Column initialValue;

	private final View llParent;
	private final EditText txtTitle;
	private final EditText txtAccountId;
	private final EditText txtResource;
	private final EditText txtRefresh;
	private final CheckBox chkNotify;
	private final CheckBox chkDelete;

	public ColumnDialog (final Context context, final int id) {
		this(context, id, null);
	}

	public ColumnDialog (final Context context, final Column initialValue) {
		this(context, initialValue != null ? initialValue.getId() : null, initialValue);
	}

	private ColumnDialog (final Context context, final int id, final Column initialValue) {
		if (initialValue != null && initialValue.getId() != id) throw new IllegalStateException("ID and initialValue ID do not match.");

		this.id = id;
		this.initialValue = initialValue;

		LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.columndialog, null);

		this.txtTitle = (EditText) this.llParent.findViewById(R.id.txtTitle);
		this.txtAccountId = (EditText) this.llParent.findViewById(R.id.txtAccountId);
		this.txtResource = (EditText) this.llParent.findViewById(R.id.txtResource);
		this.txtRefresh = (EditText) this.llParent.findViewById(R.id.txtRefresh);
		this.chkNotify = (CheckBox) this.llParent.findViewById(R.id.chkNotify);
		this.chkDelete = (CheckBox) this.llParent.findViewById(R.id.chkDelete);

		if (initialValue != null) {
			this.txtTitle.setText(initialValue.getTitle());
			this.txtAccountId.setText(initialValue.getAccountId());
			this.txtResource.setText(initialValue.getResource());
			this.txtRefresh.setText(initialValue.getRefreshIntervalMins() > 0
					? initialValue.getRefreshIntervalMins() + "mins"
					: ""); // TODO make this a number chooser.
			this.chkNotify.setChecked(initialValue.isNotify());
			this.chkDelete.setVisibility(View.VISIBLE);
		}
	}

	public Column getInitialValue () {
		return this.initialValue;
	}

	public View getRootView () {
		return this.llParent;
	}

	public void setAccount (final Account account) {
		this.txtAccountId.setText(account.getId());
	}

	public void setResource (final String resource) {
		this.txtResource.setText(resource);
	}

	public void setTitle (final String title) {
		this.txtTitle.setText(title);
	}

	public boolean isDeleteSelected () {
		return this.chkDelete.isChecked();
	}

	public Column getValue () {
		final int mins = TimeParser.parseDuration(this.txtRefresh.getText().toString());
		return new Column(this.id,
				this.txtTitle.getText().toString(),
				this.txtAccountId.getText().toString(),
				this.txtResource.getText().toString(),
				mins,
				this.initialValue != null ? this.initialValue.getExcludeColumnIds() : null, // TODO GUI for excludes.
				this.chkNotify.isChecked());
	}

}

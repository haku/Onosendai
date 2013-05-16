package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.TimeParser;

class ColumnDialog {

	private final int id;
	private final Column initialValue;

	private final LinearLayout llParent;
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

		this.llParent = new LinearLayout(context);
		this.llParent.setOrientation(LinearLayout.VERTICAL);

		this.txtTitle = new EditText(context);
		this.txtTitle.setSelectAllOnFocus(true);
		this.txtTitle.setHint("title");
		this.llParent.addView(this.txtTitle);

		this.txtAccountId = new EditText(context);
		this.txtAccountId.setSelectAllOnFocus(true);
		this.txtAccountId.setHint("accountId");
		this.llParent.addView(this.txtAccountId);

		this.txtResource = new EditText(context);
		this.txtResource.setSelectAllOnFocus(true);
		this.txtResource.setHint("resource");
		this.llParent.addView(this.txtResource);

		this.txtRefresh = new EditText(context);
		this.txtRefresh.setSelectAllOnFocus(true);
		this.txtRefresh.setHint("refresh");
		this.llParent.addView(this.txtRefresh);

		this.chkNotify = new CheckBox(context);
		this.chkNotify.setText("notify");
		this.llParent.addView(this.chkNotify);

		this.chkDelete = new CheckBox(context);
		this.chkDelete.setText("delete");
		this.chkDelete.setChecked(false);

		if (initialValue != null) {
			this.txtTitle.setText(initialValue.getTitle());
			this.txtAccountId.setText(initialValue.getAccountId());
			this.txtResource.setText(initialValue.getResource());
			this.txtRefresh.setText(initialValue.getRefreshIntervalMins() > 0
					? initialValue.getRefreshIntervalMins() + "mins"
					: ""); // TODO make this a number chooser.
			this.chkNotify.setChecked(initialValue.isNotify());
			this.llParent.addView(this.chkDelete);
		}
	}

	public Column getInitialValue () {
		return this.initialValue;
	}

	public View getRootView () {
		return this.llParent;
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
				this.initialValue.getExcludeColumnIds(), // TODO GUI for excludes.
				this.chkNotify.isChecked());
	}

}

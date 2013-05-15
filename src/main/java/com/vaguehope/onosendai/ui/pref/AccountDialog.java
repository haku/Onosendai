package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;

class AccountDialog {

	private final String id;
	private final Account initialValue;

	private final ArrayAdapter<AccountProvider> actAccountTypeAdaptor;
	private final LinearLayout llParent;
	private final Spinner spnAccountType;
	private final EditText txtUsername;
	private final EditText txtPassword;
	private final CheckBox chkDelete;

	public AccountDialog (final Context context, final String id) {
		this(context, id, null);
	}

	public AccountDialog (final Context context, final Account initialValue) {
		this(context, initialValue != null ? initialValue.getId() : null, initialValue);
	}

	private AccountDialog (final Context context, final String id, final Account initialValue) {
		if (id == null) throw new IllegalArgumentException("ID may not be null.");
		if (initialValue != null && initialValue.getId() != id) throw new IllegalStateException("ID and initialValue ID do not match.");

		this.id = id;
		this.initialValue = initialValue;

		this.actAccountTypeAdaptor = new ArrayAdapter<AccountProvider>(context, R.layout.accounttypelistrow);
		this.actAccountTypeAdaptor.add(AccountProvider.SUCCESSWHALE);
		// TODO include other account types.

		this.llParent = new LinearLayout(context);
		this.llParent.setOrientation(LinearLayout.VERTICAL);

		this.spnAccountType = new Spinner(context);
		this.spnAccountType.setAdapter(this.actAccountTypeAdaptor);
		//this.spnAccountType.setOnItemSelectedListener(this...);
		this.llParent.addView(this.spnAccountType);

		this.txtUsername = new EditText(context);
		this.txtUsername.setSelectAllOnFocus(true);
		this.txtUsername.setHint("username");
		this.llParent.addView(this.txtUsername);

		this.txtPassword = new EditText(context);
		this.txtPassword.setSelectAllOnFocus(true);
		this.txtPassword.setHint("password");
		this.llParent.addView(this.txtPassword);

		this.chkDelete = new CheckBox(context);
		this.chkDelete.setText("delete");
		this.chkDelete.setChecked(false);

		// TODO include other account types.
		if (initialValue != null) {
			this.txtUsername.setText(initialValue.getAccessToken());
			this.txtPassword.setText(initialValue.getAccessSecret());
			this.llParent.addView(this.chkDelete);
		}
	}

	public Account getInitialValue () {
		return this.initialValue;
	}

	public View getRootView () {
		return this.llParent;
	}

	public boolean isDeleteSelected() {
		return this.chkDelete.isChecked();
	}

	public Account getValue () {
		AccountProvider ap = (AccountProvider) this.spnAccountType.getSelectedItem();
		switch (ap) {
			case SUCCESSWHALE:
				return new Account(this.id,
						AccountProvider.SUCCESSWHALE,
						null, null, this.txtUsername.getText().toString(),
						this.txtPassword.getText().toString());
			default:
				return null;
		}
	}

}

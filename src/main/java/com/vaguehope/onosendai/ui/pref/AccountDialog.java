package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;

class AccountDialog {

	private final String id;
	private final AccountProvider accountProvider;
	private final Account initialValue;

	private final LinearLayout llParent;
	private final EditText txtUsername;
	private final EditText txtPassword;
	private final CheckBox chkDelete;

	public AccountDialog (final Context context, final String id, final AccountProvider accountProvider) {
		this(context, id, accountProvider, null);
	}

	public AccountDialog (final Context context, final Account initialValue) {
		this(context,
				initialValue != null ? initialValue.getId() : null,
				initialValue != null ? initialValue.getProvider() : null,
				initialValue);
	}

	private AccountDialog (final Context context, final String id, final AccountProvider accountProvider, final Account initialValue) {
		if (id == null) throw new IllegalArgumentException("ID may not be null.");
		if (accountProvider == null) throw new IllegalArgumentException("Account provider may not be null.");
		if (initialValue != null && !id.equals(initialValue.getId())) throw new IllegalStateException("ID and initialValue ID do not match.");

		this.id = id;
		this.accountProvider = accountProvider;
		this.initialValue = initialValue;

		this.llParent = new LinearLayout(context);
		this.txtUsername = new EditText(context);
		this.txtPassword = new EditText(context);
		this.chkDelete = new CheckBox(context);

		this.llParent.setOrientation(LinearLayout.VERTICAL);

		switch (this.accountProvider) {
			case SUCCESSWHALE:
			case INSTAPAPER:
				this.txtUsername.setSelectAllOnFocus(true);
				this.txtUsername.setHint("username");
				this.llParent.addView(this.txtUsername);

				this.txtPassword.setSelectAllOnFocus(true);
				this.txtPassword.setHint("password");
				this.llParent.addView(this.txtPassword);

				break;
			default:
		}

		this.chkDelete.setText("delete");
		this.chkDelete.setChecked(false);

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

	public boolean isDeleteSelected () {
		return this.chkDelete.isChecked();
	}

	/**
	 * Will getValue() return something?
	 */
	public boolean isSaveable () {
		switch (this.accountProvider) {
			case SUCCESSWHALE:
			case INSTAPAPER:
				return true;
			default:
		}
		return false;
	}

	public Account getValue () {
		switch (this.accountProvider) {
			case SUCCESSWHALE:
			case INSTAPAPER:
				final String username = this.txtUsername.getText().toString();
				final String password = this.txtPassword.getText().toString();
				return new Account(this.id, username,
						this.accountProvider,
						null, null, username, password);
			default:
				return null;
		}
	}

}

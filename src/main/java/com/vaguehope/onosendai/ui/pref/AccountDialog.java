package com.vaguehope.onosendai.ui.pref;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.provider.ProviderMgr;
import com.vaguehope.onosendai.storage.VolatileKvStore;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.ExcpetionHelper;

class AccountDialog {

	private final Context context;
	private final String id;
	private final AccountProvider accountProvider;
	private final Account initialValue;

	private final LinearLayout llParent;
	private final EditText txtUsername;
	private final EditText txtPassword;
	private final Button btnTest;
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

		this.context = context;
		this.id = id;
		this.accountProvider = accountProvider;
		this.initialValue = initialValue;

		this.llParent = new LinearLayout(context);
		this.txtUsername = new EditText(context);
		this.txtPassword = new EditText(context);
		this.btnTest = new Button(context);
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

				addTestLoginBtn();

				break;
			case BUFFER:
				this.txtUsername.setSelectAllOnFocus(true);
				this.txtUsername.setHint("accessToken");
				this.llParent.addView(this.txtUsername);

				addTestLoginBtn();

				break;
			default:
		}

		this.chkDelete.setText("delete");
		this.chkDelete.setChecked(false);

		if (initialValue != null) {
			if (this.txtUsername != null) this.txtUsername.setText(initialValue.getAccessToken());
			if (this.txtPassword != null) this.txtPassword.setText(initialValue.getAccessSecret());
			this.llParent.addView(this.chkDelete);
		}
	}

	private void addTestLoginBtn () {
		this.btnTest.setText("test login");
		this.btnTest.setOnClickListener(this.btnTestClickListener);
		this.llParent.addView(this.btnTest);
	}

	private final OnClickListener btnTestClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			testLogin();
		}
	};

	protected void testLogin () {
		new TestLoginTask(this.context, getValue()).execute(); // TODO Get an executor.
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
			case BUFFER:
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
			case BUFFER:
				final String title = this.initialValue != null ? this.initialValue.getTitle() : this.id;
				final String accessToken = this.txtUsername.getText().toString();
				return new Account(this.id, title,
						this.accountProvider,
						null, null, accessToken, null);
			default:
				return null;
		}
	}

	private static class TestLoginTask extends AsyncTask<Void, Void, String> {

		private final Context context;
		private final Account account;

		private ProgressDialog dialog;

		public TestLoginTask (final Context context, final Account account) {
			this.context = context;
			this.account = account;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.context, this.account.getProvider().getUiTitle(), "Testing login...", true);
		}

		@Override
		protected String doInBackground (final Void... params) {
			final ProviderMgr provMgr = new ProviderMgr(new VolatileKvStore());
			try {
				switch (this.account.getProvider()) {
					case SUCCESSWHALE:
						provMgr.getSuccessWhaleProvider().testAccountLogin(this.account);
						return "Success.";
					case INSTAPAPER:
						provMgr.getInstapaperProvider().testAccountLogin(this.account);
						return "Success.";
					case BUFFER:
						provMgr.getBufferAppProvider().testAccountLogin(this.account);
						return "Success.";
					default:
						return "Do not know how to test account type: " + this.account.getProvider().getUiTitle();
				}
			}
			catch (final Exception e) { // NOSONAR want to report all errors.
				return ExcpetionHelper.causeTrace(e);
			}
			finally {
				provMgr.shutdown();
			}
		}

		@Override
		protected void onPostExecute (final String result) {
			this.dialog.dismiss();
			DialogHelper.alert(this.context, result);
		}

	}

}

package com.vaguehope.onosendai.ui.pref;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.vaguehope.onosendai.R;
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

	private final View llParent;
	private final EditText txtTitle;
	private final View rowUsername;
	private final TextView txtUsernameLabel;
	private final EditText txtUsername;
	private final View rowPassword;
	private final EditText txtPassword;
	private final Button btnTest;
	private final CheckBox chkReauthenticate;
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

		final LayoutInflater inflater = LayoutInflater.from(context);
		this.llParent = inflater.inflate(R.layout.accountdialog, null);

		this.txtTitle = (EditText) this.llParent.findViewById(R.id.txtTitle);
		this.rowUsername = this.llParent.findViewById(R.id.rowUsername);
		this.txtUsername = (EditText) this.llParent.findViewById(R.id.txtUsername);
		this.txtUsernameLabel = (TextView) this.llParent.findViewById(R.id.txtUsernameLabel);
		this.rowPassword = this.llParent.findViewById(R.id.rowPassword);
		this.txtPassword = (EditText) this.llParent.findViewById(R.id.txtPassword);
		this.btnTest = (Button) this.llParent.findViewById(R.id.btnTestLogin);
		this.chkReauthenticate = (CheckBox) this.llParent.findViewById(R.id.chkReauthenticate);
		this.chkDelete = (CheckBox) this.llParent.findViewById(R.id.chkDelete);

		if (this.initialValue != null) {
			this.txtTitle.setText(this.initialValue.getTitle());
		}

		switch (this.accountProvider) {
			case TWITTER:
				this.rowUsername.setVisibility(View.GONE);
				this.rowPassword.setVisibility(View.GONE);
				this.btnTest.setVisibility(View.GONE);
				break;
			case SUCCESSWHALE:
			case INSTAPAPER:
				this.txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
				if (initialValue != null) {
					this.txtUsername.setText(initialValue.getAccessToken());
					this.txtPassword.setText(initialValue.getAccessSecret());
				}
				this.btnTest.setOnClickListener(this.btnTestClickListener);
				break;
			case BUFFER:
				this.txtUsernameLabel.setText("accessToken");
				this.rowPassword.setVisibility(View.GONE);
				if (initialValue != null) {
					this.txtUsername.setText(initialValue.getAccessToken());
				}
				this.btnTest.setOnClickListener(this.btnTestClickListener);
				break;
			default:
		}

		this.chkReauthenticate.setChecked(false);
		this.chkReauthenticate.setVisibility(initialValue != null && accountProvider == AccountProvider.TWITTER ? View.VISIBLE : View.GONE);

		this.chkDelete.setChecked(false);
		this.chkDelete.setVisibility(initialValue != null ? View.VISIBLE : View.GONE);
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

	public boolean isReauthenticateSelected () {
		return this.chkReauthenticate.isChecked();
	}

	public boolean isDeleteSelected () {
		return this.chkDelete.isChecked();
	}

	/**
	 * Will getValue() return something?
	 */
	public boolean isSaveable () {
		switch (this.accountProvider) {
			case TWITTER:
			case SUCCESSWHALE:
			case INSTAPAPER:
			case BUFFER:
				return true;
			default:
		}
		return false;
	}

	public Account getValue () {
		final String title = this.txtTitle.getText().toString();
		switch (this.accountProvider) {
			case TWITTER:
				if (this.initialValue == null) throw new IllegalStateException("Can not use account dialog to create a Twitter account.");
				return new Account(this.id, title,
						this.initialValue.getProvider(),
						this.initialValue.getConsumerKey(), this.initialValue.getConsumerSecret(),
						this.initialValue.getAccessToken(), this.initialValue.getAccessSecret());
			case SUCCESSWHALE:
			case INSTAPAPER:
				final String username = this.txtUsername.getText().toString();
				final String password = this.txtPassword.getText().toString();
				return new Account(this.id, title,
						this.accountProvider,
						null, null, username, password);
			case BUFFER:
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

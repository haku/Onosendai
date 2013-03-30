package com.vaguehope.onosendai.ui;

import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.vaguehope.onosendai.R;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.provider.successwhale.PostToAccount;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleException;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleProvider;
import com.vaguehope.onosendai.ui.SwPostToAccountLoaderTask.AccountLoaderResult;

class SwPostToAccountLoaderTask extends AsyncTask<Account, Void, AccountLoaderResult> {

	private final ViewGroup llSubAccounts;
	private final Map<PostToAccount, Boolean> enabledSubAccounts;

	public SwPostToAccountLoaderTask (final ViewGroup llSubAccounts, final Map<PostToAccount, Boolean> enabledSubAccounts) {
		this.llSubAccounts = llSubAccounts;
		this.enabledSubAccounts = enabledSubAccounts;
	}

	@Override
	protected void onPreExecute () {
		this.enabledSubAccounts.clear();
		this.llSubAccounts.removeAllViews();
		final ProgressBar progressBar = new ProgressBar(this.llSubAccounts.getContext());
		progressBar.setIndeterminate(true);
		this.llSubAccounts.addView(progressBar);
		this.llSubAccounts.setVisibility(View.VISIBLE);
	}

	@Override
	protected AccountLoaderResult doInBackground (final Account... params) {
		if (params.length != 1) throw new IllegalArgumentException("Only one account per task.");
		final Account account = params[0];

		SuccessWhaleProvider swProv = new SuccessWhaleProvider();
		try {
			return new AccountLoaderResult(swProv.getPostToAccounts(account));
		}
		catch (SuccessWhaleException e) {
			return new AccountLoaderResult(e);
		}
		finally {
			swProv.shutdown();
		}
	}

	@Override
	protected void onPostExecute (final AccountLoaderResult result) {
		this.llSubAccounts.removeAllViews();
		if (result.isSuccess()) {
			for (final PostToAccount pta : result.getAccounts()) {
				final View view = View.inflate(this.llSubAccounts.getContext(), R.layout.subaccountitem, null);
				final ToggleButton btnEnableAccount = (ToggleButton) view.findViewById(R.id.btnEnableAccount);
				final String displayName = pta.getDisplayName();
				btnEnableAccount.setTextOn(displayName);
				btnEnableAccount.setTextOff(displayName);
				btnEnableAccount.setChecked(pta.isEnabled());
				this.enabledSubAccounts.put(pta, pta.isEnabled());
				btnEnableAccount.setOnClickListener(new SubAccountToggleListener(pta, this.enabledSubAccounts));
				this.llSubAccounts.addView(view);
			}
		}
		else {
			Toast.makeText(this.llSubAccounts.getContext(),
					"Failed to fetch sub accounts: " + result.getE().toString(),
					Toast.LENGTH_LONG).show();
		}
	}

	protected static class AccountLoaderResult {

		private final boolean success;
		private final List<PostToAccount> accounts;
		private final Exception e;

		public AccountLoaderResult (final List<PostToAccount> accounts) {
			if (accounts == null) throw new IllegalArgumentException("Missing arg: accounts.");
			this.success = true;
			this.accounts = accounts;
			this.e = null;
		}

		public AccountLoaderResult (final Exception e) {
			if (e == null) throw new IllegalArgumentException("Missing arg: e.");
			this.success = false;
			this.accounts = null;
			this.e = e;
		}

		public boolean isSuccess () {
			return this.success;
		}

		public List<PostToAccount> getAccounts () {
			return this.accounts;
		}

		public Exception getE () {
			return this.e;
		}

	}

	private static class SubAccountToggleListener implements OnClickListener {

		private final PostToAccount pta;
		private final Map<PostToAccount, Boolean> enabledSubAccounts;

		public SubAccountToggleListener (final PostToAccount pta, final Map<PostToAccount, Boolean> enabledSubAccounts) {
			this.pta = pta;
			this.enabledSubAccounts = enabledSubAccounts;
		}

		@Override
		public void onClick (final View v) {
			ToggleButton btn = (ToggleButton) v;
			this.enabledSubAccounts.put(this.pta, btn.isChecked());
		}
	}

}
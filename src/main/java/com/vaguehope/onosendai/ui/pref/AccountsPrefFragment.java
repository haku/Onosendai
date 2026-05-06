package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import org.json.JSONException;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.ui.pref.MastodonOauthWizard.MastodonOauthComplete;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.LogWrapper;

public class AccountsPrefFragment extends PreferenceFragment {

	private static final LogWrapper LOG = new LogWrapper("APF");

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshAccountsList();
	}

	protected Prefs getPrefs () {
		if (this.prefs == null) throw new IllegalStateException("Prefs has not been initialised.");
		return this.prefs;
	}

	protected void refreshAccountsList () {
		getPreferenceScreen().removeAll();

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Account"); //ES
		pref.setSummary("Add a new account"); //ES
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		getPreferenceScreen().addPreference(pref);

		final List<String> accountIds = getPrefs().readAccountIds();
		for (final String accountId : accountIds) {
			try {
				final Account account = getPrefs().readAccount(accountId);
				getPreferenceScreen().addPreference(new AccountDialogPreference(getActivity(), account, this));
			}
			catch (final JSONException e) {
				DialogHelper.alert(getActivity(), "Failed to read account: ", e);
			}
		}
	}

	protected void promptNewAccountType () {
		DialogHelper.askItem(getActivity(), "Account Type", //ES
				new AccountProvider[] { AccountProvider.MASTODON },
				new Listener<AccountProvider>() {
					@Override
					public void onAnswer (final AccountProvider answer) {
						promptAddAccount(answer);
					}
				});
	}

	protected void promptAddAccount (final AccountProvider accountProvider) {
		switch (accountProvider) {
			case MASTODON:
				promptAddMastodonAccount();
				break;
			default:
				DialogHelper.alert(getActivity(), "Do not know how to add account of type: " + accountProvider);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void promptAddMastodonAccount () {
		final MastodonOauthWizard wizard = new MastodonOauthWizard(getActivity(), new MastodonOauthComplete() {
			@Override
			public String getAccountId () {
				return AccountsPrefFragment.this.prefs.getNextAccountId();
			}

			@Override
			public void onAccount (final Account account, final String screenName) throws JSONException {
				AccountsPrefFragment.this.prefs.writeNewAccount(account);
				DialogHelper.alert(getActivity(), "Mastodon account added:\n" + screenName); //ES
				refreshAccountsList();
			}
		});
		wizard.start();
	}

	@Override
	public void onActivityResult (final int requestCode, final int resultCode, final Intent intent) {
		LOG.d("onActivityResult(%d, %d, %s)", requestCode, resultCode, intent);
		super.onActivityResult(requestCode, resultCode, intent);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void askReauthenticateAccount (final Account oldAccount) {
		if (oldAccount.getProvider() == AccountProvider.MASTODON) {
			final MastodonOauthWizard wizard = new MastodonOauthWizard(getActivity(), new MastodonOauthComplete() {
				@Override
				public String getAccountId () {
					return oldAccount.getId();
				}

				@Override
				public void onAccount (final Account newAccount, final String screenName) throws JSONException {
					AccountsPrefFragment.this.prefs.updateExistingAccount(newAccount);
					DialogHelper.alert(getActivity(), "Mastodon account updated:\n" + screenName); //ES
					refreshAccountsList();
				}
			});
			wizard.start();
		}

		DialogHelper.alert(getActivity(), "Reauthenticate not supported for: " + oldAccount.getProvider());
	}

	protected void askDeleteAccount (final Account account) {
		// FIXME do not allow this if columns are using this account.
		DialogHelper.askYesNo(getActivity(),
				String.format("Delete the account %s?", account.getUiTitle()), //ES
				new Runnable() {
					@Override
					public void run () {
						getPrefs().deleteAccount(account);
						refreshAccountsList();
					}
				});
	}

	private static class AddAcountClickListener implements OnPreferenceClickListener {

		private final AccountsPrefFragment accountsPrefFragment;

		public AddAcountClickListener (final AccountsPrefFragment accountsPrefFragment) {
			this.accountsPrefFragment = accountsPrefFragment;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.accountsPrefFragment.promptNewAccountType();
			return true;
		}
	}

}

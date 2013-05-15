package com.vaguehope.onosendai.ui.pref;

import java.util.Set;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class AccountsPrefFragment extends PreferenceFragment {

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(getActivity());

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Account");
		pref.setSummary("Add a new Twitter or SuccessWhale account");
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		ps.addPreference(pref);

		// TODO read accounts from pref?

		Set<String> accountIds = getPreferenceManager().getSharedPreferences().getStringSet("account_ids", null);
		if (accountIds != null) {
			for (String accountId : accountIds) {

			}
		}

		AccountDialogPreference ap0 = new AccountDialogPreference(getActivity());
		ap0.setTitle("ExampleUser-san");
		ap0.setSummary("This is an example of an account.");
		ap0.setKey("account_key_0");
		ps.addPreference(ap0);

		AccountDialogPreference ap1 = new AccountDialogPreference(getActivity());
		ap1.setTitle("OtherUser-chan");
		ap1.setSummary("Another example of an account.");
		ap1.setKey("account_key_1");
		ps.addPreference(ap1);

		setPreferenceScreen(ps);
	}

	protected void promptAddAccount() {
		Toast.makeText(getActivity(), "TODO add account.", Toast.LENGTH_SHORT).show();
	}

	private static class AddAcountClickListener implements OnPreferenceClickListener {

		private final AccountsPrefFragment accountsPrefFragment;

		public AddAcountClickListener (final AccountsPrefFragment accountsPrefFragment) {
			this.accountsPrefFragment = accountsPrefFragment;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.accountsPrefFragment.promptAddAccount();
			return true;
		}
	}

}

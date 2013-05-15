package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.util.ArrayHelper;
import com.vaguehope.onosendai.util.DialogHelper;

public class AccountsPrefFragment extends PreferenceFragment {

	private static final String KEY_ACCOUNT_IDS = "account_ids";
	private static final String ACCOUNT_ID_SEP = ":";
	private static final String KEY_ACCOUNT_PREFIX = "account_";

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		refreshAccountsList();
	}

	protected void refreshAccountsList () {
		getPreferenceScreen().removeAll();

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Account");
		pref.setSummary("Add a new Twitter or SuccessWhale account");
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		getPreferenceScreen().addPreference(pref);

		final List<String> accountIds = readAccountIds();
		for (final String accountId : accountIds) {
			try {
				final Account account = readAccount(accountId);
				getPreferenceScreen().addPreference(new AccountDialogPreference(getActivity(), account, this));
			}
			catch (final JSONException e) {
				DialogHelper.alert(getActivity(), "Failed to read account: ", e);
			}
		}
	}

	protected void promptAddAccount () {
		String id = getNextAccountId();
		final AccountDialog dlg = new AccountDialog(getActivity(), id);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Account (" + id + ")");
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					writeNewAccount(dlg.getValue());
				}
				catch (final JSONException e) {
					DialogHelper.alert(getActivity(), "Failed to write new account: ", e);
				}
				refreshAccountsList();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", DLG_CANCEL_CLICK_LISTENER);
		dlgBuilder.create().show();
	}

	private String getNextAccountId () {
		final List<String> ids = readAccountIds();
		int x = 0;
		while (true) {
			final String id = KEY_ACCOUNT_PREFIX + x;
			if (!ids.contains(id)) return id;
			x += 1;
		}
	}

	private List<String> readAccountIds () {
		final String ids = getPreferenceManager().getSharedPreferences().getString(KEY_ACCOUNT_IDS, null);
		if (ids == null || ids.length() < 1) return Collections.emptyList();
		return Arrays.asList(ids.split(ACCOUNT_ID_SEP));
	}

	private Account readAccount (final String id) throws JSONException {
		final String raw = getPreferenceManager().getSharedPreferences().getString(id, null);
		if (raw == null) return null;
		return Account.parseJson(raw);
	}

	protected void writeNewAccount (final Account account) throws JSONException {
		final String id = account.getId();
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("Account has no ID.");
		final String json = account.toJson().toString();

		final List<String> ids = new ArrayList<String>();
		ids.addAll(readAccountIds());
		ids.add(id);
		final String idsS = ArrayHelper.join(ids, ACCOUNT_ID_SEP);

		final Editor e = getPreferenceManager().getSharedPreferences().edit();
		e.putString(id, json);
		e.putString(KEY_ACCOUNT_IDS, idsS);
		e.commit();
	}

	void deleteAccount (final Account account) {
		final List<String> ids = new ArrayList<String>();
		ids.addAll(readAccountIds());
		final String id = account.getId();
		if (!ids.remove(id)) throw new IllegalStateException("Tried to delete account '" + id + "' that does not exist in '" + ids + "'.");
		final String idsS = ArrayHelper.join(ids, ACCOUNT_ID_SEP);
		final Editor e = getPreferenceManager().getSharedPreferences().edit();
		e.putString(KEY_ACCOUNT_IDS, idsS);
		e.commit();
		refreshAccountsList();
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

	private static final DialogInterface.OnClickListener DLG_CANCEL_CLICK_LISTENER = new DialogInterface.OnClickListener() {
		@Override
		public void onClick (final DialogInterface dialog, final int whichButton) {
			dialog.cancel();
		}
	};

}

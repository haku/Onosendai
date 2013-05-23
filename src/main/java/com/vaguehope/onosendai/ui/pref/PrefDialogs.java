package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.DialogHelper;

public final class PrefDialogs {

	public interface Listener<T> {
		public void onAnswer (T answer);
	}

	protected static final List<AccountProvider> NEW_ACCOUNT_PROVIDERS = Collections.unmodifiableList(Arrays.asList(new AccountProvider[] {
			AccountProvider.TWITTER,
			AccountProvider.SUCCESSWHALE
	}));

	private static final List<String> NEW_ACCOUNT_LABELS = Collections.unmodifiableList(
			CollectionHelper.map(NEW_ACCOUNT_PROVIDERS, new Function<AccountProvider, String>() {
				@Override
				public String exec (final AccountProvider input) {
					return input.toHumanString();
				}
			}, new ArrayList<String>()));

	private PrefDialogs () {
		throw new AssertionError();
	}

	public static void askAccountType (final Context context, final Listener<AccountProvider> onAccountType) {
		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle("Account Type");
		bld.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(NEW_ACCOUNT_LABELS.toArray(new String[] {}), new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int item) {
				dialog.dismiss();
				onAccountType.onAnswer(NEW_ACCOUNT_PROVIDERS.get(item));
			}
		});
		bld.show();
	}

	public static void askAccount (final Context context, final Prefs prefs, final Listener<Account> onAccount) {
		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle("Account");
		bld.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);

		final List<Account> accounts = readAccountsOrAlert(context, prefs);
		if (accounts == null) return;

		final List<String> labels = CollectionHelper.map(accounts, new Function<Account, String>() {
			@Override
			public String exec (final Account account) {
				return account.humanTitle();
			}
		}, new ArrayList<String>());

		bld.setItems(labels.toArray(new String[] {}), new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int item) {
				dialog.dismiss();
				onAccount.onAnswer(accounts.get(item));
			}
		});
		bld.show();
	}

	private static List<Account> readAccountsOrAlert (final Context context, final Prefs prefs) {
		try {
			return new ArrayList<Account>(prefs.readAccounts());
		}
		catch (JSONException e) {
			DialogHelper.alert(context, "Failed to read accounts.", e);
			return null;
		}
	}

}

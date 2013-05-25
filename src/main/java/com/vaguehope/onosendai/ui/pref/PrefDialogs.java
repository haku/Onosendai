package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.content.Context;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.twitter.TwitterColumnType;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

public final class PrefDialogs {

	private PrefDialogs () {
		throw new AssertionError();
	}

	public static void askAccountType (final Context context, final Listener<AccountProvider> onAccountType) {
		DialogHelper.askItem(context, "Account Type",
				new AccountProvider[] { AccountProvider.TWITTER, AccountProvider.SUCCESSWHALE }
				, onAccountType);
	}

	public static void askAccount (final Context context, final Prefs prefs, final Listener<Account> onAccount) {
		final List<Account> items = readAccountsOrAlert(context, prefs);
		if (items == null) return;
		DialogHelper.askItem(context, "Account", items, onAccount);
	}

	public static void askTwitterColumnType (final Context context, final Listener<TwitterColumnType> onColumnType) {
		DialogHelper.askItem(context, "Twitter Columns", TwitterColumnType.values(), onColumnType);
	}

	private static List<Account> readAccountsOrAlert (final Context context, final Prefs prefs) {
		try {
			List<Account> items = new ArrayList<Account>();
			for (Account account : prefs.readAccounts()) {
				items.add(account);
			}
			return items;
		}
		catch (JSONException e) {
			DialogHelper.alert(context, "Failed to read accounts.", e);
			return null;
		}
	}

}

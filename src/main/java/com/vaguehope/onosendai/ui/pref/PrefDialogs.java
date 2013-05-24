package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.twitter.TwitterColumnType;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

public final class PrefDialogs {

	private PrefDialogs () {
		throw new AssertionError();
	}

	public static void askAccountType (final Context context, final Listener<AccountProvider> onAccountType) {
		final MenuItems<AccountProvider> items = new MenuItems<AccountProvider>()
				.add(AccountProvider.TWITTER, AccountProvider.TWITTER.toHumanString())
				.add(AccountProvider.SUCCESSWHALE, AccountProvider.SUCCESSWHALE.toHumanString());

		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle("Account Type");
		bld.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(items.titles(), new MenuItemAnswerListener<AccountProvider>(items, onAccountType));
		bld.show();
	}

	public static void askAccount (final Context context, final Prefs prefs, final Listener<Account> onAccount) {
		final MenuItems<Account> items = readAccountsOrAlert(context, prefs);
		if (items == null) return;

		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle("Account");
		bld.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(items.titles(), new MenuItemAnswerListener<Account>(items, onAccount));
		bld.show();
	}

	public static void askTwitterColumnType (final Context context, final Listener<TwitterColumnType> onColumnType) {
		final MenuItems<TwitterColumnType> items = new MenuItems<TwitterColumnType>();
		for (TwitterColumnType type : TwitterColumnType.values()) {
			items.add(type, type.getTitle());
		}

		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle("Twitter Columns");
		bld.setNegativeButton(android.R.string.cancel, DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(items.titles(), new MenuItemAnswerListener<TwitterColumnType>(items, onColumnType));
		bld.show();
	}

	private static MenuItems<Account> readAccountsOrAlert (final Context context, final Prefs prefs) {
		try {
			MenuItems<Account> items = new MenuItems<Account>();
			for (Account account : prefs.readAccounts()) {
				items.add(account, account.humanTitle());
			}
			return items;
		}
		catch (JSONException e) {
			DialogHelper.alert(context, "Failed to read accounts.", e);
			return null;
		}
	}

	private static class MenuItems<T> {

		private final List<MenuItem<T>> items = new ArrayList<MenuItem<T>>();

		public MenuItems () {}

		public MenuItems<T> add (final T item, final String title) {
			this.items.add(new MenuItem<T>(item, title));
			return this;
		}

		public T get (final int position) {
			return this.items.get(position).getItem();
		}

		public String[] titles () {
			return CollectionHelper.map(this.items, new Function<MenuItem<T>, String>() {
				@Override
				public String exec (final MenuItem<T> item) {
					return item.getTitle();
				}
			}, new ArrayList<String>()).toArray(new String[] {});
		}

	}

	public static class MenuItem<T> {

		private final T item;
		private final String title;

		public MenuItem (final T item, final String title) {
			this.item = item;
			this.title = title;
		}

		public T getItem () {
			return this.item;
		}

		public String getTitle () {
			return this.title;
		}

	}

	private static class MenuItemAnswerListener<T> implements DialogInterface.OnClickListener {

		private final MenuItems<T> items;
		private final Listener<T> onAnswer;

		public MenuItemAnswerListener (final MenuItems<T> items, final Listener<T> onAnswer) {
			this.items = items;
			this.onAnswer = onAnswer;
		}

		@Override
		public void onClick (final DialogInterface dialog, final int which) {
			dialog.dismiss();
			this.onAnswer.onAnswer(this.items.get(which));
		}

	}

}

package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.vaguehope.onosendai.config.AccountProvider;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.DialogHelper;

public final class PrefDialogs {

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

	public static void askAccountType (final Context context, final Listener<AccountProvider> onAccount) {
		final AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setTitle("Account Type");
		bld.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		bld.setItems(NEW_ACCOUNT_LABELS.toArray(new String[] {}), new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int item) {
				dialog.dismiss();
				onAccount.onAnswer(NEW_ACCOUNT_PROVIDERS.get(item));
			}
		});
		bld.show();
	}

	public interface Listener<T> {
		public void onAnswer (T answer);
	}

}

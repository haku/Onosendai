package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONException;
import android.content.Context;

import com.sys1yagi.mastodon4j.api.entity.MastodonList;
import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.mastodon.MastodonColumnType;
import com.vaguehope.onosendai.provider.mastodon.MastodonListsFetcher;
import com.vaguehope.onosendai.provider.twitter.TwitterColumnType;
import com.vaguehope.onosendai.provider.twitter.TwitterListsFetcher;
import com.vaguehope.onosendai.util.CollectionHelper.Function;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.StringHelper;
import com.vaguehope.onosendai.util.Titleable;

class ColumnChooser {

	public interface ColumnChoiceListener {
		void onColumn (final Account account, final String resource, final String title);
	}

	private final Context context;
	private final Prefs prefs;
	private final ColumnChoiceListener listener;

	public ColumnChooser (final Context context, final Prefs prefs, final ColumnChoiceListener listener) {
		this.context = context;
		this.prefs = prefs;
		this.listener = listener;
	}

	protected void onColumn (final Account account, final String resource) {
		onColumn(account, resource, null);
	}

	protected void onColumn (final Account account, final String resource, final String title) {
		this.listener.onColumn(account, resource, title);
	}

	private Collection<Account> readAccountsOrAlert () {
		try {
			// FIXME filter accounts by type.
			return this.prefs.readAccounts();
		}
		catch (final JSONException e) {
			DialogHelper.alert(this.context, "Failed to read accounts.", e);
			return null;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static enum InternalColumn implements Titleable {
		INSTANCE;
		@Override
		public String getUiTitle () {
			return "(internal)"; //ES
		}
	}

	protected void promptAddColumn () {
		final List<Titleable> items = new ArrayList<Titleable>(readAccountsOrAlert());
		items.add(InternalColumn.INSTANCE);
		DialogHelper.askItem(this.context, "Account", items, new Listener<Titleable>() {
			@Override
			public void onAnswer (final Titleable item) {
				if (item instanceof Account) {
					promptAddColumn((Account) item, null);
				}
				else if (item == InternalColumn.INSTANCE) {
					promptAddInternalColumn();
				}
				else {
					DialogHelper.alert(ColumnChooser.this.context, "Unknown item: " + item);
				}
			}
		});
	}

	protected void promptAddColumn (final Account account, final String previousResource) {
		switch (account.getProvider()) {
			case TWITTER:
				promptAddTwitterColumn(account); // TODO pass in existing resource value.
				break;
			case MASTODON:
				promptAddMastodonColumn(account); // TODO pass in existing resource value.
				break;
			default:
				onColumn(account, null);
				break;
		}
	}

	protected void promptAddInternalColumn () {
		DialogHelper.askItem(this.context, "Internal", InternalColumnType.values(), new Listener<InternalColumnType>() { //ES
			@Override
			public void onAnswer (final InternalColumnType type) {
				onColumn(null, type.name(), type.getUiTitle());
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void promptAddTwitterColumn (final Account account) {
		DialogHelper.askItem(this.context, "Twitter Columns", TwitterColumnType.values(), new Listener<TwitterColumnType>() { //ES
			@Override
			public void onAnswer (final TwitterColumnType type) {
				promptAddTwitterColumn(account, type);
			}
		});
	}

	protected void promptAddTwitterColumn (final Account account, final TwitterColumnType type) {
		switch (type) {
			case LIST:
				promptAddTwitterListColumn(account, null);
				break;
			case ANOTHERS_LIST:
				promptAddTwitterAnothersListColumn(account);
				break;
			case ANOTHERS_FAVORITES:
				promptAddTwitterAnothersFavoritesColumn(account);
				break;
			case SEARCH:
				promptAddTwitterSearchColumn(account);
				break;
			default:
				onColumn(account, type.getResource());
		}
	}

	protected void promptAddTwitterAnothersListColumn (final Account account) {
		// TODO auto complete screen names?
		DialogHelper.askString(this.context, "Their screen name (without @):", new Listener<String>() { //ES
			@Override
			public void onAnswer (final String answer) {
				if (StringHelper.isEmpty(answer)) {
					DialogHelper.alert(ColumnChooser.this.context, "Screen name was empty."); //ES
				}
				else {
					promptAddTwitterListColumn(account, answer);
				}
			}
		});
	}

	protected void promptAddTwitterListColumn (final Account account, final String ownerScreenName) {
		new TwitterListsFetcher(this.context, account, ownerScreenName, new Listener<List<String>>() {
			@Override
			public void onAnswer (final List<String> lists) {
				promptAddTwitterListColumn(account, ownerScreenName, lists);
			}
		}).execute();
	}

	protected void promptAddTwitterListColumn (final Account account, final String ownerScreenName, final List<String> listSlugs) {
		DialogHelper.askStringItem(this.context, "Twitter Lists", listSlugs, new Listener<String>() { //ES
			@Override
			public void onAnswer (final String answer) {
				final String owner = StringHelper.isEmpty(ownerScreenName) ? "" : ownerScreenName + "/";
				onColumn(account, TwitterColumnType.LIST.getResource() + owner + answer);
			}
		});
	}

	protected void promptAddTwitterAnothersFavoritesColumn (final Account account) {
		// TODO auto complete screen names?
		DialogHelper.askString(this.context, "Their screen name (without @):", new Listener<String>() { //ES
			@Override
			public void onAnswer (final String answer) {
				if (StringHelper.isEmpty(answer)) {
					DialogHelper.alert(ColumnChooser.this.context, "Screen name was empty."); //ES
				}
				else {
					onColumn(account, TwitterColumnType.ANOTHERS_FAVORITES.getResource() + answer);
				}
			}
		});
	}

	protected void promptAddTwitterSearchColumn (final Account account) {
		DialogHelper.askString(this.context, "Search term:", new Listener<String>() { //ES
			@Override
			public void onAnswer (final String answer) {
				onColumn(account, TwitterColumnType.SEARCH.getResource() + answer);
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void promptAddMastodonColumn (final Account account) {
		DialogHelper.askItem(this.context, "Mastodon Columns", MastodonColumnType.values(), new Listener<MastodonColumnType>() { //ES
			@Override
			public void onAnswer (final MastodonColumnType type) {
				promptAddMastodonColumn(account, type);
			}
		});
	}

	protected void promptAddMastodonColumn (final Account account, final MastodonColumnType type) {
		switch (type) {
			case LIST:
				promptAddMastodonListColumn(account);
				break;
			default:
				onColumn(account, type.getResource());
		}
	}

	protected void promptAddMastodonListColumn (final Account account) {
		new MastodonListsFetcher(this.context, account, new Listener<List<MastodonList>>() {
			@Override
			public void onAnswer (final List<MastodonList> lists) {
				promptAddMastodonListColumn(account, lists);
			}
		}).execute();
	}

	protected void promptAddMastodonListColumn (final Account account, final List<MastodonList> listSlugs) {
		DialogHelper.askItem(this.context, "Mastodon Lists", listSlugs, new Function<MastodonList, String>() {
			@Override
			public String exec (final MastodonList list) {
				return list.getTitle() + " (" + list.getId() + ")";
			}
		}, new Listener<MastodonList>() {
			@Override
			public void onAnswer (final MastodonList answer) {
				onColumn(account, MastodonColumnType.LIST.getResource() + answer.getId(), answer.getTitle());
			}
		});
	}

}

package com.vaguehope.onosendai.ui.pref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import android.content.Context;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.InternalColumnType;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleColumns;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleColumnsFetcher;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleSource;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleSources;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleSourcesFetcher;
import com.vaguehope.onosendai.provider.twitter.TwitterColumnType;
import com.vaguehope.onosendai.provider.twitter.TwitterListsFetcher;
import com.vaguehope.onosendai.util.CollectionHelper;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;
import com.vaguehope.onosendai.util.DialogHelper.Question;
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
			return "(internal)";
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
			case SUCCESSWHALE:
				promptAddSuccessWhaleColumn(account, previousResource);
				break;
			default:
				onColumn(account, null);
				break;
		}
	}

	protected void promptAddInternalColumn () {
		DialogHelper.askItem(this.context, "Internal", InternalColumnType.values(), new Listener<InternalColumnType>() {
			@Override
			public void onAnswer (final InternalColumnType type) {
				onColumn(null, type.name(), type.getUiTitle());
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void promptAddTwitterColumn (final Account account) {
		DialogHelper.askItem(this.context, "Twitter Columns", TwitterColumnType.values(), new Listener<TwitterColumnType>() {
			@Override
			public void onAnswer (final TwitterColumnType type) {
				promptAddTwitterColumn(account, type);
			}
		});
	}

	protected void promptAddTwitterColumn (final Account account, final TwitterColumnType type) {
		switch (type) {
			case LIST:
				promptAddTwitterListColumn(account);
				break;
			case SEARCH:
				promptAddTwitterSearchColumn(account);
				break;
			default:
				onColumn(account, type.getResource());
		}
	}

	protected void promptAddTwitterListColumn (final Account account) {
		new TwitterListsFetcher(this.context, account, new Listener<List<String>>() {
			@Override
			public void onAnswer (final List<String> lists) {
				promptAddTwitterListColumn(account, lists);
			}
		}).execute();
	}

	protected void promptAddTwitterListColumn (final Account account, final List<String> listSlugs) {
		DialogHelper.askStringItem(this.context, "Twitter Lists", listSlugs, new Listener<String>() {
			@Override
			public void onAnswer (final String answer) {
				onColumn(account, TwitterColumnType.LIST.getResource() + answer);
			}
		});
	}

	protected void promptAddTwitterSearchColumn (final Account account) {
		DialogHelper.askString(this.context, "Search term:", new Listener<String>() {
			@Override
			public void onAnswer (final String answer) {
				onColumn(account, TwitterColumnType.SEARCH.getResource() + answer);
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void promptAddSuccessWhaleColumn (final Account account, final String previousResource) {
		if (previousResource != null) {
			promptAddSuccessWhaleCustomColumn(account, previousResource);
			return;
		}

		final String existing = "Existing Column";
		final String custom = "Custom Source Mix";
		DialogHelper.askStringItem(this.context, "SuccessWhale Column", CollectionHelper.listOf(existing, custom), new Listener<String>() {
			@Override
			public void onAnswer (final String answer) {
				if (existing.equals(answer)) {
					promptAddSuccessWhaleExistingColumn(account);
				}
				else {
					promptAddSuccessWhaleCustomColumn(account, null);
				}
			}
		});
	}

	protected void promptAddSuccessWhaleExistingColumn (final Account account) {
		new SuccessWhaleColumnsFetcher(this.context, account, new Listener<SuccessWhaleColumns>() {
			@Override
			public void onAnswer (final SuccessWhaleColumns columns) {
				promptAddSuccessWhaleColumn(account, columns);
			}
		}).execute();
	}

	protected void promptAddSuccessWhaleColumn (final Account account, final SuccessWhaleColumns columns) {
		// TODO allow multi selection.
		DialogHelper.askItem(this.context, "SuccessWhale Columns", columns.getColumns(), new Listener<Column>() {
			@Override
			public void onAnswer (final Column column) {
				onColumn(account, column.getResource(), column.getTitle());
			}
		});
	}

	protected void promptAddSuccessWhaleCustomColumn (final Account account, final String previousResource) {
		new SuccessWhaleSourcesFetcher(this.context, account, new Listener<SuccessWhaleSources>() {
			@Override
			public void onAnswer (final SuccessWhaleSources sources) {
				promptAddSuccessWhaleColumn(account, sources, previousResource);
			}
		}).execute();
	}

	protected void promptAddSuccessWhaleColumn (final Account account, final SuccessWhaleSources sources, final String previousResource) {
		final Set<SuccessWhaleSource> previous = SuccessWhaleSources.fromResource(previousResource);
		DialogHelper.askItems(this.context, "SuccessWhale Sources", sources.getSources(),
				new Question<SuccessWhaleSource>() {
					@Override
					public boolean ask (final SuccessWhaleSource source) {
						return previous != null && previous.contains(source);
					}
				},
				new Listener<Set<SuccessWhaleSource>>() {
					@Override
					public void onAnswer (final Set<SuccessWhaleSource> anwer) {
						onColumn(account, SuccessWhaleSources.toResource(anwer));
					}
				});
	}

}

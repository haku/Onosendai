package com.vaguehope.onosendai.ui.pref;

import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import com.vaguehope.onosendai.config.Account;
import com.vaguehope.onosendai.config.Column;
import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleColumns;
import com.vaguehope.onosendai.provider.successwhale.SuccessWhaleColumnsFetcher;
import com.vaguehope.onosendai.provider.twitter.TwitterColumnType;
import com.vaguehope.onosendai.provider.twitter.TwitterListsFetcher;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

public class ColumnsPrefFragment extends PreferenceFragment {

	private Prefs prefs;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		this.prefs = new Prefs(getPreferenceManager());
		refreshColumnsList();
	}

	protected Prefs getPrefs () {
		return this.prefs;
	}

	protected void refreshColumnsList () {
		getPreferenceScreen().removeAll();

		final Preference pref = new Preference(getActivity());
		pref.setTitle("Add Column");
		pref.setSummary("Add a new column of updates");
		pref.setOnPreferenceClickListener(new AddAcountClickListener(this));
		getPreferenceScreen().addPreference(pref);

		final List<Integer> columnIds = getPrefs().readColumnIds();
		for (final Integer columnId : columnIds) {
			try {
				final Column column = getPrefs().readColumn(columnId);
				getPreferenceScreen().addPreference(new ColumnDialogPreference(getActivity(), column, this));
			}
			catch (final JSONException e) {
				DialogHelper.alert(getActivity(), "Failed to read column: ", e);
			}
		}
	}

	protected void promptAddColumn () {
		PrefDialogs.askAccount(getActivity(), this.prefs, new Listener<Account>() {
			@Override
			public void onAnswer (final Account account) {
				promptAddColumn(account);
			}
		});
	}

	protected void promptAddColumn (final Account account) {
		switch (account.getProvider()) {
			case TWITTER:
				promptAddTwitterColumn(account);
				break;
			case SUCCESSWHALE:
				promptAddSuccessWhaleColumn(account);
				break;
			default:
				promptAddColumn(account, (String) null);
				break;
		}
	}

	protected void promptAddTwitterColumn (final Account account) {
		PrefDialogs.askTwitterColumnType(getActivity(), new Listener<TwitterColumnType>() {
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
				promptAddColumn(account, type.getResource());
		}
	}

	protected void promptAddTwitterListColumn (final Account account) {
		new TwitterListsFetcher(getActivity(), account, new Listener<List<String>>() {
			@Override
			public void onAnswer (final List<String> lists) {
				promptAddTwitterListColumn(account, lists);
			}
		}).execute();
	}

	protected void promptAddTwitterListColumn (final Account account, final List<String> listSlugs) {
		DialogHelper.askStringItem(getActivity(), "Twitter List", listSlugs, new Listener<String>() {
			@Override
			public void onAnswer (final String answer) {
				promptAddColumn(account, TwitterColumnType.LIST.getResource() + answer);
			}
		});
	}

	protected void promptAddTwitterSearchColumn (final Account account) {
		DialogHelper.askString(getActivity(), "Search term:", new Listener<String>() {
			@Override
			public void onAnswer (final String answer) {
				promptAddColumn(account, TwitterColumnType.SEARCH.getResource() + answer);
			}
		});
	}

	protected void promptAddSuccessWhaleColumn (final Account account) {
		new SuccessWhaleColumnsFetcher(getActivity(), account, new Listener<SuccessWhaleColumns>() {
			@Override
			public void onAnswer (final SuccessWhaleColumns columns) {
				promptAddSuccessWhaleColumn(account, columns);
			}
		}).execute();
	}

	protected void promptAddSuccessWhaleColumn (final Account account, final SuccessWhaleColumns columns) {
		// TODO allow multi selection.
		DialogHelper.askItem(getActivity(), "SuccessWhale Column", columns.getColumns(), new Listener<Column>() {
			@Override
			public void onAnswer (final Column column) {
				promptAddColumn(account, column.getResource(), column.getTitle());
			}
		});
	}

	protected void promptAddColumn (final Account account, final String resource) {
		promptAddColumn(account, resource, null);
	}

	protected void promptAddColumn (final Account account, final String resource, final String title) {
		final int id = getPrefs().getNextColumnId();
		final ColumnDialog dlg = new ColumnDialog(getActivity(), id);
		if (account != null) dlg.setAccount(account);
		if (resource != null) dlg.setResource(resource);
		if (title != null) dlg.setTitle(title);

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(getActivity());
		dlgBuilder.setTitle("New Column (" + id + ")");
		dlgBuilder.setView(dlg.getRootView());
		dlgBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				dialog.dismiss();
				try {
					getPrefs().writeNewColumn(dlg.getValue());
				}
				catch (final JSONException e) {
					DialogHelper.alert(getActivity(), "Failed to write new column: ", e);
				}
				refreshColumnsList();
			}
		});
		dlgBuilder.setNegativeButton("Cancel", DialogHelper.DLG_CANCEL_CLICK_LISTENER);
		dlgBuilder.create().show();
	}

	private static class AddAcountClickListener implements OnPreferenceClickListener {

		private final ColumnsPrefFragment columnsPrefFragment;

		public AddAcountClickListener (final ColumnsPrefFragment columnsPrefFragment) {
			this.columnsPrefFragment = columnsPrefFragment;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			this.columnsPrefFragment.promptAddColumn();
			return true;
		}
	}

}

package com.vaguehope.onosendai.ui.pref;

import android.content.Context;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.vaguehope.onosendai.config.Prefs;
import com.vaguehope.onosendai.util.DialogHelper;
import com.vaguehope.onosendai.util.DialogHelper.Listener;

public class AddFilterSelectionActionModeCallback implements Callback {

	private static final int ID_ADD_FILTER = 100;

	private final TextView tv;

	public AddFilterSelectionActionModeCallback (final TextView tv) {
		this.tv = tv;
	}

	@Override
	public boolean onCreateActionMode (final ActionMode mode, final Menu menu) {
		final MenuItem mnuAddFilter = menu.add(Menu.NONE, ID_ADD_FILTER, Menu.NONE, "Add Filter");
		mnuAddFilter.setIcon(android.R.drawable.ic_menu_add);
		return true;
	}

	@Override
	public boolean onPrepareActionMode (final ActionMode mode, final Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked (final ActionMode mode, final MenuItem item) {
		switch (item.getItemId()) {
			case ID_ADD_FILTER:
				promptAddFilter();
				return true;
			default:
				return false;
		}
	}

	@Override
	public void onDestroyActionMode (final ActionMode mode) {/* unused */}

	private void promptAddFilter () {
		FiltersPrefFragment.promptNewFilter(
				this.tv.getContext(),
				new Prefs(this.tv.getContext()),
				new FilterAddedListener(this.tv.getContext()),
				this.tv.getText().subSequence(this.tv.getSelectionStart(), this.tv.getSelectionEnd()).toString());
	}

	private static class FilterAddedListener implements Listener<String> {

		private final Context context;

		public FilterAddedListener (final Context context) {
			this.context = context;
		}

		@Override
		public void onAnswer (final String answer) {
			DialogHelper.alert(this.context, "Filter added:\n" + answer);
		}
	}

}

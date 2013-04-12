package com.vaguehope.onosendai.ui;

import java.util.List;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;

import com.vaguehope.onosendai.config.Column;

public class GotoMenu implements OnClickListener {

	private static final int MNU_GOTO_BASE_ID = 100;

	private final MainActivity mainActivity;

	public GotoMenu (final MainActivity mainActivity) {
		this.mainActivity = mainActivity;
	}

	@Override
	public void onClick (final View v) {
		final PopupMenu mnu = new PopupMenu(this.mainActivity, v);
		final List<Column> columns = this.mainActivity.getConf().getColumns();
		int i = 0;
		for (final Column col : columns) {
			mnu.getMenu().add(Menu.NONE, MNU_GOTO_BASE_ID + i, Menu.NONE, col.getTitle());
			i++;
		}
		mnu.setOnMenuItemClickListener(new GotoItemClientListener(columns, this.mainActivity));
		mnu.show();
	}

	private static class GotoItemClientListener implements PopupMenu.OnMenuItemClickListener {

		private final List<Column> columns;
		private final MainActivity mainActivity;

		public GotoItemClientListener (final List<Column> columns, final MainActivity mainActivity) {
			this.columns = columns;
			this.mainActivity = mainActivity;
		}

		@Override
		public boolean onMenuItemClick (final MenuItem item) {
			final int page = item.getItemId() - MNU_GOTO_BASE_ID;
			if (page >= 0 && page < this.columns.size()) {
				this.mainActivity.gotoPage(page);
				return true;
			}
			return false;
		}

	}

}
